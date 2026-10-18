package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.GlobalOutputFeature;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Stage 3/4 of ZELLVERBINDUNGEN.md: locked actor pairs and birth bonds, both behind switches. */
class CellBondTest {
    static final double DT = 1.0 / 60;
    static final CellType RED = CellAdhesionTest.RED, BLUE = CellAdhesionTest.BLUE, BODY = CellAdhesionTest.BODY;
    final SimulationConfig config = SimulationConfig.getInstance();
    @BeforeEach @AfterEach void reset() {
        config.setAdhesionBonds(false); config.setAdhesionBirthBonds(false);
        config.setAdhesionRecognition(true); config.setAdhesionHoldForces(true);
    }

    /** Facing pair, radius 20, surface gap 2: actor 0 of the left cell faces actor 6 of the right one. */
    static Environment pair(CellType leftActors, CellType rightActors, double force) {
        Environment world = new Environment(400, 300);
        world.addCell(CellAdhesionTest.cell(100, 20, BODY, leftActors, force));
        world.addCell(CellAdhesionTest.cell(142, 20, BODY, rightActors, force));
        return world;
    }
    static void sense(Environment world) {
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 100); grid.build(world.getCellList());
        for (Cell c : world.getCellList()) for (SensorActor a : c.getSensorActors()) a.updateCachedPosition();
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid, world, SensorActorForceCellCalcService.Affinity.fromConfig());
        for (Cell c : world.getCellList()) c.updateForce(DT); // discard impulses, only sensing matters here
    }
    static int steps(double seconds) { return (int) Math.round(seconds / DT); }
    static SensorActor left(Environment w) { return w.getCellList().get(0).getSensorActors().get(0); }
    static SensorActor right(Environment w) { return w.getCellList().get(1).getSensorActors().get(6); }

    @Test void matchingAttractiveActorsLockInAfterHalfASecondOnlyWhenSwitchedOn() {
        Environment off = pair(RED, RED, -90);
        for (int i = 0; i < steps(1); i++) { sense(off); CellAdhesionService.updateBonds(off.getCellList(), off, DT); }
        assertNull(left(off).getBondPartner(), "forming is off by default");
        config.setAdhesionBonds(true);
        Environment world = pair(RED, RED, -90);
        for (int i = 0; i < steps(CellAdhesionService.LOCK_SECONDS) - 1; i++) { sense(world); CellAdhesionService.updateBonds(world.getCellList(), world, DT); }
        assertNull(left(world).getBondPartner());
        sense(world); CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertSame(right(world), left(world).getBondPartner());
        assertSame(left(world), right(world).getBondPartner());
        assertEquals(-90, left(world).getBondStrength(), 1e-12);
    }

    @Test void foreignColoursAndRepulsiveActorsDoNotLockIn() {
        config.setAdhesionBonds(true);
        for (Environment world : new Environment[]{pair(RED, BLUE, -90), pair(RED, RED, 90)}) {
            for (int i = 0; i < steps(1); i++) { sense(world); CellAdhesionService.updateBonds(world.getCellList(), world, DT); }
            for (Cell c : world.getCellList()) for (SensorActor a : c.getSensorActors()) assertNull(a.getBondPartner());
        }
    }

    @Test void bondHoldsAgainstNeutralCommandsAndPausedPushButReleasesOnActivePush() {
        Environment world = pair(RED, BLUE, 0);
        SensorActor.bond(left(world), right(world), -60);
        // The frozen field acts although both brains command nothing and the colours differ.
        double[] v = impulse(world);
        assertTrue(v[0] > 0 && v[1] < 0); assertEquals(0, v[0] + v[1], 1e-12);
        Cell rightCell = world.getCellList().get(1);
        right(world).setForceStrength(150);
        rightCell.setBrainPaused(true, true);
        for (int i = 0; i < steps(2); i++) CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertNotNull(left(world).getBondPartner(), "a paused brain cannot release");
        rightCell.setBrainPaused(false);
        for (int i = 0; i < steps(CellAdhesionService.LOCK_SECONDS) - 1; i++) CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertNotNull(left(world).getBondPartner());
        CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertNull(left(world).getBondPartner()); assertNull(right(world).getBondPartner());
    }
    static double[] impulse(Environment world) {
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 100); grid.build(world.getCellList());
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid, world, SensorActorForceCellCalcService.Affinity.fromConfig());
        Cell l = world.getCellList().get(0), r = world.getCellList().get(1);
        l.updateForce(DT); r.updateForce(DT);
        return new double[]{l.getVelocity().getX(), r.getVelocity().getX()};
    }

    @Test void bondsTearBeyondFieldRangeAndEndWithRemovedOrFadedPartners() {
        Environment world = pair(RED, RED, 0);
        SensorActor.bond(left(world), right(world), -60);
        world.getCellList().get(1).setPosition(new Vector2D(200, 100));
        CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertNull(left(world).getBondPartner(), "stretched beyond range");

        world = pair(RED, RED, 0);
        SensorActor.bond(left(world), right(world), -60);
        Cell gone = world.getCellList().remove(1);
        CellAdhesionService.updateBonds(world.getCellList(), world, DT);
        assertNull(left(world).getBondPartner(), "partner left the world");
        assertNull(gone.getSensorActors().get(6).getBondPartner(), "both sides are released");

        world = pair(RED, RED, 0);
        SensorActor.bond(left(world), right(world), -60);
        world.getCellList().get(1).becomeRemains(true);
        double halfLife = config.getRemainsForceHalfLife();
        config.setRemainsForceHalfLife(1);
        try {
            CellAdhesionService.updateBonds(world.getCellList(), world, 1);
            assertEquals(-30, left(world).getBondStrength(), 1e-9, "bonds to remains fade with their half-life");
            for (int i = 0; i < 20; i++) CellAdhesionService.updateBonds(world.getCellList(), world, 1);
            assertNull(left(world).getBondPartner());
        } finally { config.setRemainsForceHalfLife(halfLife); }
    }

    @Test void daughterBondsToTheAttractingMotherActorFacingIt() {
        Environment world = new Environment(400, 300);
        Cell mother = CellAdhesionTest.cell(100, 20, BODY, RED, 0), daughter = CellAdhesionTest.cell(121, .5, BODY, RED, 0);
        world.addCell(mother); world.addCell(daughter);
        assertFalse(CellAdhesionService.bindAtBirth(mother, daughter, world), "a neutral actor lets the daughter go");
        mother.getSensorActors().get(0).setForceStrength(-70);
        assertTrue(CellAdhesionService.bindAtBirth(mother, daughter, world));
        assertSame(daughter.getSensorActors().get(6), mother.getSensorActors().get(0).getBondPartner());
        assertEquals(-70, mother.getSensorActors().get(0).getBondStrength());
    }

    /** Brain that keeps radius 20 and commands no fields. */
    static class Size20Brain extends PhysicsRegressionTest.ConstantBrain {
        @Override public double[] process() { double[] o = super.process(); o[GlobalOutputFeature.SIZE.ordinal()] = 1; return o; }
    }
    /**
     * Four-cell column on the ground, top cell kicked sideways at 1 s, 8 s total. Returns
     * {end-to-end distance, largest deviation of the middle cells from the end-to-end line, bonds left}.
     */
    static double[] column(boolean bonded) {
        Environment world = new Environment(400, 400); world.addGroundBlocker();
        double ground = 400 - Environment.GroundBlockerHeight;
        Cell[] column = new Cell[4];
        for (int i = 0; i < 4; i++) {
            column[i] = new Cell(new Vector2D(200, ground - 20 - 40 * i), 20, new Size20Brain());
            column[i].setType(BODY); column[i].setEnergy(1); column[i].setEnergy(EnergyType.SECONDARY, 1);
            for (SensorActor a : column[i].getSensorActors()) a.updateCachedPosition();
            world.addCell(column[i]);
        }
        // Two bonds per joint (upper-left and upper-right of the lower cell) lock the angle.
        if (bonded) for (int i = 0; i < 3; i++) {
            SensorActor.bond(column[i].getSensorActors().get(8), column[i + 1].getSensorActors().get(4), -90);
            SensorActor.bond(column[i].getSensorActors().get(10), column[i + 1].getSensorActors().get(2), -90);
        }
        var policy = new SimulationStepPolicy() {
            @Override public boolean useSunlight() { return false; }
            @Override public boolean useCellAdhesion() { return true; }
        };
        var grid = new SpatialGridPartitioningStrategy(400, 400, 100);
        for (int i = 0; i < steps(8); i++) {
            if (i == steps(1)) column[3].setVelocity(new Vector2D(30, 0));
            grid.build(world.getCellList()); world.update(DT, grid, policy);
        }
        Vector2D a = column[0].getPosition(), b = column[3].getPosition(), axis = b.subtract(a);
        double length = axis.length(), deviation = 0;
        for (int i = 1; i < 3; i++) {
            Vector2D p = column[i].getPosition().subtract(a);
            deviation = Math.max(deviation, Math.abs(axis.getX() * p.getY() - axis.getY() * p.getX()) / length);
        }
        int bonds = 0; for (Cell c : column) for (SensorActor s : c.getSensorActors()) if (s.getBondPartner() != null) bonds++;
        return new double[]{length, deviation, bonds / 2.0};
    }

    @Test void doublyBondedColumnStaysOneStraightBodyWhileLooseColumnFallsApart() {
        double[] loose = column(false), bonded = column(true);
        System.out.printf("loose %.1f/%.1f bonded %.1f/%.1f bonds %.0f%n", loose[0], loose[1], bonded[0], bonded[1], bonded[2]);
        // A rigid rod on a round foot tips over as a whole: shape, not standing, is what bonds provide.
        assertEquals(6, bonded[2]);
        // The bonds pull the joints into about 10 % contact overlap (120 -> ~108 px).
        assertTrue(bonded[0] > 100 && bonded[0] <= 121, "bonded column keeps its length");
        assertTrue(bonded[1] < 5, "and stays straight");
        assertTrue(Math.abs(loose[0] - 120) > 12 || loose[1] > 5, "loose column does not keep its shape");
    }
}
