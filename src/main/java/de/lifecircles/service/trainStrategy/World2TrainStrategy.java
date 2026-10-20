package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.CellFactory;
import de.lifecircles.model.Environment;
import de.lifecircles.model.SimulationStepPolicy;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.SimulationConfig;

import java.util.Random;

/**
 * Open evolution in a world four times as wide as NONE, with all new physics switched on (WELT2.md):
 * spectral zones of fixed width repeating, adhesion with bonds and birth bonds, ground hold and anchors.
 * The NONE niches repeat in four segments, each shifted by one zone, so every niche meets every light form.
 */
public class World2TrainStrategy implements TrainStrategy {
    /** One NONE world; WORLD2 places four of them side by side. */
    public static final int SEGMENT_WIDTH = 1600 * 4;
    public static final int SEGMENTS = 4;
    public static final int WIDTH = SEGMENT_WIDTH * SEGMENTS;
    public static final int HEIGHT = 1200;
    /** Zone width as in NONE (a quarter segment); one cycle of the four zones per segment. */
    public static final double ZONE_WIDTH = SEGMENT_WIDTH / 4.0;

    private final SimulationConfig config = SimulationConfig.getInstance();
    private final Random random;

    public World2TrainStrategy() { this(new Random()); }
    public World2TrainStrategy(Random random) { this.random = random; }

    @Override public SimulationStepPolicy stepPolicy() { return SimulationStepPolicy.OPEN_WORLD; }

    @Override
    public Environment initializeEnvironment() {
        config.setWidth(WIDTH);
        config.setHeight(HEIGHT);
        // Camera shows about one segment like NONE; zoom out for the whole world.
        config.setScaleSimulation(6.5D);
        config.setSunSpectralZones(true);
        config.setSunZoneWidth(ZONE_WIDTH);
        config.setCellAdhesion(true);
        config.setAdhesionBonds(true);
        config.setAdhesionBirthBonds(true);
        config.setGroundAdhesion(true);
        config.setGroundAnchors(true);
        return new Environment(config.getWidth(), config.getHeight());
    }

    @Override
    public void initialize(Environment environment) {
        environment.addGroundBlocker();
        for (int segment = 0; segment < SEGMENTS; segment++) addSegment(environment, segment);
        // As many founders per width as NONE.
        for (int i = 0; i < config.getInitialCellCount() * SEGMENTS; i++) {
            final double x = random.nextDouble() * config.getWidth();
            final double y = (random.nextDouble() * (config.getHeight() - 80.0D)) + 40.0D;
            environment.addCell(CellFactory.createCell(new Vector2D(x, y), config.getCellMaxRadiusSize() / 2.0));
        }
    }

    /** NONE niches, shifted by one zone per segment and wrapped inside the segment. */
    private void addSegment(Environment environment, int segment) {
        final double start = segment * (double) SEGMENT_WIDTH, shift = segment * ZONE_WIDTH;
        final java.util.function.DoubleUnaryOperator at = local -> start + (local + shift) % SEGMENT_WIDTH;

        // Shade roof low over the ground.
        environment.addSunBlocker((int) at.applyAsDouble(1600 * 3 / 4), (int) (HEIGHT - HEIGHT / 8), 1600 * 3 / 6);
        addGroundSeparators(environment, at, 50, 600, 6);
        // Tall walls: narrow shaded gaps.
        for (int i = 0; i <= 6; i++) environment.addWallBlocker(at.applyAsDouble(1200.0D + 150.0D * i), 600.0D, 165.0D);
        // Single walls of different heights.
        environment.addWallBlocker(at.applyAsDouble(1200.0D + 150.0D * 10), 100.0D, Environment.GroundBlockerHeight);
        environment.addWallBlocker(at.applyAsDouble(1200.0D + 150.0D * 13), 200.0D, Environment.GroundBlockerHeight);
        environment.addWallBlocker(at.applyAsDouble(1200.0D + 150.0D * 16), 100.0D, Environment.GroundBlockerHeight);
        environment.addWallBlocker(at.applyAsDouble(1200.0D + 150.0D * 19), 300.0D, Environment.GroundBlockerHeight);
        environment.addWallBlocker(at.applyAsDouble(1200.0D + 150.0D * 22), 100.0D, Environment.GroundBlockerHeight);
        // Row of small platforms high up: patchy shade.
        final int xSunBlocker = 1200 + 150 * 25;
        for (int xPos = 0; xPos < 1200; xPos += 50) environment.addSunBlocker((int) at.applyAsDouble(xSunBlocker + xPos), 50, 20);
        addGroundSeparators(environment, at, 1200.0D + 150.0D * 24, 1500, 10);
    }

    private void addGroundSeparators(Environment environment, java.util.function.DoubleUnaryOperator at,
                                     double xStart, double xWidth, final int separatorCount) {
        for (int posX = 0; posX <= separatorCount; posX++) {
            final double x = (xWidth / separatorCount) * posX + xStart;
            final double yTop = Environment.GroundBlockerHeight + (config.getCellMaxRadiusSize() * 4.0D);
            environment.addWallBlocker(at.applyAsDouble(x), yTop, Environment.GroundBlockerHeight);
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        // Open evolution: no selection.
    }
}
