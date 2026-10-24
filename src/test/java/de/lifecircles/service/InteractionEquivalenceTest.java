package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import javafx.scene.paint.Color;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The optimized sensor/actor interactions (blockers by x, per-actor culling of out-of-range neighbours, affinity
 * only in range, pair offsets from centre offsets plus relative actor positions) give the former algorithm's forces
 * up to rounding and its sensed targets.
 */
class InteractionEquivalenceTest {
    static final CellType[] TYPES = {new CellType(1, 0, 0), new CellType(0, 1, 0), new CellType(.5, .5, .5),
            new CellType(1, 1, 0), new CellType(.9, .1, 0)};

    /** Dense random world with terrain at the seams; small worlds make neighbours meet across both seams. */
    static Environment world(long seed, double width, double height, int count) {
        Random random = new Random(seed);
        Environment world = new Environment(width, height);
        world.addGroundBlocker();
        for (int i = 0; i < 12; i++) world.addBlocker(new Blocker(new Vector2D(random.nextDouble() * width, random.nextDouble() * height),
                5 + random.nextDouble() * 120, 5 + random.nextDouble() * 80, Color.GRAY,
                i % 2 == 0 ? Blocker.BlockerType.WALL : Blocker.BlockerType.PLATFORM));
        world.addBlocker(new Blocker(new Vector2D(0, height / 3), 30, 60, Color.GRAY, Blocker.BlockerType.WALL)); // on the x seam
        for (int i = 0; i < count; i++) {
            Cell cell = new Cell(new Vector2D(random.nextDouble() * width * 1.2 - width * .1, random.nextDouble() * height),
                    4 + random.nextDouble() * 16, new PhysicsRegressionTest.ConstantBrain());
            cell.setType(TYPES[random.nextInt(TYPES.length)]);
            cell.setRotation(random.nextDouble() * 7);
            for (SensorActor actor : cell.getSensorActors()) {
                actor.setType(TYPES[random.nextInt(TYPES.length)]);
                actor.setForceStrength(random.nextInt(4) == 0 ? 0 : random.nextDouble() * 200 - 130);
                actor.settleForce(1);
            }
            world.addCell(cell);
        }
        for (Cell cell : world.getCellList()) for (SensorActor actor : cell.getSensorActors()) actor.updateCachedPosition();
        // Some bonds (also between far actors) and anchors.
        List<Cell> cells = world.getCellList();
        for (int i = 0; i < count / 6; i++) {
            SensorActor a = cells.get(random.nextInt(count)).getSensorActors().get(random.nextInt(12));
            SensorActor b = cells.get(random.nextInt(count)).getSensorActors().get(random.nextInt(12));
            if (a != b && a.getBondPartner() == null && b.getBondPartner() == null && a.getAnchor() == null && b.getAnchor() == null)
                SensorActor.bond(a, b, -20 - random.nextDouble() * 50);
            SensorActor c = cells.get(random.nextInt(count)).getSensorActors().get(random.nextInt(12));
            if (c.getBondPartner() == null && c.getAnchor() == null)
                c.anchor(c.getCachedPosition().add(new Vector2D(random.nextDouble() * 10 - 5, random.nextDouble() * 10 - 5)), -40);
        }
        return world;
    }

    static void same(Environment world, SensorActorForceCellCalcService.Affinity affinity, SensorActorForceCellCalcService.Ground ground,
                     String where) {
        SimulationConfig config = SimulationConfig.getInstance();
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(),
                (config.getCellMaxRadiusSize() + config.getCellActorMaxFieldRadius()) * 2);
        List<Cell> cells = world.getCellList();
        grid.build(cells);
        double[][] before = InteractionBenchmarkTest.state(cells);
        SensorActorForceReference.processInteractions(cells, grid, world, affinity, ground);
        var expected = InteractionBenchmarkTest.observe(cells);
        InteractionBenchmarkTest.restore(cells, before);
        SensorActorForceCellCalcService.processInteractions(cells, grid, world, affinity, ground);
        var actual = InteractionBenchmarkTest.observe(cells);
        InteractionBenchmarkTest.restore(cells, before);
        InteractionBenchmarkTest.close(expected, actual, where);
    }

    @Test void sameForAllSwitchesSeamsBondsAndAnchors() {
        var affinity = new SensorActorForceCellCalcService.Affinity(2, .5);
        var grounds = new SensorActorForceCellCalcService.Ground[]{null, new SensorActorForceCellCalcService.Ground(0, 2),
                new SensorActorForceCellCalcService.Ground(-30, 0)};
        double[][] sizes = {{1600, 600}, {400, 300}, {150, 120}};
        for (long seed = 1; seed <= 6; seed++) for (double[] size : sizes) {
            Environment world = world(seed, size[0], size[1], size[0] > 1000 ? 260 : 70);
            for (var ground : grounds) {
                same(world, null, ground, "seed " + seed + " " + size[0] + " colour-blind ground " + ground);
                same(world, affinity, ground, "seed " + seed + " " + size[0] + " recognition ground " + ground);
            }
        }
    }

    /** Each side computes the pair from exactly negated offsets: action and reaction cancel up to summation order, also at seams. */
    @Test void actionEqualsReactionAcrossTheSeam() {
        Random random = new Random(5);
        var affinity = new SensorActorForceCellCalcService.Affinity(2, .5);
        for (int trial = 0; trial < 300; trial++) {
            double w = 500, h = 400, x = random.nextBoolean() ? 3 : 250;
            Environment world = new Environment(w, h);
            Cell a = new Cell(new Vector2D(x, 200), 6 + random.nextDouble() * 14, new PhysicsRegressionTest.ConstantBrain());
            Cell b = new Cell(new Vector2D(x - 8 - random.nextDouble() * 25, 200 + random.nextDouble() * 20 - 10), 6 + random.nextDouble() * 14,
                    new PhysicsRegressionTest.ConstantBrain());
            for (Cell c : List.of(a, b)) {
                c.setType(TYPES[random.nextInt(TYPES.length)]); c.setRotation(random.nextDouble() * 7);
                for (SensorActor actor : c.getSensorActors()) {
                    actor.setType(TYPES[random.nextInt(TYPES.length)]); actor.setForceStrength(random.nextDouble() * 200 - 130); actor.settleForce(1);
                }
                world.addCell(c);
            }
            var grid = new SpatialGridPartitioningStrategy(w, h, 80);
            grid.build(world.getCellList());
            SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid, world, trial % 2 == 0 ? affinity : null, null);
            a.updateForce(1.0 / 60); b.updateForce(1.0 / 60);
            // Each cell sums its pair forces in its own actor order, so only rounding may differ.
            double tolerance = 1e-12 * (1 + Math.hypot(a.getVelocity().getX(), a.getVelocity().getY()));
            assertEquals(-a.getVelocity().getX(), b.getVelocity().getX(), tolerance, "trial " + trial);
            assertEquals(-a.getVelocity().getY(), b.getVelocity().getY(), tolerance, "trial " + trial);
        }
    }
}
