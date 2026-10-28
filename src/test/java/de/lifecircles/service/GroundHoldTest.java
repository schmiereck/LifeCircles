package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** BODENHALT.md: stage A (terrain surface fields) and stage B (anchors), both behind switches. */
class GroundHoldTest {
    static final double DT = 1.0 / 60, TOP = 400 - Environment.GroundBlockerHeight;
    static final CellType GREY = new CellType(.5, .5, .5), RED = CellAdhesionTest.RED, BODY = CellAdhesionTest.BODY;
    final SimulationConfig config = SimulationConfig.getInstance();
    double halfLife;
    boolean friction;
    // Anchors are compared with sliding terrain; friction (BODENREIBUNG.md, default since 28.10.2026) is its own test.
    @BeforeEach void save() { halfLife = config.getRemainsForceHalfLife(); friction = config.isBlockerFriction(); config.setBlockerFriction(false); }
    @AfterEach void restore() {
        config.setBlockerFriction(friction);
        config.setGroundAdhesion(false); config.setGroundAnchors(false); config.setGroundAnchorsGroundOnly(false);
        config.setGroundStickiness(0); config.setRemainsForceHalfLife(halfLife); config.setAdhesionRecognition(true);
    }
    static SensorActorForceCellCalcService.Ground ground() { return SensorActorForceCellCalcService.Ground.fromConfig(true); }

    static Environment world() {
        Environment world = new Environment(400, 400); world.addGroundBlocker();
        return world;
    }
    /** Radius-20 cell whose bottom is gap px above the ground (negative: pressed in). */
    static Cell resting(Environment world, double x, double gap, CellType actors, double force) {
        Cell c = CellAdhesionTest.cell(x, 20, BODY, actors, force);
        c.setPosition(new Vector2D(x, TOP - 20 - gap));
        for (SensorActor a : c.getSensorActors()) a.updateCachedPosition();
        world.addCell(c);
        return c;
    }
    static SpatialGridPartitioningStrategy grid(Environment world) {
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 100); grid.build(world.getCellList());
        return grid;
    }
    /** Downward speed after one step of fields alone, for a single cell gap px above the ground. */
    static double pull(double x, double gap, CellType actors, double force, SensorActorForceCellCalcService.Ground ground) {
        Environment world = world();
        Cell c = resting(world, x, gap, actors, force);
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid(world), world, null, ground);
        c.updateForce(DT);
        return c.getVelocity().getY();
    }
    /** Fields, viscosity, gravity, an outer push, integration and terrain collisions with fixed actor forces. */
    static void step(Environment world, SensorActorForceCellCalcService.Ground ground, Vector2D push, boolean fixRotation) {
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid(world), world, null, ground);
        for (Cell c : world.getCellList()) {
            c.applyForce(c.getVelocity().multiply(-SimulationConfig.getInstance().getViscosity()), c.getPosition());
            c.applyForce(SimulationConfig.GRAVITY_VECTOR, c.getPosition());
            c.applyForce(push);
            c.updateForce(DT);
            c.setPosition(c.getPosition().add(c.getVelocity().multiply(DT)));
            if (fixRotation) { c.setRotation(0); c.setAngularVelocity(0); }
            else c.setRotation(c.getRotation() + c.getAngularVelocity() * DT);
            BlockerCellCalcService.handleBlockerCollisions(c, world.getBlockerList(), world);
            for (SensorActor a : c.getSensorActors()) a.updateCachedPosition();
        }
    }
    static int steps(double seconds) { return (int) Math.round(seconds / DT); }
    /** Anchors an actor at its nearest terrain point. */
    static void anchor(Environment world, SensorActor actor, double strength) {
        Vector2D at = actor.calcPosition(), best = null;
        for (Blocker b : world.getBlockerList()) {
            Vector2D offset = b.offsetToSurface(at, world.getWidth(), world.getHeight());
            if (best == null || offset.lengthSquared() < best.lengthSquared()) best = offset;
        }
        actor.anchor(at.add(best), strength);
    }

    @Test void terrainPullsOnlyWhenSwitchedOnAndOnlyAttractingActors() {
        assertEquals(0, pull(200, 3, GREY, -90, null), 1e-12, "no surface field without stage A");
        double grey = pull(200, 3, GREY, -90, ground());
        assertTrue(grey > 0, "attraction pulls the cell down to the ground");
        assertEquals(0, pull(200, 3, GREY, 90, ground()), 1e-12, "the ground cannot be pushed off");
        assertEquals(0, pull(200, 3, GREY, 0, ground()), 1e-12, "neutral actors on a neutral ground");
        assertEquals(0, pull(200, 12, GREY, -90, ground()), 1e-12, "beyond the field range nothing acts");
        // Periodic: the ground is wider than the world, the border behaves like the middle.
        assertEquals(grey, pull(0, 3, GREY, -90, ground()), 1e-12);
    }

    @Test void foreignColoursHoldWeakerButAreNeverRejected() {
        double grey = pull(200, 3, GREY, -90, ground()), red = pull(200, 3, RED, -90, ground());
        // sim(red, grey) = 0.5, sharpness 4.
        assertEquals(Math.pow(.5, 4), red / grey, 1e-9);
        assertEquals(grey, pull(200, 3, RED, -90, SensorActorForceCellCalcService.Ground.fromConfig(false)), 1e-12,
                "without recognition every colour holds fully");
        config.setGroundStickiness(-45);
        assertEquals(pull(200, 3, GREY, -45, new SensorActorForceCellCalcService.Ground(0, 4)),
                pull(200, 3, GREY, 0, ground()), 1e-12, "a sticky ground holds neutral actors like their own attraction");
    }

    /** Gap between cell bottom and ground after three seconds under an upward push. */
    static double liftedGap(SensorActorForceCellCalcService.Ground ground, double push) {
        Environment world = world();
        Cell c = resting(world, 200, 0, GREY, -90);
        for (int i = 0; i < steps(3); i++) step(world, ground, new Vector2D(0, -push), true);
        return TOP - c.getPosition().getY() - 20;
    }

    @Test void grippingCellNeedsSeveralCellWeightsToLiftOff() {
        double g = SimulationConfig.GRAVITY;
        System.out.printf("lift gaps: free 1.5g %.2f, grip 2g %.2f, grip 4g %.2f, grip 10g %.2f%n",
                liftedGap(null, 1.5 * g), liftedGap(ground(), 2 * g), liftedGap(ground(), 4 * g), liftedGap(ground(), 10 * g));
        assertTrue(liftedGap(null, 1.5 * g) > 50, "without grip one and a half cell weights lift the cell");
        assertTrue(liftedGap(ground(), 2 * g) < 1, "with grip two cell weights do not");
        assertTrue(liftedGap(ground(), 10 * g) > 50, "strong pull tears it off");
    }

    /**
     * Largest net turn within two seconds after a spin, cell resting on the ground. Only the foot actors
     * (indices 2-4) attract: attraction all around would form a periodic wheel that turns through.
     */
    static double turned(SensorActorForceCellCalcService.Ground ground) {
        Environment world = world();
        Cell c = resting(world, 200, 0, GREY, -90);
        for (int i = 0; i < 12; i++) if (i < 2 || i > 4) c.getSensorActors().get(i).setForceStrength(0);
        c.setAngularVelocity(2);
        double angle = 0, largest = 0, last = c.getRotation();
        for (int i = 0; i < steps(2); i++) {
            step(world, ground, new Vector2D(0, 0), false);
            angle += Math.IEEEremainder(c.getRotation() - last, 2 * Math.PI); last = c.getRotation();
            largest = Math.max(largest, Math.abs(angle));
        }
        return largest;
    }

    @Test void gripResistsRollingButNotSliding() {
        double free = turned(null), grip = turned(ground());
        System.out.printf("turned: free %.2f rad, grip %.2f rad%n", free, grip);
        assertTrue(grip < free / 2, "attracting actors pull the cell back when it rolls");
        // Stage A acts normal to the surface: sideways the cell still slides freely.
        Environment a = world(), b = world();
        Cell ca = resting(a, 200, 0, GREY, -90), cb = resting(b, 200, 0, GREY, -90);
        for (int i = 0; i < steps(2); i++) {
            step(a, null, new Vector2D(SimulationConfig.GRAVITY, 0), true);
            step(b, ground(), new Vector2D(SimulationConfig.GRAVITY, 0), true);
        }
        assertEquals(ca.getPosition().getX(), cb.getPosition().getX(), 1e-6);
    }

    /** Senses and runs the bond pass once per step for a fixed world, discarding impulses. */
    static void sense(Environment world, int steps) {
        for (int i = 0; i < steps; i++) {
            SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid(world), world, null, ground());
            for (Cell c : world.getCellList()) c.updateForce(DT);
            CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        }
    }
    static long anchors(Cell c) { return c.getSensorActors().stream().filter(a -> a.getAnchor() != null).count(); }

    @Test void touchingGreyAttractionAnchorsAfterHalfASecondOnlyWithBothSwitches() {
        config.setGroundAdhesion(true);
        Environment off = world(); Cell offCell = resting(off, 200, -.5, GREY, -90);
        sense(off, steps(1));
        assertEquals(0, anchors(offCell), "anchors are off by default");
        config.setGroundAnchors(true); config.setGroundAdhesion(false);
        sense(off, steps(1));
        assertEquals(0, anchors(offCell), "anchors need stage A");
        config.setGroundAdhesion(true);
        Environment world = world(); Cell c = resting(world, 200, -.5, GREY, -90);
        sense(world, steps(CellAdhesionService.LOCK_SECONDS) - 1);
        assertEquals(0, anchors(c));
        sense(world, 1);
        // Pressed 0.5 px in, the bottom actor (index 3) anchors at its contact point; its neighbours at
        // +-30 deg are 2.2 px above the ground, within half the field range (4.7 px), and anchor right below.
        SensorActor bottom = c.getSensorActors().get(3), side = c.getSensorActors().get(2);
        assertEquals(3, anchors(c));
        assertEquals(200, bottom.getAnchor().getX(), 1e-9); assertEquals(TOP + .5, bottom.getAnchor().getY(), 1e-9);
        assertEquals(side.calcPosition().getX(), side.getAnchor().getX(), 1e-9); assertEquals(TOP, side.getAnchor().getY(), 1e-9);
        assertEquals(-90, bottom.getBondStrength(), 1e-12);
        Environment high = world(); Cell above = resting(high, 200, 6, GREY, -90);
        sense(high, steps(1));
        assertEquals(0, anchors(above), "6 px above the ground is beyond half the field range");
        Environment red = world(), pushing = world();
        Cell redCell = resting(red, 200, -.5, RED, -90), pushCell = resting(pushing, 200, -.5, GREY, 90);
        sense(red, steps(1)); sense(pushing, steps(1));
        assertEquals(0, anchors(redCell), "foreign colours do not anchor");
        assertEquals(0, anchors(pushCell), "repelling actors do not anchor");
    }

    /** A wall to the left of a radius-20 cell at (245, 150), touching or ground-only. */
    static Environment wallWorld() {
        Environment world = world();
        world.addWallBlocker(200, 330, 60); // x 200..225, y 70..340
        return world;
    }

    @Test void groundOnlyAnchorsDoNotClimbWalls() {
        config.setGroundAdhesion(true); config.setGroundAnchors(true);
        for (boolean groundOnly : new boolean[]{false, true}) {
            config.setGroundAnchorsGroundOnly(groundOnly);
            Environment world = wallWorld();
            Cell c = CellAdhesionTest.cell(244.5, 20, BODY, GREY, -90);
            c.setPosition(new Vector2D(244.5, 150)); world.addCell(c);
            sense(world, steps(1));
            assertEquals(groundOnly ? 0 : 3, anchors(c), "actors at 150, 180 and 210 deg face the wall");
        }
    }

    @Test void anchorsReleaseLikeBonds() {
        Environment world = world();
        Cell c = resting(world, 200, 0, GREY, 0);
        SensorActor bottom = c.getSensorActors().get(3);
        anchor(world, bottom, -90);
        bottom.setForceStrength(150);
        c.setBrainPaused(true, true);
        for (int i = 0; i < steps(2); i++) CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertNotNull(bottom.getAnchor(), "a paused brain cannot release");
        c.setBrainPaused(false);
        for (int i = 0; i < steps(CellAdhesionService.LOCK_SECONDS) - 1; i++) CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertNotNull(bottom.getAnchor());
        CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertNull(bottom.getAnchor(), "an active push releases");

        bottom.setForceStrength(0);
        anchor(world, bottom, -90);
        c.setPosition(c.getPosition().add(new Vector2D(0, -15)));
        CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertNull(bottom.getAnchor(), "torn off beyond the field range");

        c.setPosition(new Vector2D(200, TOP - 20));
        anchor(world, bottom, -90);
        world.getBlockerList().clear();
        CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertNull(bottom.getAnchor(), "no terrain under the anchor any more");

        world.addGroundBlocker();
        anchor(world, bottom, -90);
        c.becomeRemains(true);
        config.setRemainsForceHalfLife(1);
        CellAdhesionService.updateBonds(world.getCellList(), world, 1);
        assertEquals(-45, bottom.getBondStrength(), 1e-9, "remains loosen with their half-life");
        for (int i = 0; i < 20; i++) CellAdhesionService.updateBonds(world.getCellList(), world, 1);
        assertNull(bottom.getAnchor());
        config.setRemainsForceHalfLife(Double.POSITIVE_INFINITY);
        anchor(world, bottom, -90);
        for (int i = 0; i < 20; i++) CellAdhesionService.updateBonds(world.getCellList(), world, 1);
        assertEquals(-90, bottom.getBondStrength(), 1e-12, "wood stays anchored");
    }

    @Test void anchorsAreSavedAndDroppedInAWorldWithoutTerrainThere(@TempDir java.nio.file.Path dir) throws Exception {
        Environment world = world();
        Cell c = CellFactory.createCell(new Vector2D(200, TOP - 20), 20, .02, 0, 0); // serialisable brain
        for (SensorActor a : c.getSensorActors()) a.updateCachedPosition();
        world.addCell(c);
        anchor(world, c.getSensorActors().get(3), -70);
        String file = dir.resolve("anchored.cells").toString();
        FileService.getInstance().saveCellsToFile(file, new java.util.ArrayList<>(world.getCellList()));
        Cell loaded = FileService.getInstance().loadCellsFromFile(file).get(0);
        SensorActor bottom = loaded.getSensorActors().get(3);
        assertEquals(200, bottom.getAnchor().getX(), 1e-9); assertEquals(TOP, bottom.getAnchor().getY(), 1e-9);
        assertEquals(-70, bottom.getBondStrength(), 1e-12);
        Environment same = world(); same.addCell(loaded);
        CellAdhesionService.updateBonds(same.getCellList(), same, DT);
        assertNotNull(bottom.getAnchor(), "the same terrain keeps it");
        Environment bare = new Environment(400, 400); bare.addCell(loaded);
        CellAdhesionService.updateBonds(bare.getCellList(), bare, DT);
        assertNull(bottom.getAnchor(), "no terrain at the anchor point");
    }

    @Test void anchorsHoldSidewaysAndOnWalls() {
        // Sideways: the bottom three actors anchored, pushed with one cell weight for three seconds.
        Environment loose = world(), held = world();
        Cell l = resting(loose, 200, 0, GREY, -90), h = resting(held, 200, 0, GREY, -90);
        for (int i = 2; i <= 4; i++) anchor(held, h.getSensorActors().get(i), -90);
        for (int i = 0; i < steps(3); i++) {
            step(loose, ground(), new Vector2D(SimulationConfig.GRAVITY, 0), false);
            step(held, ground(), new Vector2D(SimulationConfig.GRAVITY, 0), false);
        }
        System.out.printf("sideways: loose %.1f px, anchored %.1f px%n", l.getPosition().getX() - 200, h.getPosition().getX() - 200);
        assertTrue(l.getPosition().getX() - 200 > 50, "surface field alone slides");
        assertTrue(Math.abs(h.getPosition().getX() - 200) < 3, "anchors hold sideways");

        // Wall: three left actors on the wall, gravity only.
        Environment slide = wallWorld(), hang = wallWorld();
        Cell s = CellAdhesionTest.cell(245, 20, BODY, GREY, -90), w = CellAdhesionTest.cell(245, 20, BODY, GREY, -90);
        s.setPosition(new Vector2D(245, 150)); w.setPosition(new Vector2D(245, 150));
        slide.addCell(s); hang.addCell(w);
        for (int i = 5; i <= 7; i++) anchor(hang, w.getSensorActors().get(i), -90);
        for (int i = 0; i < steps(3); i++) {
            step(slide, ground(), new Vector2D(0, 0), false);
            step(hang, ground(), new Vector2D(0, 0), false);
        }
        System.out.printf("wall: field only dropped %.1f px, anchored %.1f px%n", s.getPosition().getY() - 150, w.getPosition().getY() - 150);
        assertTrue(s.getPosition().getY() - 150 > 100, "the surface field alone slides down the wall");
        assertTrue(w.getPosition().getY() - 150 < 3, "anchored on the wall it hangs");
    }

    /**
     * The bonded four-cell column of CellBondTest, kicked sideways at 1 s, 8 s total. Returns the tilt of the
     * foot-to-top axis from vertical in degrees.
     */
    static double columnTilt(boolean anchored) {
        Environment world = world();
        Cell[] column = new Cell[4];
        for (int i = 0; i < 4; i++) {
            column[i] = new Cell(new Vector2D(200, TOP - 20 - 40 * i), 20, new CellBondTest.Size20Brain());
            column[i].setType(BODY); column[i].setEnergy(1); column[i].setEnergy(EnergyType.SECONDARY, 1);
            for (SensorActor a : column[i].getSensorActors()) a.updateCachedPosition();
            world.addCell(column[i]);
        }
        for (int i = 0; i < 3; i++) {
            SensorActor.bond(column[i].getSensorActors().get(8), column[i + 1].getSensorActors().get(4), -90);
            SensorActor.bond(column[i].getSensorActors().get(10), column[i + 1].getSensorActors().get(2), -90);
        }
        // Foot actors at 60 deg and 120 deg, 2.7 px above the ground.
        if (anchored) for (int i : new int[]{2, 4}) anchor(world, column[0].getSensorActors().get(i), -90);
        var policy = new SimulationStepPolicy() {
            @Override public boolean useSunlight() { return false; }
            @Override public boolean useCellAdhesion() { return true; }
        };
        var grid = new SpatialGridPartitioningStrategy(400, 400, 100);
        for (int i = 0; i < steps(8); i++) {
            if (i == steps(1)) column[3].setVelocity(new Vector2D(30, 0));
            grid.build(world.getCellList()); world.update(DT, grid, policy);
        }
        Vector2D axis = column[0].getPosition().getShortestDirectionWithWrapAround(column[3].getPosition(), 400, 400);
        return Math.toDegrees(Math.atan2(Math.abs(axis.getX()), -axis.getY()));
    }

    @Test void anchoredColumnStillStandsAfterAKickWhileTheLooseOneTips() {
        double loose = columnTilt(false), anchored = columnTilt(true);
        System.out.printf("column tilt: loose %.1f deg, anchored %.1f deg%n", loose, anchored);
        assertTrue(loose > 45, "without anchors the rigid column tips over");
        assertTrue(anchored < 10, "two anchored foot actors keep it upright");
    }
}
