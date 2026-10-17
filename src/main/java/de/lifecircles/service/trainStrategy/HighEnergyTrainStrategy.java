package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.service.SimulationConfig;

import java.util.List;
import java.util.Random;

/**
 * Training-Strategie: HighEnergy.
 * Startet mit 20 Zellen, selektiert alle 2500 Schritte die Top-20% nach Energie,
 * erzeugt mutierte Nachkommen und ersetzt die Population.
 */
public class HighEnergyTrainStrategy implements TrainStrategy {
    private static final int INITIAL_COUNT = 20;
    private static final int GENERATION_STEP = 2500 * 1;
    private static final double SELECTION_PERCENT = 0.2;
    private final SimulationConfig config = SimulationConfig.getInstance();
    private final Random random = new Random();
    private long stepCounter = 0;

    @Override
    public Environment initializeEnvironment() {
        return new Environment(config.getWidth(), config.getHeight());
    }

    @Override
    public void initialize(Environment environment) {
        this.stepCounter = 0;
        // Add ground blocker by default
        environment.addGroundBlocker();
        final int sunBlockerXPos = (1024 * 2 / 4);
        final int sunBlockerXWidth = (1024 * 2 / 6);
        environment.addSunBlocker(sunBlockerXPos, (int)(environment.getHeight() - (environment.getHeight() / 8)), sunBlockerXWidth);

        final double xStart =  ((sunBlockerXPos + sunBlockerXWidth)) + 50;
        final double xWidth =  config.getWidth() - (xStart + 150);
        final int seperatorCount = 8;
        for (int posX = 0; posX <= seperatorCount; posX++) {
            final double x = (xWidth / seperatorCount) * posX + xStart;
            final double yTop = Environment.GroundBlockerHeight + (config.getCellMaxRadiusSize() * 3.0D);
            final double yBottom = Environment.GroundBlockerHeight;
            environment.addWallBlocker(x, yTop, yBottom);
        }

        this.config.setEnergyPerRay(0.02D); // 0.005; //0.015; // 0.025;

        // Initiale Population
        for (int i = 0; i < INITIAL_COUNT; i++) {
            double x = random.nextDouble() * this.config.getWidth();
            double y = random.nextDouble() * this.config.getHeight();
            environment.addCell(CellFactory.createCell(new Vector2D(x, y), this.config.getCellMaxRadiusSize() / 2.0));
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        this.stepCounter++;
        if (this.stepCounter % GENERATION_STEP != 0) {
            return;
        }
        List<Cell> cells = environment.getCellList().stream().filter(cell -> !cell.isRemains()).toList();
        if (cells.isEmpty()) {
            return;
        }
        int winnersCount = Math.min(INITIAL_COUNT, Math.max(1, (int) (cells.size() * SELECTION_PERCENT)));
        List<Cell> winners = cells.stream().sorted((a, b) -> Double.compare(b.getEnergy(), a.getEnergy()))
                .limit(winnersCount).toList();
        TrainingPopulationBuilder.build(environment, winners, INITIAL_COUNT, false, random)
                .ifPresent(environment::resetCells);
    }
}
