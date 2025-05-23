package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;

import static de.lifecircles.service.trainStrategy.TestTrainStrategyUtils.createAndAddCell;

/**
 * Tests Energie-Transfer interactions.
 */
public class TestBTrainStrategy implements TrainStrategy {
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

        final double yTop = this.config.getHeight() - 75.0D;

        // Delivery:
        {
            final double x = radiusSize * 8.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y - radiusSize * 4.0D, radiusSize, true, true, false);

            createAndAddCell(environment, x - radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, true, true, false);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, true, true, false);

            createAndAddCell(environment, x - radiusSize * 2.4D, y, radiusSize, true, true, false);
            createAndAddCell(environment, x + radiusSize * 0.0D, y, radiusSize, true, true, false);
            createAndAddCell(environment, x + radiusSize * 2.4D, y, radiusSize, true, true, false);

            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = radiusSize * 14.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y, radiusSize, true, true);
            createAndAddCell(environment, x + radiusSize * 2.5D, y, radiusSize, true, true);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, true, true);
            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = radiusSize * 22.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y, radiusSize, true, true);
            createAndAddCell(environment, x + radiusSize * 2.25D, y, radiusSize, true, true);
            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        // Absorbtion:
        {
            final double x = radiusSize * 32.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y - radiusSize * 4.0D, radiusSize, true, false, true);

            createAndAddCell(environment, x - radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, true, false, true);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, true, false, true);

            createAndAddCell(environment, x - radiusSize * 2.4D, y, radiusSize, true, false, true);
            createAndAddCell(environment, x + radiusSize * 0.0D, y, radiusSize, true, false, true);
            createAndAddCell(environment, x + radiusSize * 2.4D, y, radiusSize, true, false, true);

            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        // No-op
    }
}
