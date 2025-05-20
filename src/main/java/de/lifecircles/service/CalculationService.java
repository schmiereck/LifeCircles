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
    private final Environment environment;
    private final AtomicBoolean running;
    private final AtomicBoolean paused;
    private volatile SimulationStateDto latestState;
    private final Object stateLock = new Object();
    private final SimulationConfig config;
    private final TrainStrategy trainStrategy;
    private int updateFpsCount = 0;
    private long lastFpsTime = System.nanoTime();
    private volatile double fps = 0.0;
    private final AtomicLong stepCount = new AtomicLong(0);
    private final PartitioningStrategy partitioner;

    public CalculationService() {
        this.config = SimulationConfig.getInstance();
        this.trainStrategy =
                switch (config.getTrainMode()) {
                    case HIGH_ENERGY -> new HighEnergyTrainStrategy();
                    case HIGH_POSITION -> new HighPositionTrainStrategy();
                    case TEST_A -> new TestATrainStrategy();
                    case TEST_B -> new TestBTrainStrategy();
                    default -> new DefaultTrainStrategy();
                };
        this.environment = this.trainStrategy.initializeEnvironment();

        final double cellRadius = this.config.getCellMaxRadiusSize();
        final double fieldRadius = this.config.getCellActorMaxFieldRadius();
        final double interactionRadius = (cellRadius + fieldRadius) * 2.0D;

        this.partitioner = PartitioningStrategyFactory.createStrategy(
                this.environment.getWidth(), this.environment.getHeight(), interactionRadius);

        this.running = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
        this.initializeSimulation();
    }

    private void initializeSimulation() {
        trainStrategy.initialize(environment);
        updateState();
    }

    @Override
    public void run() {
        this.running.set(true);
        long lastUpdateTime = System.nanoTime();
        final double calcTimeStep = this.config.getCalcTimeStep();

        while (this.running.get()) {
            if (!this.paused.get()) {
                long currentTime = System.nanoTime();
                double deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0;
                final double targetRunTimeStep = this.config.getRunTimeStep();

                if (deltaTime >= targetRunTimeStep) {
                    this.update(calcTimeStep);

                    // FPS tracking
                    this.updateFpsCount++;
                    this.stepCount.incrementAndGet();
                    long nowFps = System.nanoTime();
                    if (nowFps - this.lastFpsTime >= 1_000_000_000L) {
                        this.fps = this.updateFpsCount / ((nowFps - this.lastFpsTime) / 1_000_000_000.0D);
                        this.updateFpsCount = 0;
                        this.lastFpsTime = nowFps;
                    }

                    lastUpdateTime = currentTime;
                }
            }

            // Reduziere Thread.sleep auf 0, um Verzögerungen zu minimieren
            try {
                Thread.sleep(0, 500_000); // 0.5 ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void update(final double deltaTime) {
        final List<Cell> cells = this.environment.getCells();

        // Update all cells with their neighborhood information
        this.partitioner.build(cells);

        // Update environment physics
        this.environment.update(deltaTime, this.partitioner);

        // Strategy-based selection/mutation
        this.trainStrategy.selectAndMutate(this.environment);
    }

    private void updateState() {
        List<SimulationStateDto.CellStateDto> cellStates = new ArrayList<>();

        for (Cell cell : environment.getCells()) {
            if (Objects.nonNull(cell)) {
                List<SimulationStateDto.ActorStateDto> actorStateDtos = new ArrayList<>();

                for (SensorActor actor : cell.getSensorActors()) {
                    CellType actorType = actor.getType();
                    actorStateDtos.add(new SimulationStateDto.ActorStateDto(
                            actor.calcPosition(),
                            new double[]{actorType.getRed(), actorType.getGreen(), actorType.getBlue()},
                            actor.getForceStrength()
                    ));
                }

                CellType cellType = cell.getType();
                cellStates.add(new SimulationStateDto.CellStateDto(
                        cell.getPosition(),
                        cell.getRotation(),
                        cell.getRadiusSize(),
                        new double[]{cellType.getRed(), cellType.getGreen(), cellType.getBlue()},
                        actorStateDtos,
                        cell.getEnergy(),
                        cell.getAge(),
                        cell.getCellState() // Zell-Zustand hinzufügen
                ));
            }
        }

        synchronized (this.stateLock) {
            this.latestState = new SimulationStateDto(
                cellStates,
                this.environment.getBlockers(),
                this.environment.getSunRays(),
                this.environment.getWidth(),
                this.environment.getHeight()
            );
        }
    }
    /**
     * Findet eine Zelle an der angegebenen Position in der Simulation.
     * @param worldX X-Koordinate in der Simulationswelt
     * @param worldY Y-Koordinate in der Simulationswelt
     * @return Die gefundene Zelle oder null, wenn keine Zelle an der Position ist
     */
    public Cell findCellAt(double worldX, double worldY) {
        // Zugriff auf das Environment und die Zellen
        if (environment == null) return null;

        // Durchsuche die Zellen und finde die, die an der Klickposition ist
        for (Cell cell : environment.getCells()) {
            double distance = new Vector2D(worldX, worldY).distance(cell.getPosition());
            if (distance <= cell.getRadiusSize()) {
                return cell; // Zelle gefunden
            }
        }

        return null; // Keine Zelle gefunden
    }

    public SimulationStateDto getLatestState() {
        synchronized (this.stateLock) {
            this.updateState();
           return this.latestState;
        }
    }

    public void start() {
        if (!this.running.get()) {
            new Thread(this).start();
        }
        this.paused.set(false);
    }

    public void pause() {
        this.paused.set(true);
    }

    public void resume() {
        this.paused.set(false);
    }

    public void stop() {
        running.set(false);
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
}
