package de.lifecircles.service.trainStrategy;

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
    public Environment initializeEnvironment() {
        config.setWidth(1600 * 3);
        config.setHeight(1200);

        config.setScaleSimulation(4.7D);

        return new Environment(config.getWidth(), config.getHeight());
    }

    @Override
    public void initialize(Environment environment) {
        // Add ground blocker by default
        environment.addGroundBlocker();
        environment.addSunBlocker();
        environment.addWallBlocker(1200.0D, 600.0D, 100.0D);
        environment.addWallBlocker(1200.0D + 150.0D * 1, 600.0D, 100.0D);
        environment.addWallBlocker(1200.0D + 150.0D * 2, 600.0D, 100.0D);
        environment.addWallBlocker(1200.0D + 150.0D * 3, 600.0D, 100.0D);

        Random random = new Random();
        for (int i = 0; i < config.getInitialCellCount(); i++) {
            double x = random.nextDouble() * config.getWidth();
            double y = random.nextDouble() * config.getHeight();
            environment.addCell(new Cell(new Vector2D(x, y), config.getCellMaxRadiusSize() / 2.0,
                    SimulationConfig.brainSynapseConnectivityDefault));
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        // No-op
    }
}
