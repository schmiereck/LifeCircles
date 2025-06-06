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
    private static final int GENERATION_STEP = 2500 * 4;
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
        // Sortiere global: erst nach Y absteigend, dann nach Abstand zur Abschnittsmitte aufsteigend
        cells.sort(Comparator.comparingDouble((Cell c) -> -c.getPosition().getY())
                .thenComparingDouble(c -> {
                    double cx = c.getPosition().getX();
                    int section = (int) (cx / xSpace);
                    double sectionMid = xSpace * section + xSpace / 2.0D;
                    return Math.abs(cx - sectionMid);
                }));
        int winnersCount = Math.min(SeperatorCount, cells.size());
        List<Cell> winners = new ArrayList<>(cells.subList(0, winnersCount));
        winners.forEach(cell -> cell.setEnergy(SimulationConfig.CELL_MAX_ENERGY));

        List<Cell> nextGen = new ArrayList<>();
        nextGen.addAll(winners);
        while (nextGen.size() < SeperatorCount) {
            final Cell parent = winners.get(random.nextInt(winnersCount));
            final Cell childCell = ReproductionManagerService.reproduce(config, environment, parent);
            if (Objects.nonNull(childCell)) {
                nextGen.add(childCell);
                childCell.setEnergy(SimulationConfig.CELL_MAX_ENERGY);
            }
        }
        for (int posX = 0; posX < SeperatorCount; posX++) {
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
