package de.lifecircles.service.trainStrategy;


import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;

import static de.lifecircles.service.trainStrategy.TestTrainStrategyUtils.createAndAddCell;

/**
 * Tests Blocker interactions.
 */
public class TestCTrainStrategy implements TrainStrategy {
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

        {
            final double yTop = this.config.getHeight() - (Environment.GroundBlockerHeight + 8.0D);
            createAndAddCell(environment, 40.0D, yTop, radiusSize, false, false);
        }
        {
            final double yTop = this.config.getHeight() - (Environment.GroundBlockerHeight + 0.0D);
            createAndAddCell(environment, 100.0D, yTop, radiusSize, false, false);
        }
        {
            final double yTop = this.config.getHeight() - (Environment.GroundBlockerHeight - 8.0D);
            createAndAddCell(environment, 160.0D, yTop, radiusSize, false, false);
        }
        {
            final double yTop = this.config.getHeight() - (Environment.GroundBlockerHeight / 2.0D);
            createAndAddCell(environment, 220.0D, yTop, radiusSize, false, false);
        }
        {
            final double yTop = this.config.getHeight() - 13.0;
            createAndAddCell(environment, 280.0D, yTop, radiusSize, false, false);
        }
        {
            final double yTop = this.config.getHeight() - 8.0;
            createAndAddCell(environment, 340.0D, yTop, radiusSize, false, false);
        }

        final double xStart = 450.0D;
        final double yStart = 600.0D;
        final double xStep = 100.0D;
        final double wallWidth = 50.0D;
        {
            final double x = xStart + xStep * 0;
            environment.addWallBlocker(x, 600.0D, Environment.GroundBlockerHeight, wallWidth);
            createAndAddCell(environment, x - 20, yStart, radiusSize, false, false);
        }
        {
            final double x = xStart + xStep * 1;
            environment.addWallBlocker(x, 600.0D, Environment.GroundBlockerHeight, wallWidth);
            createAndAddCell(environment, x - 15, yStart, radiusSize, false, false);
        }
        {
            final double x = xStart + xStep * 2;
            environment.addWallBlocker(x, 600.0D, Environment.GroundBlockerHeight, wallWidth);
            createAndAddCell(environment, x, yStart, radiusSize, false, false);
        }
        {
            final double x = xStart + xStep * 3;
            environment.addWallBlocker(x, 600.0D, Environment.GroundBlockerHeight, wallWidth);
            createAndAddCell(environment, x + 5, yStart, radiusSize, false, false);
        }
        {
            final double x = xStart + xStep * 4;
            environment.addWallBlocker(x, 600.0D, Environment.GroundBlockerHeight, wallWidth);
            createAndAddCell(environment, x + 15, yStart, radiusSize, false, false);
        }
        {
            final double x = xStart + xStep * 5;
            environment.addWallBlocker(x, 600.0D, Environment.GroundBlockerHeight, wallWidth);
            createAndAddCell(environment, x + wallWidth / 2.0D - 2.0D, 300.0D, radiusSize, false, false);
            createAndAddCell(environment, x + wallWidth / 2.0D, 400.0D, radiusSize, false, false);
            createAndAddCell(environment, x + wallWidth / 2.0D + 2.0D, 500.0D, radiusSize, false, false);
        }
        {
            final double x = xStart + xStep * 6;
            environment.addWallBlocker(x, 600.0D, Environment.GroundBlockerHeight, wallWidth);
            createAndAddCell(environment, x + (wallWidth - 5.0D), yStart, radiusSize, false, false);
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        // No-op
    }
}
