package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.CellFactory;
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
        //config.setWidth(1600 * 3);
        config.setWidth(1600 * 4);
        config.setHeight(1200);

        //config.setScaleSimulation(5.7D);
        config.setScaleSimulation(6.5D);

        //config.setEnergyPerRay(0.02D); // 0.005; //0.015; // 0.025;

        return new Environment(config.getWidth(), config.getHeight());
    }

    @Override
    public void initialize(Environment environment) {
        // Add ground blocker by default
        environment.addGroundBlocker();
        environment.addSunBlocker(1600 * 3 / 4, (int)(environment.getHeight() - (environment.getHeight() / 8)), 1600 * 3 / 6);

        this.addGroundSeperators(environment, 50, 600, 6);

        environment.addWallBlocker(1200.0D + 150.0D * 0, 600.0D, 165.0D);
        environment.addWallBlocker(1200.0D + 150.0D * 1, 600.0D, 165.0D);
        environment.addWallBlocker(1200.0D + 150.0D * 2, 600.0D, 165.0D);
        environment.addWallBlocker(1200.0D + 150.0D * 3, 600.0D, 165.0D);
        environment.addWallBlocker(1200.0D + 150.0D * 4, 600.0D, 165.0D);
        environment.addWallBlocker(1200.0D + 150.0D * 5, 600.0D, 165.0D);
        environment.addWallBlocker(1200.0D + 150.0D * 6, 600.0D, 165.0D);

        environment.addWallBlocker(1200.0D + 150.0D * 10, 100.0D, Environment.GroundBlockerHeight);
        environment.addWallBlocker(1200.0D + 150.0D * 13, 200.0D, Environment.GroundBlockerHeight);
        environment.addWallBlocker(1200.0D + 150.0D * 16, 100.0D, Environment.GroundBlockerHeight);
        environment.addWallBlocker(1200.0D + 150.0D * 19, 300.0D, Environment.GroundBlockerHeight);
        environment.addWallBlocker(1200.0D + 150.0D * 22, 100.0D, Environment.GroundBlockerHeight);

        final int xSunBlocker = 1200 + 150 * 25;

        for (int xPos = 0; xPos < 1200; xPos += 50) {
            environment.addSunBlocker(xSunBlocker + xPos, 50, 20);
        }

        this.addGroundSeperators(environment, 1200.0D + 150.0D * 24, 1500, 10);

        environment.addSunBlocker(1600 * 3 / 4, (int)(environment.getHeight() - (environment.getHeight() / 8)), 1600 * 3 / 6);

        Random random = new Random();
        for (int i = 0; i < config.getInitialCellCount(); i++) {
            final double x = random.nextDouble() * config.getWidth();
            final double y = (random.nextDouble() * (config.getHeight() - 80.0D)) + 40.0D;
            final double hiddenCountFactor = SimulationConfig.hiddenCountFactorDefault;
            //final double hiddenCountFactor = 0.5D;
            final double stateHiddenLayerSynapseConnectivity = SimulationConfig.stateHiddenLayerSynapseConnectivityDefault;
            //final double stateHiddenLayerSynapseConnectivity = 0.001D;
            final double hiddenLayerSynapseConnectivity = SimulationConfig.hiddenLayerSynapseConnectivityDefault;
            //final double hiddenLayerSynapseConnectivity = 0.002D;
            environment.addCell(CellFactory.createCell(new Vector2D(x, y), config.getCellMaxRadiusSize() / 2.0,
                    hiddenCountFactor,
                    stateHiddenLayerSynapseConnectivity, hiddenLayerSynapseConnectivity));
        }
    }

    private void addGroundSeperators(Environment environment, double xStart, double xWidth, final int seperatorCount) {
        for (int posX = 0; posX <= seperatorCount; posX++) {
            final double x = (xWidth / seperatorCount) * posX + xStart;
            final double yTop = Environment.GroundBlockerHeight + (config.getCellMaxRadiusSize() * 4.0D);
            final double yBottom = Environment.GroundBlockerHeight;
            environment.addWallBlocker(x, yTop, yBottom);
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        // No-op
    }
}
