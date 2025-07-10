package de.lifecircles.model;

import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;

import java.util.*;

/**
 * Represents the simulation environment.
 * Manages physics simulation and cell interactions.
 */
public class Environment {
    public static double GroundBlockerHeight = 50.0D;

    private static final Random random = new Random();
    private final SimulationConfig config;
    private final EnergySunCalcService energySunCalcService;
    private final List<SunRay> sunRayList;
    private double width;
    private double height;
    private final List<Cell> cellList;
    private final List<Blocker> blockerList;
    private Cell lastDeadCell;

    // Singleton-Instanz für einfachen Zugriff
    private static Environment instance;

    public Environment(double width, double height) {
        this.width = width;
        this.height = height;
        this.cellList = new ArrayList<>();
        this.blockerList = new ArrayList<>();
        this.config = SimulationConfig.getInstance();
        this.sunRayList = new ArrayList<>();
        this.energySunCalcService = new EnergySunCalcService();

        // Setze diese Instanz als globalen Zugriffspunkt
        instance = this;
    }

    /**
     * Gibt die aktuelle Environment-Instanz zurück
     */
    public static Environment getInstance() {
        return instance;
    }

    public void addGroundBlocker() {
        final double extraWidth = this.config.getCellMaxRadiusSize();
        final Blocker ground = new Blocker(
            new Vector2D((this.width / 2.0D), this.height - (GroundBlockerHeight / 2.0D)), // Position at bottom
                this.width + (extraWidth * 4.0D),                              // Full width
                GroundBlockerHeight,                                 // Height
            javafx.scene.paint.Color.GRAY,      // Color
            Blocker.BlockerType.GROUND          // Type
        );
        this.blockerList.add(ground);
    }

    public void addSunBlocker(final int xStart, final int yStart, final int blockerWidth) {
        Blocker sunBlocker = new Blocker(
            new Vector2D(xStart, yStart), // Position at bottom
            blockerWidth,                              // Full width
            25,                                 // Height
            javafx.scene.paint.Color.GRAY,      // Color
            Blocker.BlockerType.PLATFORM          // Type
        );
        this.blockerList.add(sunBlocker);
    }

    public void addWallBlocker(final double x, final double yTop, final double yBottom) {
        final double wallWidth = 25;
        addWallBlocker(x, yTop, yBottom, wallWidth);
    }

    public void addWallBlocker(double x, double yTop, double yBottom, final double wallWidth) {
        final double wallHeight = yTop - yBottom;
        Blocker wallBlocker = new Blocker(
            new Vector2D(x + wallWidth/2, (this.height - yBottom) - wallHeight/2), // Position at bottom
            wallWidth,                              // Full width
            wallHeight,                                 // Height
            javafx.scene.paint.Color.GRAY,      // Color
            Blocker.BlockerType.WALL          // Type
        );
        this.blockerList.add(wallBlocker);
    }

    public void addCell(Cell cell) {
        this.cellList.add(cell);
    }

    public List<Cell> getCopyOfCellList() {
        return new ArrayList<>(this.cellList);
    }

    public List<Cell> getCellList() {
        return this.cellList;
    }

    public List<Blocker> getBlockerList() {
        return this.blockerList;
    }

    public void addBlocker(Blocker blocker) {
        this.blockerList.add(blocker);
    }

    /**
     * Updates the simulation state.
     * @param deltaTime Time step in seconds
     * @param partitioner Pre-built partitioning strategy for interactions
     */
    public void update(final double deltaTime, final PartitioningStrategy partitioner) {
        // Calculate sun energy rays
        this.sunRayList.clear();
        this.sunRayList.addAll(
                this.energySunCalcService.calculateSunEnergy(
                        this.cellList, this.blockerList, this.width, this.height, this.config, deltaTime
                )
        );

        // Process repulsive forces
        RepulsionCellCalcService.processRepulsiveForces(this.cellList, partitioner);
        // Process sensor/actor interactions
        SensorActorForceCellCalcService.processInteractions(this.cellList, partitioner);
        // Process energy transfers between cells
        EnergyTransferCellCalcService.processEnergyTransfers(this.cellList);

        for (final Cell cell : this.cellList) {
        //this.cells.parallelStream().forEach(cell -> {
            // Apply viscosity
            //Vector2D viscousForce = cell.getVelocity().multiply(-VISCOSITY);
            final Vector2D viscousForce = cell.getVelocity().multiply(-SimulationConfig.getInstance().getViscosity());
            cell.applyForce(viscousForce, cell.getPosition());

            // Apply gravity
            //cell.applyForce(SimulationConfig.GRAVITY_VECTOR.multiply(cell.getRadiusSize()), cell.getPosition(), deltaTime);
            cell.applyForce(SimulationConfig.GRAVITY_VECTOR, cell.getPosition());

            // Handle blocker collisions after force application
            BlockerCellCalcService.handleBlockerCollisions(cell, this.blockerList);
        }
        //);

        this.cellList.parallelStream().forEach(cell -> {
            CellCalcService.updateForces(cell);
        });

        // Parallel execution of neural networks and cell updates
        this.cellList.parallelStream().forEach(cell -> {
            CellCalcService.updateCell(cell, deltaTime);
            // Wrap position around environment boundaries
            this.wrapPosition(cell);
        });

        // Update cells and handle reproduction
        final List<Cell> newCells = new ArrayList<>();
        final Iterator<Cell> iterator = this.cellList.iterator();

        while (iterator.hasNext()) {
            final Cell cell = iterator.next();

            // Get nearby cell types for the neural network via partitioner
            //List<Cell> neighborCells = partitioner.getNeighbors(cell);
            //List<CellType> nearbyTypes = neighborCells.stream()
            //    .filter(other -> other != cell)
            //    .filter(other -> cell.getPosition().distance(other.getPosition()) <= config.getCellInteractionRadius())
            //    .map(Cell::getType)
            //    .toList();

            // Update position, rotation, and neural network
            //cell.updateWithNeighbors(deltaTime, nearbyTypes);

            // Handle reproduction.
            if (ReproductionManagerService.canReproduce(this.config, cell)) {
                final Cell childCell = ReproductionManagerService.reproduce(this.config, this, cell);
                if (Objects.nonNull(childCell)) {
                    newCells.add(childCell);
                }
            }

            // Remove cell after configured age.
            if ((cell.getAge() >= this.config.getCellDeathAge()) || (cell.getEnergy() <= 0.0D)) {
                this.lastDeadCell = cell;
                iterator.remove();
            }
        }

        // Add new cells from reproduction (skip in HIGH_ENERGY mode)
        this.cellList.addAll(newCells);

        // Repopulation (skip in HIGH_ENERGY mode)
        //if (config.getTrainMode() == TrainMode.NONE) {
        this.calcRepopulationIfNeeded();
        //}

        // Update statistics
        StatisticsManagerService.getInstance().update(this.cellList);
    }

    /**
     * Repopulation: when only REPOPULATION_THRESHOLD_PERCENT of initial cells remain, generate mutated offspring and split energy
     */
    private void calcRepopulationIfNeeded() {
        int currentCount = this.cellList.size();
        int initialCount = this.config.getInitialCellCount();
        // If all cells are dead, repopulate with random initial cells
        if (currentCount == 0) {
            if (this.lastDeadCell != null) {
                // Repopulate by mutating the last dead cell
                for (int i = 0; i < initialCount; i++) {
                    final Cell childCell = ReproductionManagerService.reproduce(this.config, this, this.lastDeadCell);
                    if (Objects.nonNull(childCell)) {
                        childCell.setEnergy(1.0D);
                        childCell.setMutationRateFactor(this.lastDeadCell.getMutationRateFactor());
                        childCell.setMutationStrengthFactor(this.lastDeadCell.getMutationStrengthFactor());
                        cellList.add(childCell);
                    }
                }
            } else {
                for (int i = 0; i < initialCount; i++) {
                    Vector2D pos = new Vector2D(this.random.nextDouble() * this.width, random.nextDouble() * this.height);
                    Cell newCell = CellFactory.createCell(pos, this.config.getCellMaxRadiusSize() / 2.0D);
                    cellList.add(newCell);
                }
            }
        } else {
            int thresholdCount = (int) Math.ceil(initialCount * SimulationConfig.REPOPULATION_THRESHOLD_PERCENT);
            if (currentCount < thresholdCount) {
                int toSpawn = initialCount - currentCount;
                for (int i = 0; i < toSpawn; i++) {
                    Cell parentCell = this.cellList.get(random.nextInt(this.cellList.size()));
                    if (Objects.nonNull(parentCell)) {
                        Cell childCell = ReproductionManagerService.reproduce(this.config, this, parentCell);
                        if (Objects.nonNull(childCell)) {
                            childCell.setEnergy(1.0D);
                            childCell.setMutationRateFactor(parentCell.getMutationRateFactor());
                            childCell.setMutationStrengthFactor(parentCell.getMutationStrengthFactor());
                            this.cellList.add(childCell);
                        }
                    }
                }
            }
        }
    }

    private void wrapPosition(Cell cell) {
        Vector2D pos = cell.getPosition();
        double x = pos.getX();
        double y = pos.getY();

        // Wrap horizontally
        if (x < 0) x += this.width;
        if (x >= this.width) x -= this.width;

        // Wrap vertically
        if (y < 0) y += this.height;
        if (y >= this.height) y -= this.height;

        if (x != pos.getX() || y != pos.getY()) {
            cell.setPosition(new Vector2D(x, y));
        }
    }

    // Getter for current sun ray visuals
    public List<SunRay> getSunRayList() {
        return new ArrayList<>(sunRayList);
    }

    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    public void resetCells() {
        this.cellList.clear();
    }

    /**
     * Resets the cell population with the provided new generation.
     */
    public void resetCells(List<Cell> newCells) {
        this.cellList.clear();
        this.cellList.addAll(newCells);
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setHeight(double height) {
        this.height = height;
    }
}
