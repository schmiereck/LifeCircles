package de.lifecircles.model;

import de.lifecircles.model.reproduction.ReproductionManager;
import de.lifecircles.service.StatisticsManager;
import de.lifecircles.service.ActorSensorCellCalcService;
import de.lifecircles.service.BlockerCellCalcService;
import de.lifecircles.service.RepulsionCellCalcService;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.service.EnergySunCalcService;
import de.lifecircles.service.EnergyBeamCellCalcService;
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

/**
 * Represents the simulation environment.
 * Manages physics simulation and cell interactions.
 */
public class Environment {
    private static final double VISCOSITY = 0.75;
    private static final double GRAVITY = 9.81;
    private static final Vector2D GRAVITY_VECTOR = new Vector2D(0, GRAVITY);
    private static final Random random = new Random();
    private static final double REPOPULATION_THRESHOLD_PERCENT = 0.25;
    private final SimulationConfig config;
    private final EnergySunCalcService energySunCalcService;
    private final List<SunRay> sunRays;
    private final double width;
    private final double height;
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

        // Add ground blocker by default
        addGroundBlocker();
        addSunBlocker();
    }

    private void addGroundBlocker() {
        Blocker ground = new Blocker(
            new Vector2D(width/2, height - 10), // Position at bottom
            width,                              // Full width
            20,                                 // Height
            javafx.scene.paint.Color.GRAY,      // Color
            Blocker.BlockerType.GROUND          // Type
        );
        blockers.add(ground);
    }

    private void addSunBlocker() {
        Blocker sunBlocker = new Blocker(
            new Vector2D(width/4, height - height/8), // Position at bottom
            width/6,                              // Full width
            20,                                 // Height
            javafx.scene.paint.Color.GRAY,      // Color
            Blocker.BlockerType.PLATFORM          // Type
        );
        blockers.add(sunBlocker);
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
     */
    public void update(double deltaTime) {
        // Calculate sun energy rays
        sunRays.clear();
        sunRays.addAll(
            energySunCalcService.calculateSunEnergy(
                cells, blockers, width, height, config, deltaTime
            )
        );

        // Choose partitioning strategy: QuadTree or SpatialGrid
        //PartitioningStrategy partitioner = new QuadTreePartitioningStrategy(width, height);
        // For spatial grid strategy, uncomment below:
        PartitioningStrategy partitioner = PartitioningStrategyFactory.createStrategy(width, height, Cell.getMaxSize());
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
            Vector2D viscousForce = cell.getVelocity().multiply(-VISCOSITY);
            cell.applyForce(viscousForce, cell.getPosition(), deltaTime);

            // Apply gravity
            cell.applyForce(GRAVITY_VECTOR.multiply(cell.getSize()), cell.getPosition(), deltaTime);

            // Handle blocker collisions after force application
            BlockerCellCalcService.handleBlockerCollisions(cell, blockers, deltaTime);

            // Get nearby cell types for the neural network via partitioner
            List<Cell> neighborCells = partitioner.getNeighbors(cell);
            List<CellType> nearbyTypes = neighborCells.stream()
                .filter(other -> other != cell)
                .filter(other -> cell.getPosition().distance(other.getPosition()) <= config.getCellInteractionRadius())
                .map(Cell::getType)
                .toList();

            // Update position, rotation, and neural network
            cell.updateWithNeighbors(deltaTime, nearbyTypes);

            // Wrap position around environment boundaries
            wrapPosition(cell);

            // Handle reproduction
            if (ReproductionManager.canReproduce(cell)) {
                Cell childCell = ReproductionManager.reproduce(config, cell);
                newCells.add(childCell);
            }

            // Remove dead cells (no energy)
            if (cell.getEnergy() <= 0) {
                lastDeadCell = cell;
                iterator.remove();
            }
        }

        // Add new cells from reproduction
        cells.addAll(newCells);

        // Delegate energy beam processing to service
        sunRays.addAll(EnergyBeamCellCalcService.processEnergyBeams(cells, width, height));

        calcRepopulationIfNeeded();

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
}
