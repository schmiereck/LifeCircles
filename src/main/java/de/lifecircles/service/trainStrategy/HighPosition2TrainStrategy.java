package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.service.ReproductionManagerService;
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
    private static final int GENERATION_STEP = 2500 * 1; // Anzahl der Schritte bis zur Selektion
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

        return new Environment(config.getWidth(), config.getHeight());
    }

    private final static int SeperatorCount = 8 * 2;

    @Override
    public void initialize(Environment environment) {
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
            //final double brainSynapseConnectivity = SimulationConfig.brainSynapseConnectivityDefault;
            final double brainSynapseConnectivity = 0.025D;
            environment.addCell(CellFactory.createCell(new Vector2D(x, y), config.getCellMaxRadiusSize() / 2.0,
                    hiddenCountFactor, stateHiddenLayerSynapseConnectivity, brainSynapseConnectivity));
        }
    }

    // Gewichtungsfaktoren
    private static final double POSITION_WEIGHT = 0.8D; // Gewichtung der Y-Position (höher = wichtiger)
    private static final double DISTANCE_WEIGHT = 0.2D; // Gewichtung des Abstands zur Mitte
    //private static final double POSITION_WEIGHT = 1.0D; // Gewichtung der Y-Position (höher = wichtiger)
    //private static final double DISTANCE_WEIGHT = 0.0D; // Gewichtung des Abstands zur Mitte

    @Override
    public void selectAndMutate(Environment environment) {
        stepCounter++;
        if (stepCounter % GENERATION_STEP != 0) {
            return;
        }
        List<Cell> cells = environment.getCells();
        if (cells.isEmpty()) {
            return;
        }
        double xSpace = (config.getWidth() / SeperatorCount);

        final double minHeight = cells.stream().
                min((c1, c2) -> Double.compare(c1.getPosition().getY(), c2.getPosition().getY()))
                .map(cell -> cell.getPosition().getY()).orElse(0.0D);
        final double maxHeight = cells.stream().
                max((c1, c2) -> Double.compare(c1.getPosition().getY(), c2.getPosition().getY()))
                .map(cell -> cell.getPosition().getY()).orElse(0.0D);
        final double heightRange = maxHeight - minHeight;

        // Sortiere Zellen nach einer gewichteten Kombination aus Y-Position und Nähe zur Mitte
        final List<Cell> sortedCellList = cells.stream().
                sorted((c1, c2) -> {
                    double c1x = c1.getPosition().getX();
                    double c2x = c2.getPosition().getX();
                    int section1 = (int) (c1x / xSpace);
                    int section2 = (int) (c2x / xSpace);

                    // Berechne Abstand zur Mitte für beide Zellen
                    double section1Mid = xSpace * section1 + xSpace / 2.0D;
                    double section2Mid = xSpace * section2 + xSpace / 2.0D;
                    double distToMid1 = Math.abs(c1x - section1Mid);
                    double distToMid2 = Math.abs(c2x - section2Mid);

                    // Normalisiere die Werte
                    double maxPossibleDistance = xSpace / 2.0D; // Maximaler Abstand zur Mitte eines Abschnitts
                    double normalizedDistToMid1Score = (1.0D - (distToMid1 / maxPossibleDistance)); // 0 = perfekt zentriert, 1 = am Rand
                    double normalizedDistToMid2Score = (1.0D - (distToMid2 / maxPossibleDistance));

                    // Y-Position (höher ist besser)
                    // Statt auf gesamte Höhe zu normalisieren, verwenden wir die tatsächlichen min/max Werte
                    // und stellen sicher, dass wir Division durch Null vermeiden
                    double normalizedY1Score = heightRange > 0.0D ?
                            1.0D - ((maxHeight - c1.getPosition().getY()) / heightRange) :
                            0.5D; // Defaultwert bei keinem Unterschied
                    double normalizedY2Score = heightRange > 0.0D ?
                            1.0D - ((maxHeight - c2.getPosition().getY()) / heightRange) :
                            0.5D;

                    // Berechne Gesamtbewertung:
                    // - Hohe Y-Position gibt einen hohen Wert
                    // - Kleine Distanz zur Mitte gibt einen hohen Wert
                    double score1 = (POSITION_WEIGHT * normalizedY1Score) + (DISTANCE_WEIGHT * (normalizedDistToMid1Score));
                    double score2 = (POSITION_WEIGHT * normalizedY2Score) + (DISTANCE_WEIGHT * (normalizedDistToMid2Score));

                    return Double.compare(score1, score2);
                }).
                toList();

        // Wähle die besten Zellen aus jedem Abschnitt aus
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

        List<Cell> nextGen = new ArrayList<>();

        nextGen.addAll(winners);

        // nextGen auffüllen, wenn zu klein:
        while (nextGen.size() < SeperatorCount) {
            final Cell parentCell = winners.get(random.nextInt(winners.size()));
            parentCell.setEnergy(SimulationConfig.CELL_MAX_ENERGY);
            final Cell childCell = ReproductionManagerService.reproduce(config, environment, parentCell);
            if (Objects.nonNull(childCell)) {
                nextGen.add(childCell);
                childCell.setEnergy(SimulationConfig.CELL_MAX_ENERGY);
            }
        }

        // nextGen initalisieren.
        for (int posX = 0; posX < nextGen.size(); posX++) {
            final Cell cell = nextGen.get(posX);
            final double x = xSpace * posX + xSpace / 2.0D;
            final double y = config.getHeight() - Environment.GroundBlockerHeight - config.getCellMaxRadiusSize();
            cell.setType(new CellType(
                    random.nextDouble(),
                    random.nextDouble(),
                    random.nextDouble()
            ));
            cell.setCellState(0);
            cell.setAge(0.0D);
            cell.setEnergy(SimulationConfig.CELL_MAX_ENERGY);
            cell.setPosition(new Vector2D(x, y));
        }
        environment.resetCells(nextGen);
    }
}
