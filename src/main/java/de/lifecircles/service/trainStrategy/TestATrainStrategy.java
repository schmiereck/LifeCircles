package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.Blocker;
import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.service.MechanicsPreset;

import static de.lifecircles.service.trainStrategy.TestTrainStrategyUtils.createAndAddCell;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests Sensor-Actor Force interactions with hard-wired brains.
 * <p>
 * Spring fields (FEDERFELD.md) are on with threshold 100 %: only full attraction (FORCE = 0) is a spring, the
 * original figures on the left attract with {@link #CLASSIC} = 99 % and keep the classic field. Right of them
 * stand twins: each figure once classic (left) and once as spring (right), under gravity on the ground.
 */
public class TestATrainStrategy implements TrainStrategy {
    /** Attraction of the classic figures, just below the spring threshold. */
    public static final double CLASSIC = 0.99D;
    /** Full attraction: a spring with threshold 1.0. */
    public static final double SPRING = 1.0D;
    /** Twin figures (cantilever, bridge, tower, falling beam), positions in cell radii from the left. */
    public static final double[][] TWIN_X = { { 70, 88 }, { 106, 138 }, { 172, 180 }, { 188, 214 } };
    public static final String[] TWIN_NAMES = { "Kragarm", "Brücke", "Turm", "Fallbalken" };
    public static final String[][] TWIN_SHAPES = {
            { "#########",
              "#########",
              "##",
              "##",
              "##",
              "##" },
            { "##############",
              "##          ##",
              "##          ##",
              "##          ##" },
            { "##", "##", "##", "##", "##", "##", "##", "##", "##", "##", "##", "##", "##", "##", "##", "##" },
            { "###########", "###########" } };
    /** Lowest row of the falling beam above the ground, in cell radii. */
    public static final double DROP_HEIGHT = 30.0D;
    /** Height of the pedestal the falling beam hits, in cell radii. */
    public static final double PEDESTAL_HEIGHT = 8.0D;

    private final SimulationConfig config = SimulationConfig.getInstance();

    @Override
    public Environment initializeEnvironment() {
        MechanicsPreset.BALANCED.apply(config);
        this.config.setWidth(4800);
        this.config.setHeight(800);

        this.config.setScaleSimulation(4.8D);

        config.setEnergyPerRay(0.01D); // 0.005; //0.015; // 0.025;

        this.config.setInitialCellCount(1);

        this.config.setSpringAdhesion(true);
        this.config.setSpringAdhesionThreshold(SPRING);

        return new Environment(config.getWidth(), config.getHeight());
    }

    /**
     * Hexagonally packed figure: rows top to bottom, '#' places a cell. The bottom row rests at yBottom,
     * every second row is shifted by one radius.
     */
    public static List<Cell> addFigure(Environment environment, double x0, double yBottom, double radius,
                                       double level, String... rows) {
        final List<Cell> cells = new ArrayList<>();
        for (int k = 0; k < rows.length; k++) {
            final String row = rows[rows.length - 1 - k];
            final double shift = (k % 2 == 1) ? radius : 0.0D;
            for (int c = 0; c < row.length(); c++) {
                if (row.charAt(c) == '#') {
                    cells.add(createAndAddCell(environment, x0 + c * 2.0D * radius + shift,
                            yBottom - k * Math.sqrt(3.0D) * radius, radius, level));
                }
            }
        }
        return cells;
    }

    /** Twin figure i as classic (side 0) or spring (side 1). */
    public static List<Cell> addTwin(Environment environment, int figure, int side) {
        final double radius = SimulationConfig.getInstance().getCellMaxRadiusSize();
        final double ground = environment.getHeight() - Environment.GroundBlockerHeight - radius;
        final double yBottom = TWIN_NAMES[figure].equals("Fallbalken") ? ground - DROP_HEIGHT * radius : ground;
        if (TWIN_NAMES[figure].equals("Fallbalken")) {
            // Narrow pedestal under the beam's middle: the impact bends both overhanging halves.
            final double pedestalHeight = PEDESTAL_HEIGHT * radius;
            environment.getBlockerList().add(new Blocker(
                    new Vector2D((TWIN_X[figure][side] + TWIN_SHAPES[figure][0].length() - 0.5D) * radius,
                            environment.getHeight() - Environment.GroundBlockerHeight - pedestalHeight / 2.0D),
                    2.0D * radius, pedestalHeight, javafx.scene.paint.Color.GRAY, Blocker.BlockerType.PLATFORM));
        }
        return addFigure(environment, TWIN_X[figure][side] * radius, yBottom, radius,
                side == 0 ? CLASSIC : SPRING, TWIN_SHAPES[figure]);
    }

    @Override
    public void initialize(Environment environment) {
        // Add ground blocker by default
        environment.addGroundBlocker();

        final double radiusSize = this.config.getCellMaxRadiusSize();

        final double yTopTop = environment.getHeight() - Environment.GroundBlockerHeight;
        final double yTop = yTopTop - 80.0D;

        // Anziehung:
        {
            final double x = radiusSize * 8.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y - radiusSize * 8.0D, radiusSize, CLASSIC);

            createAndAddCell(environment, x - radiusSize * 1.25D, y - radiusSize * 6.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 6.0D, radiusSize, CLASSIC);

            createAndAddCell(environment, x - radiusSize * 2.4D, y - radiusSize * 4.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x, y - radiusSize * 4.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 2.4D, y - radiusSize * 4.0D, radiusSize, CLASSIC);

            createAndAddCell(environment, x - radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, CLASSIC);

            createAndAddCell(environment, x - radiusSize * 2.4D, y, radiusSize, CLASSIC);
            createAndAddCell(environment, x, y, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 2.4D, y, radiusSize, CLASSIC);

            createAndAddCell(environment, x - radiusSize * 3.5D, y + radiusSize * 2.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x - radiusSize * 1.25D, y + radiusSize * 2.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 1.25D, y + radiusSize * 2.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 3.5D, y + radiusSize * 2.0D, radiusSize, CLASSIC);

            // Drop from Top:
            createAndAddCell(environment, x + radiusSize * 3.5D, y - radiusSize * 16.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x - radiusSize * 3.0D, y - radiusSize * 16.0D, radiusSize, CLASSIC);

            createAndAddCell(environment, x + radiusSize * 4.5D, y - radiusSize * 19.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x - radiusSize * 4.0D, y - radiusSize * 19.0D, radiusSize, CLASSIC);

            createAndAddCell(environment, x + radiusSize * 5.5D, y - radiusSize * 22.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x - radiusSize * 5.0D, y - radiusSize * 22.0D, radiusSize, CLASSIC);
        }
        {
            final double x = radiusSize * 20.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y - radiusSize * 4.0D, radiusSize, CLASSIC);

            createAndAddCell(environment, x - radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, CLASSIC);

            createAndAddCell(environment, x - radiusSize * 2.4D, y, radiusSize, CLASSIC);
            createAndAddCell(environment, x, y, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 2.4D, y, radiusSize, CLASSIC);

            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = radiusSize * 28.0D;
            final double y = yTop;

            createAndAddCell(environment, x, y, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 2.5D, y, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 2.0D, radiusSize, CLASSIC);
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

            createAndAddCell(environment, x, y, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 2.25D, y, radiusSize, CLASSIC);
            createAndAddCell(environment, x + radiusSize * 1.125D, y - radiusSize * 2.0D, radiusSize, false);
            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }

        // Pusher:
        {
            final double x = radiusSize * 29.25D;
            final double y = 300.0;

            createAndAddCell(environment, x, y, radiusSize, CLASSIC);
            createAndAddCell(environment, x, y + radiusSize * 2.1D, radiusSize, false, false, false, false);
            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        // Born to be Wild:
        {
            final double x = radiusSize * 62.0D;
            final double y = yTopTop;

            createAndAddCell(environment, x, y - radiusSize * 2.0D, radiusSize, false, false, false, false);
            createAndAddCell(environment, x, y - radiusSize * 2.75D, radiusSize, false, false, false, false);
            //createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }

        // Federzwillinge: links klassisch (99 %), rechts Feder (100 %). Only in the full-width world: the
        // mechanics benchmarks reuse the snowman in a 1400 px world, where the twins would wrap onto it.
        if (environment.getWidth() >= 4800) for (int figure = 0; figure < TWIN_NAMES.length; figure++) {
            addTwin(environment, figure, 0);
            addTwin(environment, figure, 1);
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        // No-op
    }
}
