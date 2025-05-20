package de.lifecircles.model;

import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;

import java.util.*;

import java.io.*;

/**
 * Represents the simulation environment.
 * Manages physics simulation and cell interactions.
 */
public class Environment {
    private static final Random random = new Random();
    private final SimulationConfig config;
    private final EnergySunCalcService energySunCalcService;
    private final List<SunRay> sunRays;
    private double width;
    private double height;
    private final List<Cell> cells;
    private final List<Blocker> blockers;
    private Cell lastDeadCell;

    // Singleton-Instanz für einfachen Zugriff
    private static Environment instance;

    public Environment(double width, double height) {
        this.width = width;
        this.height = height;
        this.cells = new ArrayList<>();
        this.blockers = new ArrayList<>();
        this.config = SimulationConfig.getInstance();
        this.sunRays = new ArrayList<>();
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
        Blocker ground = new Blocker(
            new Vector2D(width/2, height - 15), // Position at bottom
            width,                              // Full width
            30,                                 // Height
            javafx.scene.paint.Color.GRAY,      // Color
            Blocker.BlockerType.GROUND          // Type
        );
        blockers.add(ground);
    }

    public void addSunBlocker(final int xStart, final int yStart, final int blockerWidth) {
        Blocker sunBlocker = new Blocker(
            new Vector2D(xStart, yStart), // Position at bottom
            blockerWidth,                              // Full width
            25,                                 // Height
            javafx.scene.paint.Color.GRAY,      // Color
            Blocker.BlockerType.PLATFORM          // Type
        );
        blockers.add(sunBlocker);
    }

    public void addWallBlocker(double x, double yTop, double yBottom) {
        final double wallHeight = yTop - yBottom;
        final double wallWidth = 25;
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
    public void update(final double deltaTime, final PartitioningStrategy partitioner) {
        // Calculate sun energy rays
        this.sunRays.clear();
        this.sunRays.addAll(
                this.energySunCalcService.calculateSunEnergy(
                        this.cells, this.blockers, this.width, this.height, this.config, deltaTime
                )
        );

        // Process repulsive forces
        RepulsionCellCalcService.processRepulsiveForces(cells, partitioner);
        // Process sensor/actor interactions
        SensorActorForceCellCalcService.processInteractions(cells, partitioner);
        // Process energy transfers between cells
        EnergyTransferCellCalcService.processEnergyTransfers(cells);

        this.cells.parallelStream().forEach(cell -> {
            // Apply viscosity
            //Vector2D viscousForce = cell.getVelocity().multiply(-VISCOSITY);
            final Vector2D viscousForce = cell.getVelocity().multiply(-SimulationConfig.getInstance().getViscosity());
            cell.applyForce(viscousForce, cell.getPosition());

            // Apply gravity
            //cell.applyForce(SimulationConfig.GRAVITY_VECTOR.multiply(cell.getRadiusSize()), cell.getPosition(), deltaTime);
            cell.applyForce(SimulationConfig.GRAVITY_VECTOR, cell.getPosition());

            // Handle blocker collisions after force application
            BlockerCellCalcService.handleBlockerCollisions(cell, this.blockers, deltaTime);
        });

        this.cells.parallelStream().forEach(cell -> {
            CellCalcService.updateForces(cell);
        });

        // Parallel execution of neural networks and cell updates
        this.cells.parallelStream().forEach(cell -> {
            CellCalcService.updateCell(cell, deltaTime);
            // Wrap position around environment boundaries
            this.wrapPosition(cell);
        });

        // Update cells and handle reproduction
        final List<Cell> newCells = new ArrayList<>();
        final Iterator<Cell> iterator = this.cells.iterator();

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
                final Cell childCell = ReproductionManagerService.reproduce(this.config, cell);
                if (Objects.nonNull(childCell)) {
                    newCells.add(childCell);
                }
            }

            // Remove cell after configured age.
            if ((cell.getAge() >= config.getCellDeathAge()) || (cell.getEnergy() <= 0.0D)) {
                lastDeadCell = cell;
                iterator.remove();
            }
        }

        // Add new cells from reproduction (skip in HIGH_ENERGY mode)
        cells.addAll(newCells);

        // Repopulation (skip in HIGH_ENERGY mode)
        //if (config.getTrainMode() == TrainMode.NONE) {
        this.calcRepopulationIfNeeded();
        //}

        // Update statistics
        StatisticsManagerService.getInstance().update(cells);
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
                for (int i = 0; i < initialCount; i++) {
                    Cell childCell = ReproductionManagerService.reproduce(this.config, this.lastDeadCell);
                    childCell.setEnergy(1.0D);
                    childCell.setMutationRateFactor(this.lastDeadCell.getMutationRateFactor());
                    childCell.setMutationStrengthFactor(this.lastDeadCell.getMutationStrengthFactor());
                    cells.add(childCell);
                }
            } else {
                for (int i = 0; i < initialCount; i++) {
                    Vector2D pos = new Vector2D(this.random.nextDouble() * this.width, random.nextDouble() * this.height);
                    Cell newCell = CellFactory.createCell(pos, this.config.getCellMaxRadiusSize() / 2.0D);
                    cells.add(newCell);
                }
            }
        } else {
            int thresholdCount = (int) Math.ceil(initialCount * SimulationConfig.REPOPULATION_THRESHOLD_PERCENT);
            if (currentCount < thresholdCount) {
                int toSpawn = initialCount - currentCount;
                for (int i = 0; i < toSpawn; i++) {
                    Cell parentCell = cells.get(random.nextInt(cells.size()));
                    if (Objects.nonNull(parentCell)) {
                        Cell childCell = ReproductionManagerService.reproduce(config, parentCell);
                        if (Objects.nonNull(childCell)) {
                            childCell.setEnergy(1.0D);
                            childCell.setMutationRateFactor(parentCell.getMutationRateFactor());
                            childCell.setMutationStrengthFactor(parentCell.getMutationStrengthFactor());
                            cells.add(childCell);
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

    /**
     * Speichert alle Zellen in eine Datei.
     */
    public void saveCellsToFile(String filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(cells);
        }
    }

    /**
     * Lädt Zellen aus einer Datei und ersetzt die aktuellen Zellen.
     */
    @SuppressWarnings("unchecked")
    public void loadCellsFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            List<Cell> loadedCells = (List<Cell>) ois.readObject();
            resetCells(loadedCells);
        }
    }
}
