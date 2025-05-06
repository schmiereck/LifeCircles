package de.lifecircles.model;

import de.lifecircles.model.reproduction.ReproductionManager;
import de.lifecircles.service.StatisticsManager;
import de.lifecircles.service.ActorSensorCellCalcService;
import de.lifecircles.service.BlockerCellCalcService;
import de.lifecircles.service.RepulsionCellCalcService;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.service.EnergySunCalcService;
import de.lifecircles.service.EnergyTransferCellCalcService;
import de.lifecircles.model.SunRay;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.PartitioningStrategy;
import de.lifecircles.service.PartitioningStrategyFactory;
import de.lifecircles.service.QuadTreePartitioningStrategy;
import de.lifecircles.service.SpatialGridPartitioningStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Random;
import de.lifecircles.service.TrainMode;

/**
 * Represents the simulation environment.
 * Manages physics simulation and cell interactions.
 */
public class Environment {
    private static final double GRAVITY = 9.81;
    private static final Vector2D GRAVITY_VECTOR = new Vector2D(0, GRAVITY);
    private static final Random random = new Random();
    private static final double REPOPULATION_THRESHOLD_PERCENT = 0.25;
    private final SimulationConfig config;
    private final EnergySunCalcService energySunCalcService;
    private final List<SunRay> sunRays;
    private double width;
    private double height;
    private final List<Cell> cells;
    private final List<Blocker> blockers;
    private Cell lastDeadCell;

    public Environment(double width, double height) {
        this.width = width;
        this.height = height;
        this.cells = new ArrayList<>();
        this.blockers = new ArrayList<>();
        this.config = SimulationConfig.getInstance();
        this.sunRays = new ArrayList<>();
        this.energySunCalcService = new EnergySunCalcService();
    }

    public void addGroundBlocker() {
        Blocker ground = new Blocker(
            new Vector2D(width/2, height - 10), // Position at bottom
            width,                              // Full width
            20,                                 // Height
            javafx.scene.paint.Color.GRAY,      // Color
            Blocker.BlockerType.GROUND          // Type
        );
        blockers.add(ground);
    }

    public void addSunBlocker() {
        Blocker sunBlocker = new Blocker(
            new Vector2D(width/4, this.height - height/8), // Position at bottom
            width/6,                              // Full width
            20,                                 // Height
            javafx.scene.paint.Color.GRAY,      // Color
            Blocker.BlockerType.PLATFORM          // Type
        );
        blockers.add(sunBlocker);
    }

    public void addWallBlocker(double x, double yTop, double yBottom) {
        final double wallHeight = yTop - yBottom;
        final double wallWidth = 20;
        Blocker wallBlocker = new Blocker(
            new Vector2D(x + wallWidth/2, (this.height - yBottom) - wallHeight/2), // Position at bottom
            wallWidth,                              // Full width
            wallHeight,                                 // Height
            javafx.scene.paint.Color.GRAY,      // Color
            Blocker.BlockerType.WALL          // Type
        );
        blockers.add(wallBlocker);
    }

    public void addCell(Cell cell) {
        cells.add(cell);
    }

    public List<Cell> getCells() {
        return new ArrayList<>(cells);
    }

    public List<Blocker> getBlockers() {
        return new ArrayList<>(blockers);
    }

    public void addBlocker(Blocker blocker) {
        blockers.add(blocker);
    }

    /**
     * Updates the simulation state.
     * @param deltaTime Time step in seconds
     * @param partitioner Pre-built partitioning strategy for interactions
     */
    public void update(double deltaTime, PartitioningStrategy partitioner) {
        // Calculate sun energy rays
        sunRays.clear();
        sunRays.addAll(
            energySunCalcService.calculateSunEnergy(
                cells, blockers, width, height, config, deltaTime
            )
        );

        // Using provided partitioner for interactions
        partitioner.build(cells);
        // Process sensor/actor interactions
        ActorSensorCellCalcService.processInteractions(cells, deltaTime, partitioner);
        // Process repulsive forces
        RepulsionCellCalcService.processRepulsiveForces(cells, deltaTime, partitioner);

        // Update cells and handle reproduction
        List<Cell> newCells = new ArrayList<>();
        Iterator<Cell> iterator = cells.iterator();

        while (iterator.hasNext()) {
            Cell cell = iterator.next();

            // Apply viscosity
            //Vector2D viscousForce = cell.getVelocity().multiply(-VISCOSITY);
            Vector2D viscousForce = cell.getVelocity().multiply(-SimulationConfig.getInstance().getViscosity());
            cell.applyForce(viscousForce, cell.getPosition(), deltaTime);

            // Apply gravity
            cell.applyForce(GRAVITY_VECTOR.multiply(cell.getSize()), cell.getPosition(), deltaTime);

            // Handle blocker collisions after force application
            BlockerCellCalcService.handleBlockerCollisions(cell, blockers, deltaTime);

            // Get nearby cell types for the neural network via partitioner
            //List<Cell> neighborCells = partitioner.getNeighbors(cell);
            //List<CellType> nearbyTypes = neighborCells.stream()
            //    .filter(other -> other != cell)
            //    .filter(other -> cell.getPosition().distance(other.getPosition()) <= config.getCellInteractionRadius())
            //    .map(Cell::getType)
            //    .toList();

            // Update position, rotation, and neural network
            //cell.updateWithNeighbors(deltaTime, nearbyTypes);

            // Wrap position around environment boundaries
            wrapPosition(cell);

            // Handle reproduction (skip in HIGH_ENERGY mode)
            if (ReproductionManager.canReproduce(this.config, cell)) {
                Cell childCell = ReproductionManager.reproduce(this.config, cell);
                newCells.add(childCell);
            }

            // Remove cell after configured age.
            if ((cell.getAge() >= config.getCellDeathAge()) || (cell.getEnergy() <= 0.0D)) {
                lastDeadCell = cell;
                iterator.remove();
            }
        }

        // Process energy transfers between cells
        EnergyTransferCellCalcService.processEnergyTransfers(partitioner, cells, deltaTime);

        // Add new cells from reproduction (skip in HIGH_ENERGY mode)
        cells.addAll(newCells);

        // Repopulation (skip in HIGH_ENERGY mode)
        if (config.getTrainMode() == TrainMode.NONE) {
            calcRepopulationIfNeeded();
        }

        // Update statistics
        StatisticsManager.getInstance().update(cells);
    }

    /**
     * Repopulation: when only REPOPULATION_THRESHOLD_PERCENT of initial cells remain, generate mutated offspring and split energy
     */
    private void calcRepopulationIfNeeded() {
        int currentCount = cells.size();
        int initialCount = config.getInitialCellCount();
        // If all cells are dead, repopulate with random initial cells
        if (currentCount == 0) {
            if (lastDeadCell != null) {
                // Repopulate by mutating the last dead cell
                lastDeadCell.setEnergy(1.0);
                for (int i = 0; i < initialCount; i++) {
                    Cell child = ReproductionManager.reproduce(config, lastDeadCell);
                    cells.add(child);
                }
            } else {
                for (int i = 0; i < initialCount; i++) {
                    Vector2D pos = new Vector2D(random.nextDouble() * width, random.nextDouble() * height);
                    Cell newCell = new Cell(pos, config.getCellMaxRadius() / 2.0D);
                    cells.add(newCell);
                }
            }
        } else {
            int thresholdCount = (int) Math.ceil(initialCount * REPOPULATION_THRESHOLD_PERCENT);
            if (currentCount < thresholdCount) {
                int toSpawn = initialCount - currentCount;
                for (int i = 0; i < toSpawn; i++) {
                    Cell parent = cells.get(random.nextInt(cells.size()));
                    Cell child = ReproductionManager.reproduce(config, parent);
                    cells.add(child);
                }
            }
        }
    }

    private void wrapPosition(Cell cell) {
        Vector2D pos = cell.getPosition();
        double x = pos.getX();
        double y = pos.getY();

        // Wrap horizontally
        if (x < 0) x += width;
        if (x >= width) x -= width;

        // Wrap vertically
        if (y < 0) y += height;
        if (y >= height) y -= height;

        if (x != pos.getX() || y != pos.getY()) {
            cell.setPosition(new Vector2D(x, y));
        }
    }

    // Getter for current sun ray visuals
    public List<SunRay> getSunRays() {
        return new ArrayList<>(sunRays);
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    /**
     * Resets the cell population with the provided new generation.
     */
    public void resetCells(List<Cell> newCells) {
        cells.clear();
        cells.addAll(newCells);
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setHeight(double height) {
        this.height = height;
    }
}
