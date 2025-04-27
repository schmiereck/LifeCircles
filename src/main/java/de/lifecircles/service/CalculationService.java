package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellType;
import de.lifecircles.model.Environment;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.TrainMode;
import de.lifecircles.service.TrainStrategy;
import de.lifecircles.service.HighEnergyTrainStrategy;
import de.lifecircles.service.DefaultTrainStrategy;
import de.lifecircles.service.HighPositionTrainStrategy;
import de.lifecircles.service.dto.SimulationState;
import de.lifecircles.service.ActorSensorCellCalcService;
import de.lifecircles.service.BlockerCellCalcService;
import de.lifecircles.service.PartitioningStrategy;
import de.lifecircles.service.SpatialGridPartitioningStrategy;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service responsible for running the simulation calculations in a separate thread.
 */
public class CalculationService implements Runnable {
    private final Environment environment;
    private final AtomicBoolean running;
    private final AtomicBoolean paused;
    private volatile SimulationState latestState;
    private final Object stateLock = new Object();
    private final SimulationConfig config;
    private final TrainStrategy trainStrategy;
    private int updateCount = 0;
    private long lastFpsTime = System.nanoTime();
    private volatile double fps = 0.0;
    private final AtomicLong stepCount = new AtomicLong(0);

    public CalculationService() {
        this.config = SimulationConfig.getInstance();
        this.environment = new Environment(config.getWidth(), config.getHeight());
        this.running = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
        this.trainStrategy = 
            switch (config.getTrainMode()) {
                case HIGH_ENERGY -> new HighEnergyTrainStrategy();
                case HIGH_POSITION -> new HighPositionTrainStrategy();
                default -> new DefaultTrainStrategy();
            };
        initializeSimulation();
    }

    private void initializeSimulation() {
        trainStrategy.initialize(environment);
        updateState();
    }

    @Override
    public void run() {
        running.set(true);
        long lastUpdateTime = System.nanoTime();
        double targetDelta = 1.0 / config.getTargetUpdatesPerSecond();

        while (running.get()) {
            if (!paused.get()) {
                long currentTime = System.nanoTime();
                double deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0;

                if (deltaTime >= targetDelta) {
                    update(config.getTimeStep());
                    // FPS tracking
                    updateCount++;
                    stepCount.incrementAndGet();
                    long nowFps = System.nanoTime();
                    if (nowFps - lastFpsTime >= 1_000_000_000L) {
                        fps = updateCount / ((nowFps - lastFpsTime) / 1_000_000_000.0);
                        updateCount = 0;
                        lastFpsTime = nowFps;
                    }
                    lastUpdateTime = currentTime;
                }
            }

            // Small sleep to prevent excessive CPU usage
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void update(final double deltaTime) {
        // Retrieve cells and process sensor/actor interactions
        final List<Cell> cells = environment.getCells();
        ActorSensorCellCalcService.processInteractions(cells, deltaTime);

        // Update all cells with their neighborhood information
        final double interactionRadius = config.getCellInteractionRadius();
        final PartitioningStrategy partitioner = PartitioningStrategyFactory.createStrategy(
            environment.getWidth(), environment.getHeight(), interactionRadius
        );
        partitioner.build(cells);
        
        // Parallel execution of neural networks and cell updates
        cells.parallelStream().forEach(cell -> {
            List<CellType> neighborTypes = new ArrayList<>();
            for (Cell other : partitioner.getNeighbors(cell)) {
                if (other != cell && cell.getPosition().distance(other.getPosition()) <= interactionRadius) {
                    neighborTypes.add(other.getType());
                }
            }
            cell.updateWithNeighbors(deltaTime, neighborTypes);
        }
        );

        // Update environment physics
        environment.update(deltaTime);

        // Strategy-based selection/mutation
        trainStrategy.selectAndMutate(environment);

        // Update state for visualization
        updateState();
    }

    private void updateState() {
        List<SimulationState.CellState> cellStates = new ArrayList<>();

        for (Cell cell : environment.getCells()) {
            List<SimulationState.ActorState> actorStates = new ArrayList<>();
            
            for (SensorActor actor : cell.getSensorActors()) {
                CellType actorType = actor.getType();
                actorStates.add(new SimulationState.ActorState(
                    actor.getPosition(),
                    new double[]{actorType.getRed(), actorType.getGreen(), actorType.getBlue()},
                    actor.getForceStrength()
                ));
            }

            CellType cellType = cell.getType();
            cellStates.add(new SimulationState.CellState(
                cell.getPosition(),
                cell.getRotation(),
                cell.getSize(),
                new double[]{cellType.getRed(), cellType.getGreen(), cellType.getBlue()},
                actorStates,
                cell.getEnergy(),
                cell.getAge()
            ));
        }

        synchronized (stateLock) {
            latestState = new SimulationState(
                cellStates,
                environment.getBlockers(),
                environment.getSunRays(),
                environment.getWidth(),
                environment.getHeight()
            );
        }
    }

    public SimulationState getLatestState() {
        synchronized (stateLock) {
            return latestState;
        }
    }

    public void start() {
        if (!running.get()) {
            new Thread(this).start();
        }
        paused.set(false);
    }

    public void pause() {
        paused.set(true);
    }

    public void resume() {
        paused.set(false);
    }

    public void stop() {
        running.set(false);
    }

    public boolean isPaused() {
        return paused.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    public double getFps() {
        return fps;
    }
    public long getStepCount() {
        return stepCount.get();
    }
}
