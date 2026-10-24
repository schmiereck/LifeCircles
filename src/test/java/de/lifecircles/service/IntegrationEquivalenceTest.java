package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import javafx.scene.paint.Color;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Blocker collisions with the cheap per-axis pretest, and bond/anchor updates with list marks and indexed terrain
 * checks, give the former results bit for bit (IntegrationReference).
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

    @Test void bondsAndAnchorsFormHoldAndReleaseExactlyAsBefore() {
        config.setAdhesionBonds(true); config.setGroundAdhesion(true); config.setGroundAnchors(true);
        int formed = 0, anchored = 0;
        for (long seed = 1; seed <= 4; seed++) for (boolean groundOnly : new boolean[]{false, true}) {
            config.setGroundAnchorsGroundOnly(groundOnly);
            double w = seed % 2 == 0 ? 400 : 1200, h = 300;
            Environment ref = InteractionEquivalenceTest.world(seed, w, h, 90), now = InteractionEquivalenceTest.world(seed, w, h, 90);
            // Removed cells: partners outside the list must be released the same way.
            ref.getCellList().remove(5); now.getCellList().remove(5);
            Random moves = new Random(seed);
            var affinity = SensorActorForceCellCalcService.Affinity.fromConfig();
            for (int step = 0; step < 90; step++) {
                long jiggle = moves.nextLong();
                for (Environment world : List.of(ref, now)) {
                    Random random = new Random(jiggle);
                    for (Cell cell : world.getCellList()) {
                        cell.setPosition(cell.getPosition().add(new Vector2D(random.nextDouble() * 1.2 - .6, random.nextDouble() * 1.2 - .6)));
                        for (SensorActor actor : cell.getSensorActors()) actor.updateCachedPosition();
                    }
                    var grid = new SpatialGridPartitioningStrategy(w, h, (config.getCellMaxRadiusSize() + config.getCellActorMaxFieldRadius()) * 2);
                    grid.build(world.getCellList());
                    SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid, world, affinity, null);
                }
                IntegrationReference.updateBonds(ref.getCellList(), ref, 1.0 / 30);
                CellAdhesionService.updateBonds(now.getCellList(), now, 1.0 / 30);
                List<Cell> a = ref.getCellList(), b = now.getCellList();
                for (int i = 0; i < a.size(); i++) for (int k = 0; k < 12; k++) {
                    SensorActor x = a.get(i).getSensorActors().get(k), y = b.get(i).getSensorActors().get(k);
                    String where = "seed " + seed + " step " + step + " cell " + i + " actor " + k;
                    assertEquals(Double.doubleToRawLongBits(x.getBondTimer()), Double.doubleToRawLongBits(y.getBondTimer()), where + " timer");
                    assertEquals(Double.doubleToRawLongBits(x.getBondStrength()), Double.doubleToRawLongBits(y.getBondStrength()), where + " strength");
                    assertEquals(x.getAnchor() == null, y.getAnchor() == null, where + " anchor");
                    if (x.getAnchor() != null) {
                        assertEquals(x.getAnchor().getX(), y.getAnchor().getX(), 0, where); assertEquals(x.getAnchor().getY(), y.getAnchor().getY(), 0, where);
                        if (step == 89) anchored++;
                    }
                    assertEquals(x.getBondPartner() == null, y.getBondPartner() == null, where + " partner");
                    if (x.getBondPartner() != null) {
                        int pi = a.indexOf(x.getBondPartner().getParentCell()), pk = x.getBondPartner().getParentCell().getSensorActors().indexOf(x.getBondPartner());
                        int qi = b.indexOf(y.getBondPartner().getParentCell()), qk = y.getBondPartner().getParentCell().getSensorActors().indexOf(y.getBondPartner());
                        assertEquals(pi, qi, where); assertEquals(pk, qk, where);
                        if (step == 89) formed++;
                    }
                }
            }
        }
        assertTrue(formed > 0 && anchored > 0, "bonds " + formed + ", anchors " + anchored);
    }
}
