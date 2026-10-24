package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.dto.SimulationStateDto;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategyFactory;
import de.lifecircles.service.trainStrategy.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service responsible for running the simulation calculations in a separate thread.
 */
public class CalculationService implements Runnable {
    private Environment environment;
    private final AtomicBoolean running;
    private final AtomicBoolean paused;
    private volatile SimulationStateDto latestState;
    /** While running, the render thread asks for a snapshot; the calculation thread captures it after a step. */
    private volatile boolean snapshotRequested;
    private volatile boolean snapshotActorsRequested = true;
    /**
     * Fair, so threads waiting between steps (selection, detail view, loading, status) get the lock at the next
     * release although the calculation thread steps again at once.
     */
    private final java.util.concurrent.locks.ReentrantLock simulationLock = new java.util.concurrent.locks.ReentrantLock(true);
    private long stateVersion;
    private long snapshotVersion = -1;
    private boolean snapshotIncludesActors = true;
    private boolean stateReadable = true;
    private final SimulationConfig config;
    private TrainStrategy trainStrategy;
    private Thread worker;
    private int updateFpsCount = 0;
    private long lastFpsTime = System.nanoTime();
    private volatile double fps = 0.0;
    private final AtomicLong stepCount = new AtomicLong(0);
    private PartitioningStrategy partitioner;

    public CalculationService() {
        this.config = SimulationConfig.getInstance();
        this.running = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(true);
        newSimulation(config.getTrainMode());
    }

    /** Replace world, strategy, spatial index and snapshot together; leave the new world paused. */
    public void newSimulation(TrainMode mode) {
        Objects.requireNonNull(mode);
        simulationLock.lock(); try {
            paused.set(true);
            this.trainStrategy = strategyFor(mode);
            this.environment = this.trainStrategy.initializeEnvironment();
            double interactionRadius = (config.getCellMaxRadiusSize() + config.getCellActorMaxFieldRadius()) * 2;
            this.partitioner = PartitioningStrategyFactory.createStrategy(
                    environment.getWidth(), environment.getHeight(), interactionRadius);
            config.setTrainMode(mode);
            resetCounters();
            this.initializeSimulation();
            StatisticsManagerService.getInstance().reset(environment.getCellList());
        } finally { simulationLock.unlock(); }
    }

    public static TrainStrategy strategyFor(TrainMode mode) {
        return switch (mode) {
            case CARRION_RAIN -> new CarrionRainTrainStrategy();
            case CARRION_CONVERSION -> new CarrionRainTrainStrategy(EnergyType.SECONDARY);
            case HIGH_ENERGY -> new HighEnergyTrainStrategy();
            case HIGH_POSITION -> new HighPositionTrainStrategy();
            case HIGH_POSITION2 -> new HighPosition2TrainStrategy();
            case HIGH_POSITION3 -> new HighPosition3TrainStrategy();
            case WORLD2 -> new World2TrainStrategy();
            case RACE -> new RaceTrainStrategy();
            case TEST_A -> new TestATrainStrategy();
            case TEST_B -> new TestBTrainStrategy();
            case TEST_C -> new TestCTrainStrategy();
            default -> new DefaultTrainStrategy();
        };
    }

    public void resetSimulation() {
        simulationLock.lock(); try {
            paused.set(true);
            stateReadable=false;
            environment.resetCells();
            environment.getBlockerList().clear();
            environment.getSunRayList().clear();
            trainStrategy.initialize(environment);
            resetCounters();
            stateVersion++;
            stateReadable=true;
            updateState();
            StatisticsManagerService.getInstance().reset(environment.getCellList());
        } finally { simulationLock.unlock(); }
    }

    private void resetCounters() {
        stepCount.set(0);
        fps=0;
        updateFpsCount=0;
        lastFpsTime=System.nanoTime();
    }

    /** Read outside the simulation lock; publish the complete import between steps. */
    public void loadCellsFromFile(String filePath, boolean merge) throws java.io.IOException, ClassNotFoundException {
        List<Cell> loaded = FileService.getInstance().loadCellsFromFileWithProgress(filePath);
        importCells(loaded, merge);
        System.out.println((merge ? "Zusammenführen abgeschlossen: " : "Population ersetzt: ") + loaded.size() + " Zellen");
    }

    void importCells(List<Cell> loaded, boolean merge) {
        simulationLock.lock(); try {
            List<Cell> previous = new ArrayList<>(environment.getCellList());
            try {
                for(Cell cell:loaded)cell.migrateEnergyUsage();
                if (merge) environment.getCellList().addAll(loaded);
                else environment.resetCells(loaded);
                trainStrategy.onPopulationImported(environment, List.copyOf(loaded), merge);
            } catch (RuntimeException exception) {
                environment.resetCells(previous);
                throw exception;
            }
            stateVersion++;
            // Loading must be visible even before Start or while paused, in either LOD mode.
            updateState(snapshotIncludesActors);
        } finally { simulationLock.unlock(); }
    }

    public void initializeSimulation() {
        simulationLock.lock(); try {
            stateReadable = false;
            this.trainStrategy.initialize(this.environment);
            stateVersion++;
            stateReadable = true;
            this.updateState();
        } finally { simulationLock.unlock(); }
    }

    // Deterministic headless worlds use the same update/snapshot path as the application.
    CalculationService(Environment environment, TrainStrategy strategy, PartitioningStrategy partitioner) {
        this.environment = environment;
        this.trainStrategy = strategy;
        this.partitioner = partitioner;
        this.config = SimulationConfig.getInstance();
        this.running = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
        this.initializeSimulation();
    }

    @Override
    public void run() {
        long lastUpdateTime = System.nanoTime();
        final double calcTimeStep = this.config.getCalcTimeStep();

        while (this.running.get()) {
            if (!this.paused.get()) {
                long currentTime = System.nanoTime();
                double deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0;
                final double targetRunTimeStep = this.config.getRunTimeStep();

                if (deltaTime >= targetRunTimeStep) {
                    simulationLock.lock(); try {
                        // A scenario change or Pause may have arrived after the outer check.
                        if (!running.get() || paused.get()) continue;
                        this.update(calcTimeStep);
                        // Reset and scenario replacement also clear these counters under this lock.
                        this.updateFpsCount++;
                        long nowFps = System.nanoTime();
                        if (nowFps - this.lastFpsTime >= 1_000_000_000L) {
                            this.fps = this.updateFpsCount / ((nowFps - this.lastFpsTime) / 1_000_000_000.0D);
                            this.updateFpsCount = 0;
                            this.lastFpsTime = nowFps;
                        }
                        lastUpdateTime = currentTime;
                        // The render thread never waits for a step: it reads the last published snapshot.
                        if (snapshotRequested) {
                            snapshotRequested = false;
                            updateState(snapshotActorsRequested);
                        }
                    } finally { simulationLock.unlock(); }
                    // Behind the target pace: step again at once (the fair lock still lets waiting threads in).
                    continue;
                }
            }

            // Ahead of the target pace or paused. Thread.sleep(0, 500_000) sleeps about 1.25 ms on Windows,
            // so short waits spin politely instead.
            try {
                long ahead = (long) (config.getRunTimeStep() * 1e9) - (System.nanoTime() - lastUpdateTime);
                if (paused.get() || ahead > 1_300_000L) Thread.sleep(1);
                else Thread.onSpinWait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    void update(final double deltaTime) {
        simulationLock.lock(); try {
            stateReadable = false;
            this.trainStrategy.beforeStep(this.environment, deltaTime);
            final List<Cell> cellList = this.environment.getCellList();

            // Update all cells with their neighborhood information
            long phaseStart = environment.getProfiler().start();
            this.partitioner.build(cellList);
            environment.getProfiler().finish(SimulationProfiler.Phase.GRID, phaseStart);

            // Update environment physics
            this.environment.update(deltaTime, this.partitioner, this.trainStrategy.stepPolicy());

            // Strategy-based selection/mutation
            phaseStart = environment.getProfiler().start();
            this.trainStrategy.selectAndMutate(this.environment, deltaTime);
            environment.getProfiler().finish(SimulationProfiler.Phase.SELECTION, phaseStart);
            this.stepCount.incrementAndGet();
            stateVersion++;
            stateReadable = true;
        } finally { simulationLock.unlock(); }
    }

    public String getTrainingStatus() { simulationLock.lock(); try { return trainStrategy.getStatus(); } finally { simulationLock.unlock(); } }

    /** Caller holds simulationLock across the complete capture; the volatile field publishes it. */
    private void updateState() {
        updateState(true);
    }

    private void updateState(boolean includeActors) {
        long phaseStart = environment.getProfiler().start();
        final List<SimulationStateDto.CellStateDto> cellStates = new ArrayList<>();

        for (final Cell cell : this.environment.getCopyOfCellList()) {
            if (Objects.nonNull(cell)) {
                final List<SimulationStateDto.ActorStateDto> actorStateDtos = new ArrayList<>();

                if (includeActors) for (final SensorActor actor : cell.getSensorActors()) {
                    final CellType actorType = actor.getType();
                    actorStateDtos.add(new SimulationStateDto.ActorStateDto(
                            actor.calcPosition(),
                            new double[]{actorType.getRed(), actorType.getGreen(), actorType.getBlue()},
                            actor.getForceStrength()
                    ));
                }

                final CellType cellType = cell.getType();
                double[] additionalEnergies = new double[EnergyType.ALL.size() - 2];
                for (EnergyType energyType : EnergyType.ALL) if (energyType.getCode() >= 2)
                    additionalEnergies[energyType.getCode() - 2] = cell.getEnergy(energyType);
                cellStates.add(new SimulationStateDto.CellStateDto(
                        cell.getPosition(),
                        cell.getRotation(),
                        cell.getRadiusSize(),
                        new double[]{cellType.getRed(), cellType.getGreen(), cellType.getBlue()},
                        actorStateDtos,
                        cell.getEnergy(),
                        cell.getEnergy(EnergyType.SECONDARY),
                        additionalEnergies,
                        cell.getAge(),
                        cell.getCellState(),
                        cell.isRemains()
                ));
            }
        }

        this.latestState = new SimulationStateDto(
                cellStates,
                this.environment.getBlockerList(),
                this.environment.getSunRayList(),
                this.environment.getWidth(),
                this.environment.getHeight()
            );
        snapshotVersion = stateVersion;
        snapshotIncludesActors = includeActors;
        environment.getProfiler().finish(SimulationProfiler.Phase.SNAPSHOT, phaseStart);
    }
    /**
     * Findet eine Zelle an der angegebenen Position in der Simulation.
     * @param worldX X-Koordinate in der Simulationswelt
     * @param worldY Y-Koordinate in der Simulationswelt
     * @return Die gefundene Zelle oder null, wenn keine Zelle an der Position ist
     */
    public Cell findCellAt(double worldX, double worldY) {
        simulationLock.lock(); try {
            if (!stateReadable) return null;
            for (Cell cell : environment.getCellList()) {
                double distance = new Vector2D(worldX, worldY).getShortestDirectionWithWrapAround(
                        cell.getPosition(), environment.getWidth(), environment.getHeight()).length();
                if (distance <= cell.getRadiusSize()) {
                    return cell;
                }
            }
            return null;
        } finally { simulationLock.unlock(); }
    }

    public SimulationStateDto getLatestState() {
        return getLatestState(true);
    }

    /**
     * Overview snapshots omit actor payload; full snapshots remain the default API. While the simulation runs, the
     * snapshot is captured by the calculation thread after its next step and this returns the last published one
     * at once, so rendering neither waits for nor holds up a step.
     */
    public SimulationStateDto getLatestState(boolean includeActors) {
        SimulationStateDto published = latestState;
        if (running.get() && !paused.get() && published != null) {
            snapshotActorsRequested = includeActors;
            snapshotRequested = true;
            return published;
        }
        simulationLock.lock(); try {
            if (stateReadable && (snapshotVersion != stateVersion || snapshotIncludesActors != includeActors))
                this.updateState(includeActors);
            return this.latestState;
        } finally { simulationLock.unlock(); }
    }

    /** Reads live detail/network data between complete simulation steps. Do not mutate it. */
    public void readSimulationState(Runnable reader) {
        simulationLock.lock(); try {
            if (stateReadable) reader.run();
        } finally { simulationLock.unlock(); }
    }

    public synchronized void start() {
        if (this.running.compareAndSet(false,true)) {
            worker=new Thread(this,"LifeCircles calculation");
            worker.start();
        }
        this.paused.set(false);
    }

    public void pause() {
        simulationLock.lock(); try { this.paused.set(true); } finally { simulationLock.unlock(); }
    }

    public void resume() {
        this.paused.set(false);
    }

    public synchronized void stop() {
        this.running.set(false);
        if(worker!=null && worker!=Thread.currentThread()) {
            worker.interrupt();
            try { worker.join(); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); }
            worker=null;
        }
    }

    public boolean isPaused() {
        return this.paused.get();
    }

    public boolean isRunning() {
        return this.running.get();
    }

    public double getFps() {
        return this.fps;
    }
    public long getStepCount() {
        return this.stepCount.get();
    }

    /** Continues the count of a loaded world (steps stored beside its cells file). */
    public void setStepCount(long steps) {
        if (steps < 0) throw new IllegalArgumentException("Step count must be >= 0");
        this.stepCount.set(steps);
    }
}
