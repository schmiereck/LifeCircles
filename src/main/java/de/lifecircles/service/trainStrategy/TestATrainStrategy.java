package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;

import static de.lifecircles.service.trainStrategy.TestTrainStrategyUtils.createAndAddCell;

/**
 * Tests Sensor-Actor Force interactions.
 */
public class TestATrainStrategy implements TrainStrategy {
    private final SimulationConfig config = SimulationConfig.getInstance();

    @Override
    public Environment initializeEnvironment() {
        //config.setWidth(1600 * 3);
        this.config.setWidth(1200);
        this.config.setHeight(800);

        //config.setScaleSimulation(5.7D);
        this.config.setScaleSimulation(1.2D);

        config.setEnergyPerRay(0.01D); // 0.005; //0.015; // 0.025;

        this.config.setInitialCellCount(1);

        return new Environment(config.getWidth(), config.getHeight());
    }

    @Override
    public void initialize(Environment environment) {
        // Add ground blocker by default
        environment.addGroundBlocker();

        final double radiusSize = this.config.getCellMaxRadiusSize();

        final double yTop = this.config.getHeight() - 100.0D;

        // Anziehung:
        {
            final double x = radiusSize * 8.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y - radiusSize * 8.0D, radiusSize, true);

            createAndAddCell(environment, x - radiusSize * 1.25D, y - radiusSize * 6.0D, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 6.0D, radiusSize, true);

            createAndAddCell(environment, x - radiusSize * 2.4D, y - radiusSize * 4.0D, radiusSize, true);
            createAndAddCell(environment, x, y - radiusSize * 4.0D, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 2.4D, y - radiusSize * 4.0D, radiusSize, true);

            createAndAddCell(environment, x - radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, true);

            createAndAddCell(environment, x - radiusSize * 2.4D, y, radiusSize, true);
            createAndAddCell(environment, x, y, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 2.4D, y, radiusSize, true);

            createAndAddCell(environment, x - radiusSize * 3.5D, y + radiusSize * 2.0D, radiusSize, true);
            createAndAddCell(environment, x - radiusSize * 1.25D, y + radiusSize * 2.0D, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 1.25D, y + radiusSize * 2.0D, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 3.5D, y + radiusSize * 2.0D, radiusSize, true);
        }
        {
            final double x = radiusSize * 20.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y - radiusSize * 4.0D, radiusSize, true);

            createAndAddCell(environment, x - radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, true);

            createAndAddCell(environment, x - radiusSize * 2.4D, y, radiusSize, true);
            createAndAddCell(environment, x, y, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 2.4D, y, radiusSize, true);

            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = radiusSize * 28.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 2.5D, y, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, true);
            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        // Abstoßung:
        {
            final double x = radiusSize * 36.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y, radiusSize, false);
            createAndAddCell(environment, x + radiusSize * 2.25D, y, radiusSize, false);
            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = radiusSize * 44.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y, radiusSize, false);
            createAndAddCell(environment, x + radiusSize * 2.25D, y, radiusSize, false);
            createAndAddCell(environment, x + radiusSize * 1.125D, y - radiusSize * 2.0D, radiusSize, false);
            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }

        // Anziehung & Abstoßung:
        {
            final double x = radiusSize * 52.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 2.25D, y, radiusSize, true);
            createAndAddCell(environment, x + radiusSize * 1.125D, y - radiusSize * 2.0D, radiusSize, false);
            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }

        // Pusher:
        {
            final double x = radiusSize * 26.0D;
            final double y = 300.0;

            createAndAddCell(environment, x, y, radiusSize, true, false, false, false);
            createAndAddCell(environment, x, y + radiusSize * 2.1D, radiusSize, false, false, false, false);
            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        // No-op
    }
}
