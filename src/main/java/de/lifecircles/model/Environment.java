package de.lifecircles.model;

import de.lifecircles.service.*;
import de.lifecircles.model.neural.CellBrainService;
import java.util.stream.IntStream;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;

import java.util.*;

/**
 * Represents the simulation environment.
 * Manages physics simulation and cell interactions.
 */
public class Environment {
    public static double GroundBlockerHeight = 50.0D;

    private final SimulationConfig config;
    private final EnergySunCalcService energySunCalcService;
    private final SimulationProfiler profiler = new SimulationProfiler();
    public SimulationProfiler getProfiler() { return profiler; }
    private final List<SunRay> sunRayList;
    private double width;
    private double height;
    private final List<Cell> cellList;
    private final List<Blocker> blockerList;

    // Singleton-Instanz für einfachen Zugriff
    private static Environment instance;

    public Environment(double width, double height) {
        this(width, height, new Random());
    }

    /** Seedable sunlight for reproducible continuations; ordinary worlds remain random. */
    public Environment(double width, double height, Random sunRandom) {
        this.width = width;
        this.height = height;
        this.cellList = new ArrayList<>();
        this.blockerList = new ArrayList<>();
        this.config = SimulationConfig.getInstance();
        this.sunRayList = new ArrayList<>();
        this.energySunCalcService = new EnergySunCalcService(sunRandom);

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
        update(deltaTime, partitioner, SimulationStepPolicy.NONE);
    }

    public void update(final double deltaTime, final PartitioningStrategy partitioner, final SimulationStepPolicy policy) {
        long phaseStart = profiler.start();
        // Calculate sun energy rays
        this.sunRayList.clear();
        if (policy.useSunlight()) this.sunRayList.addAll(
                this.energySunCalcService.calculateSunEnergy(
                        this.cellList, this.blockerList, this.width, this.height, this.config, deltaTime
                )
        );
        else this.cellList.forEach(Cell::resetSunRayHit);

        profiler.finish(SimulationProfiler.Phase.SUN, phaseStart);
        phaseStart = profiler.start();
        // Process repulsive forces
        RepulsionCellCalcService.processRepulsiveForces(this.cellList, partitioner, this);
        profiler.finish(SimulationProfiler.Phase.REPULSION, phaseStart);
        phaseStart = profiler.start();
        // Process sensor/actor interactions
        SensorActorForceCellCalcService.processInteractions(this.cellList, partitioner, this);
        profiler.finish(SimulationProfiler.Phase.INTERACTIONS, phaseStart);
        phaseStart = profiler.start();
        // Process energy transfers between cells
        EnergyTransferCellCalcService.processEnergyTransfers(this.cellList, this, deltaTime);
        profiler.finish(SimulationProfiler.Phase.TRANSFER, phaseStart);
        phaseStart = profiler.start();

        for (final Cell cell : this.cellList) {
        //this.cells.parallelStream().forEach(cell -> {
            // Apply viscosity
            //Vector2D viscousForce = cell.getVelocity().multiply(-VISCOSITY);
            final Vector2D viscousForce = cell.getVelocity().multiply(-SimulationConfig.getInstance().getViscosity());
            cell.applyForce(viscousForce, cell.getPosition());

            // Apply gravity
            //cell.applyForce(SimulationConfig.GRAVITY_VECTOR.multiply(cell.getRadiusSize()), cell.getPosition(), deltaTime);
            cell.applyForce(SimulationConfig.GRAVITY_VECTOR, cell.getPosition());


        }
        //);

        this.cellList.parallelStream().forEach(cell -> {
            CellCalcService.updateForces(cell, deltaTime);
        });

        profiler.finish(SimulationProfiler.Phase.INTEGRATION, phaseStart);
        phaseStart = profiler.start();
        // Freeze all sensed inputs before any worker changes energy, age or outputs.
        final List<double[]> brainInputs = this.cellList.stream().map(cell -> CellBrainService.generateInputs(cell, this)).toList();
        profiler.finish(SimulationProfiler.Phase.INPUTS, phaseStart);
        phaseStart = profiler.start();
        IntStream.range(0, this.cellList.size()).parallel().forEach(i -> {
            Cell cell = this.cellList.get(i);
            CellBrainService.think(cell, brainInputs.get(i));
            if (policy != SimulationStepPolicy.NONE) policy.afterThinking(cell);
        });
        profiler.finish(SimulationProfiler.Phase.BRAINS, phaseStart);
        phaseStart = profiler.start();
        IntStream.range(0, this.cellList.size()).parallel().forEach(i -> {
            Cell cell = this.cellList.get(i);
            CellCalcService.advanceCell(cell, deltaTime, policy.brainEnergyCosts());
            this.wrapPosition(cell);
            BlockerCellCalcService.handleBlockerCollisions(cell, this.blockerList, this);
            this.wrapPosition(cell);
            for (SensorActor actor : cell.getSensorActors()) actor.updateCachedPosition();
        });

        profiler.finish(SimulationProfiler.Phase.INTEGRATION, phaseStart);
        phaseStart = profiler.start();
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

            // Dead cells cannot reproduce or serve as an automatic restart seed.
            if (policy.isDead(cell)) {
                iterator.remove();
                continue;
            }

            // Living cells reproduce using only their available energy.
            if (policy.allowReproduction(cell) && ReproductionManagerService.canReproduce(this.config, cell)) {
                final Cell childCell = policy.reproduce(this, cell);
                if (Objects.nonNull(childCell)) {
                    policy.onBirth(cell, childCell);
                    newCells.add(childCell);
                }
            }

        }

        // Population growth comes only from regular, energy-sharing reproduction.
        this.cellList.addAll(newCells);
        profiler.finish(SimulationProfiler.Phase.REPRODUCTION, phaseStart);
        phaseStart = profiler.start();

        // Update statistics
        StatisticsManagerService.getInstance().update(this.cellList);
        profiler.finish(SimulationProfiler.Phase.STATISTICS, phaseStart);
    }

    private void wrapPosition(Cell cell) {
        Vector2D pos = cell.getPosition();
        double x = pos.getX();
        double y = pos.getY();

        // Wrap horizontally
        x = Vector2D.wrapCoordinate(x, this.width);

        // Wrap vertically
        y = Vector2D.wrapCoordinate(y, this.height);

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
