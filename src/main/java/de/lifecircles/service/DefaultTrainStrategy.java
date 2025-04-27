package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.SimulationConfig;

import java.util.Random;

/**
 * Default training strategy: no special behavior (random initialization, no selection).
 */
public class DefaultTrainStrategy implements TrainStrategy {
    private final SimulationConfig config = SimulationConfig.getInstance();

    @Override
    public void initialize(Environment environment) {
        Random random = new Random();
        for (int i = 0; i < config.getInitialCellCount(); i++) {
            double x = random.nextDouble() * config.getWidth();
            double y = random.nextDouble() * config.getHeight();
            environment.addCell(new Cell(new Vector2D(x, y), config.getCellMaxRadius() / 2.0));
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        // No-op
    }
}
