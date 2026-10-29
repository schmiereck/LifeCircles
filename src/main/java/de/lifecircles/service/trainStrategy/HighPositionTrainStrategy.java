package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellFactory;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.SimulationConfig;

import java.util.*;

/**
 * Training-Strategie: HighPosition.
 * Start mit 16 Zellen, selektiert alle 10000 Schritte die besten 16 nach Höhe über dem Boden,
 * erzeugt mutierte Nachkommen und ersetzt die Population.
 */
public class HighPositionTrainStrategy implements TrainStrategy {
    private static final int GENERATION_STEP = 2500 * 4;
    private final SimulationConfig config = SimulationConfig.getInstance();
    private final Random random = new Random();
    private long stepCounter = 0;
    // Footer information (like RACE and HIGH_POSITION3); counts since the scenario started, not stored in saves.
    private int generation = 1;
    private double elapsed;
    private Double lastBest;
    private volatile String status = "";

    @Override
    public Environment initializeEnvironment() {
        this.config.setWidth(1600 * 4.0D);
        this.config.setHeight(1200);

        this.config.setScaleSimulation(1.6D * 4.0D);
        //this.config.setViscosity(5.75D * 2.0D);

        //this.config.setEnergyPerRay(0.015D); // 0.005; //0.015; // 0.025;

        return new Environment(config.getWidth(), config.getHeight());
    }

    private final static int SeperatorCount = 8 * 2;

    @Override
    public void initialize(Environment environment) {
        this.stepCounter = 0;
        this.generation = 1; this.elapsed = 0; this.lastBest = null;
        updateStatus(environment);
        // Add ground blocker by default
        environment.addGroundBlocker();
        //environment.addSunBlocker();
        for (int posX = 0; posX <= SeperatorCount; posX++) {
            final double x = (config.getWidth() / SeperatorCount) * posX;
            final double yTop = config.getHeight() - (config.getHeight() / 2.0D);
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
            final double stateHiddenLayerSynapseConnectivity = 0.05D;
            //final double hiddenLayerSynapseConnectivity = SimulationConfig.brainSynapseConnectivityDefault;
            final double hiddenLayerSynapseConnectivity = 0.075D;
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
        stepCounter++;
        if (stepCounter % GENERATION_STEP != 0) {
            if (stepCounter % 30 == 0) updateStatus(environment);
            return;
        }
        generation++; elapsed = 0;
        List<Cell> cells = environment.getCellList().stream().filter(cell -> !cell.isRemains()).toList();
        if (cells.isEmpty()) {
            lastBest = null;
            updateStatus(environment);
            return;
        }
        int winnersCount = Math.min(SeperatorCount, cells.size());
        List<Cell> winners = cells.stream().sorted(Comparator.comparingDouble(c -> c.getPosition().getY()))
                .limit(winnersCount).toList();
        lastBest = heightAboveGround(winners.getFirst());
        TrainingPopulationBuilder.build(environment, winners, SeperatorCount, true, random)
                .ifPresent(environment::resetCells);
        updateStatus(environment);
    }

    /** Height of the cell's top above the ground surface in px. */
    private double heightAboveGround(Cell cell) {
        return config.getHeight() - Environment.GroundBlockerHeight - (cell.getPosition().getY() - cell.getRadiusSize());
    }

    /** Generation, time within it, living cells, current highest cell and the last winner's height. */
    private void updateStatus(Environment environment) {
        List<Cell> living = environment.getCellList().stream().filter(cell -> !cell.isRemains()).toList();
        double highest = living.stream().mapToDouble(this::heightAboveGround).max().orElse(Double.NaN);
        status = String.format(Locale.ROOT, "HIGH_POSITION · G%d · Schritt %d/%d · %.0f s · %d Zellen · höchste %s · letzte Bestwertung %s",
                generation, stepCounter % GENERATION_STEP, GENERATION_STEP, elapsed, living.size(),
                Double.isNaN(highest) ? "—" : String.format(Locale.ROOT, "%.0f px", highest),
                lastBest == null ? "—" : String.format(Locale.ROOT, "%.0f px", lastBest));
    }
}
