package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import javafx.scene.paint.Color;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Blocker collisions with the cheap per-axis pretest give the former results bit for bit; the two-phase bond and
 * anchor update stays consistent, deterministic and close to the former sequential one (IntegrationReference).
 */
class IntegrationEquivalenceTest {
    final SimulationConfig config = SimulationConfig.getInstance();
    boolean bonds, ground, anchors, groundOnly;
    @BeforeEach void save() { bonds = config.isAdhesionBonds(); ground = config.isGroundAdhesion(); anchors = config.isGroundAnchors(); groundOnly = config.isGroundAnchorsGroundOnly(); }
    @AfterEach void restore() { config.setAdhesionBonds(bonds); config.setGroundAdhesion(ground); config.setGroundAnchors(anchors); config.setGroundAnchorsGroundOnly(groundOnly); }

    @Test void blockerCollisionsInsideAtSeamsAndOnFullWidthTerrain() {
        Random random = new Random(12);
        for (double[] size : new double[][]{{1600, 600}, {300, 200}, {90, 70}}) {
            double w = size[0], h = size[1];
            Environment world = new Environment(w, h);
            world.addGroundBlocker();
            for (int i = 0; i < 25; i++) world.addBlocker(new Blocker(new Vector2D(random.nextDouble() * w, random.nextDouble() * h),
                    2 + random.nextDouble() * w / 4, 2 + random.nextDouble() * h / 4, Color.GRAY, Blocker.BlockerType.PLATFORM));
            world.addBlocker(new Blocker(new Vector2D(w / 2, h / 3), w, 10, Color.GRAY, Blocker.BlockerType.PLATFORM)); // full width
            world.addBlocker(new Blocker(new Vector2D(0, h / 2), 20, h * 2, Color.GRAY, Blocker.BlockerType.WALL)); // full height at the seam
            for (int i = 0; i < 4000; i++) {
                double r = 1 + random.nextDouble() * 20;
                Vector2D p = new Vector2D(random.nextDouble() * w * 1.4 - w * .2, random.nextDouble() * h * 1.4 - h * .2);
                Vector2D v = new Vector2D(random.nextDouble() * 40 - 20, random.nextDouble() * 40 - 20);
                Cell a = new Cell(p, r, new PhysicsRegressionTest.ConstantBrain()), b = new Cell(p, r, new PhysicsRegressionTest.ConstantBrain());
                a.setVelocity(v); b.setVelocity(v);
                IntegrationReference.handleBlockerCollisions(a, world.getBlockerList(), world);
                BlockerCellCalcService.handleBlockerCollisions(b, world.getBlockerList(), world);
                String where = w + " cell " + i;
                assertEquals(Double.doubleToRawLongBits(a.getPosition().getX()), Double.doubleToRawLongBits(b.getPosition().getX()), where);
                assertEquals(Double.doubleToRawLongBits(a.getPosition().getY()), Double.doubleToRawLongBits(b.getPosition().getY()), where);
                assertEquals(Double.doubleToRawLongBits(a.getVelocity().getX()), Double.doubleToRawLongBits(b.getVelocity().getX()), where);
                assertEquals(Double.doubleToRawLongBits(a.getVelocity().getY()), Double.doubleToRawLongBits(b.getVelocity().getY()), where);
            }
        }
    }

    /** One step of motion and sensing, identical for every world with the same jiggle. */
    void step(Environment world, long jiggle) {
        Random random = new Random(jiggle);
        for (Cell cell : world.getCellList()) {
            cell.setPosition(cell.getPosition().add(new Vector2D(random.nextDouble() * 1.2 - .6, random.nextDouble() * 1.2 - .6)));
            for (SensorActor actor : cell.getSensorActors()) actor.updateCachedPosition();
        }
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), (config.getCellMaxRadiusSize() + config.getCellActorMaxFieldRadius()) * 2);
        grid.build(world.getCellList());
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid, world, SensorActorForceCellCalcService.Affinity.fromConfig(), null);
    }

    /** Links are mutual, nobody holds terrain and a partner at once, partners are listed cells. */
    static void consistent(List<Cell> cells, String where) {
        Set<Cell> listed = Collections.newSetFromMap(new IdentityHashMap<>()); listed.addAll(cells);
        for (Cell cell : cells) for (SensorActor actor : cell.getSensorActors()) {
            SensorActor partner = actor.getBondPartner();
            if (actor.getAnchor() != null) assertNull(partner, where + ": anchored actor with a partner");
            if (partner == null) continue;
            assertSame(actor, partner.getBondPartner(), where + ": one-sided bond");
            assertTrue(listed.contains(partner.getParentCell()), where + ": partner outside the list");
            assertEquals(actor.getBondStrength(), partner.getBondStrength(), where + ": strengths differ");
        }
    }

    static int[] count(List<Cell> cells) {
        int bonds = 0, anchors = 0;
        for (Cell cell : cells) for (SensorActor actor : cell.getSensorActors()) {
            if (actor.getBondPartner() != null) bonds++;
            if (actor.getAnchor() != null) anchors++;
        }
        return new int[]{bonds / 2, anchors};
    }

    /**
     * Two phases (own actors in parallel, pairs in list order): deterministic, consistent, and close to the former
     * sequential update. It differs on purpose where that one depended on list order (an actor released by an
     * earlier cell could start anchoring or forming in the same step) and where an anchored actor took a partner.
     */
    @Test void bondsAndAnchorsStayConsistentDeterministicAndCloseToTheFormerUpdate() {
        config.setAdhesionBonds(true); config.setGroundAdhesion(true); config.setGroundAnchors(true);
        long[] former = new long[2], current = new long[2];
        for (long seed = 1; seed <= 4; seed++) for (boolean groundOnly : new boolean[]{false, true}) {
            config.setGroundAnchorsGroundOnly(groundOnly);
            double w = seed % 2 == 0 ? 400 : 1200, h = 300;
            Environment ref = InteractionEquivalenceTest.world(seed, w, h, 90), now = InteractionEquivalenceTest.world(seed, w, h, 90),
                    again = InteractionEquivalenceTest.world(seed, w, h, 90);
            // Removed cells: partners outside the list are released.
            for (Environment world : List.of(ref, now, again)) world.getCellList().remove(5);
            Random moves = new Random(seed);
            for (int step = 0; step < 90; step++) {
                long jiggle = moves.nextLong();
                for (Environment world : List.of(ref, now, again)) step(world, jiggle);
                IntegrationReference.updateBonds(ref.getCellList(), ref, 1.0 / 30);
                CellAdhesionService.updateBonds(now.getCellList(), now, 1.0 / 30);
                CellAdhesionService.updateBonds(again.getCellList(), again, 1.0 / 30);
                String where = "seed " + seed + " step " + step;
                consistent(now.getCellList(), where);
                List<Cell> b = now.getCellList(), c = again.getCellList();
                for (int i = 0; i < b.size(); i++) for (int k = 0; k < 12; k++) {
                    SensorActor x = b.get(i).getSensorActors().get(k), y = c.get(i).getSensorActors().get(k);
                    assertEquals(Double.doubleToRawLongBits(x.getBondTimer()), Double.doubleToRawLongBits(y.getBondTimer()), where + " cell " + i + " actor " + k);
                    assertEquals(x.getAnchor() == null, y.getAnchor() == null, where);
                    assertEquals(x.getBondPartner() == null, y.getBondPartner() == null, where);
                }
                int[] r = count(ref.getCellList()), n = count(now.getCellList());
                former[0] += r[0]; former[1] += r[1]; current[0] += n[0]; current[1] += n[1];
            }
        }
        String summary = "bond-steps " + current[0] + " (former " + former[0] + "), anchor-steps " + current[1] + " (former " + former[1] + ")";
        System.out.println("BONDS " + summary);
        assertTrue(current[0] > 0 && current[1] > 0, summary);
        assertEquals(former[0], current[0], former[0] * .15, summary);
        assertEquals(former[1], current[1], former[1] * .15, summary);
    }
}
