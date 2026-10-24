package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import javafx.scene.paint.Color;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The optimized sensor/actor interactions (blockers by x, per-actor culling of out-of-range neighbours, affinity
 * only in range) give the former algorithm's forces and sensed targets bit for bit.
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
        List<Object> expected = InteractionBenchmarkTest.observe(cells);
        InteractionBenchmarkTest.restore(cells, before);
        SensorActorForceCellCalcService.processInteractions(cells, grid, world, affinity, ground);
        List<Object> actual = InteractionBenchmarkTest.observe(cells);
        InteractionBenchmarkTest.restore(cells, before);
        assertEquals(expected.size(), actual.size(), where);
        for (int i = 0; i < expected.size(); i++) assertEquals(expected.get(i), actual.get(i), where + " observation " + i);
    }

    @Test void identicalForAllSwitchesSeamsBondsAndAnchors() {
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
}
