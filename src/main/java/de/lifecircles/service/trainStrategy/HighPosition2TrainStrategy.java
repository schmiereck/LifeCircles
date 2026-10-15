package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.service.SimulationConfig;

import java.util.*;

/** Selects sustained height of ground-connected colonies during the final ten seconds. */
public class HighPosition2TrainStrategy implements TrainStrategy {
    private static final int GENERATION_STEP = 1500 * 2; // Anzahl der Schritte bis zur Selektion
    // The simulation currently makes one selection call per 60-Hz physics step.
    private static final int EVALUATION_WINDOW_STEPS = 600;
    private static final int SAMPLE_INTERVAL_STEPS = 30;
    private final SimulationConfig config = SimulationConfig.getInstance();
    private final Random random;
    private final EvaluationObserver observer;
    private long stepCounter = 0;
    private final GroundedColonyFitness fitness = new GroundedColonyFitness(SeperatorCount);

    @FunctionalInterface
    interface EvaluationObserver {
        /** Called between steps, before replacing the population. Observe only. */
        void observe(Environment world, GroundedColonyFitness fitness, List<Cell> winners);
    }

    public HighPosition2TrainStrategy() { this(new Random(), null); }

    HighPosition2TrainStrategy(Random random, EvaluationObserver observer) {
        this.random = Objects.requireNonNull(random);
        this.observer = observer;
    }

    @Override
    public Environment initializeEnvironment() {
        this.config.setWidth(1600 * 4.0D);
        this.config.setHeight(1200);

        this.config.setScaleSimulation(1.6D * 4.0D);
        //this.config.setViscosity(5.75D * 2.0D);

        this.config.setEnergyPerRay(0.005D); // 0.005; //0.015; // 0.025;
        this.config.setReproductionAgeThreshold(6.0D); // 8.0D

        return new Environment(config.getWidth(), config.getHeight());
    }

    private final static int SeperatorCount = 8 * 2;

    @Override
    public void initialize(Environment environment) {
        this.stepCounter = 0;
        this.fitness.reset();
        // Add ground blocker by default
        environment.addGroundBlocker();
        //environment.addSunBlocker();
        for (int posX = 0; posX <= SeperatorCount; posX++) {
            final double x = (config.getWidth() / SeperatorCount) * posX;
            final double yTop = Environment.GroundBlockerHeight + (config.getCellMaxRadiusSize() * 2.0D);
            final double yBottom = Environment.GroundBlockerHeight;
            environment.addWallBlocker(x, yTop, yBottom);
        }

        for (int posX = 0; posX < SeperatorCount; posX++) {
            final double xSpace = (config.getWidth() / SeperatorCount);
            final double x = xSpace * posX + xSpace / 2.0D;
            final double y = config.getHeight() - Environment.GroundBlockerHeight - config.getCellMaxRadiusSize();

            //final double hiddenCountFactor = SimulationConfig.hiddenCountFactorDefault;
            final double hiddenCountFactor = 0.5D;
            //final double stateHiddenLayerSynapseConnectivity = SimulationConfig.stateHiddenLayerSynapseConnectivityDefault;
            final double stateHiddenLayerSynapseConnectivity = 0.01D;
            //final double hiddenLayerSynapseConnectivity = SimulationConfig.brainSynapseConnectivityDefault;
            final double hiddenLayerSynapseConnectivity = 0.025D;
            environment.addCell(CellFactory.createCell(new Vector2D(x, y), config.getCellMaxRadiusSize() / 2.0,
                    hiddenCountFactor,
                    stateHiddenLayerSynapseConnectivity, hiddenLayerSynapseConnectivity));
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        stepCounter++;
        long phase = (stepCounter - 1) % GENERATION_STEP + 1;
        if (phase > GENERATION_STEP - EVALUATION_WINDOW_STEPS && phase % SAMPLE_INTERVAL_STEPS == 0)
            fitness.sample(environment);
        if (phase != GENERATION_STEP) {
            return;
        }
        List<Cell> cells = environment.getCellList();
        if (cells.isEmpty()) {
            fitness.reset();
            return;
        }
        double xSpace = (environment.getWidth() / SeperatorCount);

        final List<Cell> sortedCellList = fitness.ranked(environment);

        List<Cell> winners = new ArrayList<>();
        int[] selectedPerSection = new int[SeperatorCount];
        int maxPerSection = 2;

        for (Cell cell : sortedCellList) {
            int section = (int) (cell.getPosition().getX() / xSpace);
            if (section >= 0 && section < SeperatorCount && selectedPerSection[section] < maxPerSection) {
                winners.add(cell);
                selectedPerSection[section]++;
            }

            if (winners.size() >= SeperatorCount) {
                break;
            }
        }

        if (observer != null) observer.observe(environment,fitness,List.copyOf(winners));
        TrainingPopulationBuilder.build(environment, winners, SeperatorCount, true, random)
                .ifPresent(environment::resetCells);
        fitness.reset();
    }
}
