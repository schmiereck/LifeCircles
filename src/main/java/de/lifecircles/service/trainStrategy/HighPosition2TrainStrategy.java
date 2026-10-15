package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.service.SimulationConfig;

import java.util.*;

/**
 * Training-Strategie: HighPosition.
 * Start mit SeperatorCount Zellen, selektiert alle GENERATION_STEP Schritte die Top SeperatorCount,
 * erzeugt mutierte Nachkommen und ersetzt die Population.
 *
 * Die Gewinner-Zellen werden global aus allen Zellen ausgewählt,
 * indem sie zuerst nach Y-Position (höchste zuerst) und dann
 * nach Nähe zur Abschnittsmitte sortiert werden.
 * Die besten N Zellen (N = SeperatorCount) werden als Gewinner verwendet.
 * Damit werden beide Kriterien – Höhe und Zentrierung – bei der globalen Auswahl berücksichtigt.
 */
public class HighPosition2TrainStrategy implements TrainStrategy {
    private static final int GENERATION_STEP = 1500 * 2; // Anzahl der Schritte bis zur Selektion
    private final SimulationConfig config = SimulationConfig.getInstance();
    private final Random random = new Random();
    private long stepCounter = 0;

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

    // Gewichtungsfaktoren
    private static final double POSITION_WEIGHT = 0.7D; // Gewichtung der Y-Position (höher = wichtiger)
    private static final double DISTANCE_WEIGHT = 0.3D; // Gewichtung des Abstands zur Mitte
    //private static final double POSITION_WEIGHT = 1.0D; // Gewichtung der Y-Position (höher = wichtiger)
    //private static final double DISTANCE_WEIGHT = 0.0D; // Gewichtung des Abstands zur Mitte

    @Override
    public void selectAndMutate(Environment environment) {
        stepCounter++;
        if (stepCounter % GENERATION_STEP != 0) {
            return;
        }
        List<Cell> cells = environment.getCellList();
        if (cells.isEmpty()) {
            return;
        }
        double xSpace = (environment.getWidth() / SeperatorCount);

        final double minHeight = cells.stream().
                min((c1, c2) -> Double.compare(c1.getPosition().getY(), c2.getPosition().getY()))
                .map(cell -> cell.getPosition().getY()).orElse(0.0D);
        final double maxHeight = cells.stream().
                max((c1, c2) -> Double.compare(c1.getPosition().getY(), c2.getPosition().getY()))
                .map(cell -> cell.getPosition().getY()).orElse(0.0D);
        final double heightRange = maxHeight - minHeight;

        // Lower scores prefer higher cells and positions near the section center.
        Map<Cell, Double> scores = new IdentityHashMap<>();
        for (Cell cell : cells) {
            double x = cell.getPosition().getX();
            double middle = Math.floor(x / xSpace) * xSpace + xSpace / 2;
            double distance = Math.abs(x - middle) / (xSpace / 2);
            double height = heightRange > 0 ? (cell.getPosition().getY() - minHeight) / heightRange : .5;
            scores.put(cell, POSITION_WEIGHT * height + DISTANCE_WEIGHT * distance);
        }
        final List<Cell> sortedCellList = cells.stream().sorted(Comparator.comparingDouble(scores::get)).toList();

        List<Cell> winners = new ArrayList<>();
        int[] selectedPerSection = new int[SeperatorCount];
        int maxPerSection = 2;//Math.max(1, cells.size() / SeperatorCount);

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

        TrainingPopulationBuilder.build(environment, winners, SeperatorCount, true, random)
                .ifPresent(environment::resetCells);
    }
}
