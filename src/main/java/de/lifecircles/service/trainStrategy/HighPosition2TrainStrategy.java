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
    /** Steps of the running generation: from the config at its start, unless a study fixed them. */
    private int generationSteps;
    private final boolean fixedLength;
    private final GroundedColonyFitness fitness = new GroundedColonyFitness(SeperatorCount);
    // Footer information (like RACE and HIGH_POSITION3); counts since the scenario started, not stored in saves.
    private int generation = 1;
    private double elapsed;
    private Double lastBest;
    private volatile String status = "";

    @FunctionalInterface
    interface EvaluationObserver {
        /** Called between steps, before replacing the population. Observe only. */
        void observe(Environment world, GroundedColonyFitness fitness, List<Cell> winners);
    }

    public HighPosition2TrainStrategy() { this(new Random(), null); }

    /** Generation length from the settings (highPosition2GenerationSeconds, default 50 s). */
    HighPosition2TrainStrategy(Random random, EvaluationObserver observer) {
        this.random = Objects.requireNonNull(random);
        this.observer = observer;
        this.fixedLength = false;
        this.generationSteps = configuredSteps();
    }

    /** Studies may lengthen generations; the evaluation window stays the final ten seconds. */
    HighPosition2TrainStrategy(Random random, EvaluationObserver observer, int generationSteps) {
        if (generationSteps <= EVALUATION_WINDOW_STEPS) throw new IllegalArgumentException("Generation must exceed the evaluation window");
        this.generationSteps = generationSteps;
        this.random = Objects.requireNonNull(random);
        this.observer = observer;
        this.fixedLength = true;
    }

    /** One selection call per 60-Hz physics step: 50 s are the former 3000 steps. */
    private int configuredSteps() {
        return Math.max(EVALUATION_WINDOW_STEPS + SAMPLE_INTERVAL_STEPS,
                (int) Math.round(config.getHighPosition2GenerationSeconds() * GENERATION_STEP / 50.0));
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
        if (!fixedLength) this.generationSteps = configuredSteps();
        this.generation = 1; this.elapsed = 0; this.lastBest = null;
        updateStatus(environment);
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

    @Override public String getStatus() { return status; }

    @Override
    public void selectAndMutate(Environment environment, double deltaTime) {
        elapsed += deltaTime;
        selectAndMutate(environment);
    }

    @Override
    public void selectAndMutate(Environment environment) {
        long phase = ++stepCounter;
        if (phase > generationSteps - EVALUATION_WINDOW_STEPS && phase % SAMPLE_INTERVAL_STEPS == 0)
            fitness.sample(environment);
        if (phase != generationSteps) {
            if (phase % SAMPLE_INTERVAL_STEPS == 0) updateStatus(environment);
            return;
        }
        generation++; elapsed = 0; stepCounter = 0;
        if (!fixedLength) generationSteps = configuredSteps();
        List<Cell> cells = environment.getCellList().stream().filter(cell -> !cell.isRemains()).toList();
        if (cells.isEmpty()) {
            fitness.reset();
            lastBest = null;
            updateStatus(environment);
            return;
        }
        double xSpace = (environment.getWidth() / SeperatorCount);

        final List<Cell> sortedCellList = fitness.ranked(environment).stream().filter(cell -> !cell.isRemains()).toList();

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

        lastBest = winners.isEmpty() ? null : fitness.persistentScore(winners.getFirst());
        if (observer != null) observer.observe(environment,fitness,List.copyOf(winners));
        TrainingPopulationBuilder.build(environment, winners, SeperatorCount, true, random)
                .ifPresent(environment::resetCells);
        fitness.reset();
        updateStatus(environment);
    }

    /** Generation, time within it, phase, living cells and the last winner's sustained height (0..1 of the floor). */
    private void updateStatus(Environment environment) {
        long phase = stepCounter;
        long living = environment.getCellList().stream().filter(cell -> !cell.isRemains()).count();
        status = String.format(Locale.ROOT, "HIGH_POSITION2 · G%d · Schritt %d/%d · %.0f s · %s · %d Zellen · letzte Bestwertung %s",
                generation, phase, generationSteps, elapsed,
                phase > generationSteps - EVALUATION_WINDOW_STEPS ? "Bewertung" : "Wachstum", living,
                lastBest == null ? "—" : String.format(Locale.ROOT, "%.3f", lastBest));
    }
}
