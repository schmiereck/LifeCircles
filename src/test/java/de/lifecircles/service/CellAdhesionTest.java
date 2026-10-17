package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/** Stage 1 and 2 of ZELLVERBINDUNGEN.md: force tonus across pause/death, colour/type key-lock adhesion. */
class CellAdhesionTest {
    static final double DT = 1.0 / 60;
    static final CellType RED = new CellType(1, 0, 0), BLUE = new CellType(0, 0, 1), BODY = new CellType(.2, .6, .4);
    final SimulationConfig config = SimulationConfig.getInstance();
    double tau, halfLife, sharpness, rejection;
    @BeforeEach void save() {
        tau = config.getActorForceTonusSeconds(); halfLife = config.getRemainsForceHalfLife();
        sharpness = config.getAdhesionSharpness(); rejection = config.getAdhesionRejection();
    }
    @AfterEach void restore() {
        config.setActorForceTonusSeconds(tau); config.setRemainsForceHalfLife(halfLife);
        config.setAdhesionSharpness(sharpness); config.setAdhesionRejection(rejection);
    }

    static Cell cell(double x, CellType body, CellType actors, double force) { return cell(x, 10, body, actors, force); }
    static Cell cell(double x, double radius, CellType body, CellType actors, double force) {
        Cell cell = new Cell(new Vector2D(x, 100), radius, new PhysicsRegressionTest.ConstantBrain());
        cell.setType(body); cell.setEnergy(1); cell.setEnergy(EnergyType.SECONDARY, 1);
        for (SensorActor actor : cell.getSensorActors()) {
            actor.setType(actors); actor.setForceStrength(force); actor.settleForce(1); actor.updateCachedPosition();
        }
        return cell;
    }

    @Test void commandsAreApproachedWithTheTonusTimeConstant() {
        config.setActorForceTonusSeconds(.5);
        Cell cell = cell(100, BODY, RED, 0);
        cell.getSensorActors().getFirst().setForceStrength(-90);
        CellAdhesionService.settleForces(java.util.List.of(cell), DT);
        assertEquals(-90 * (1 - Math.exp(-DT / .5)), cell.getSensorActors().getFirst().getForceStrength(), 1e-12);
        config.setActorForceTonusSeconds(0);
        cell.getSensorActors().getFirst().setForceStrength(-90);
        CellAdhesionService.settleForces(java.util.List.of(cell), DT);
        assertEquals(-90, cell.getSensorActors().getFirst().getForceStrength(), 1e-12);
    }

    @Test void pausedBrainsHoldTheirFieldsOnlyInAdhesiveWorlds() {
        Cell held = cell(100, BODY, RED, -90), legacy = cell(100, BODY, RED, -90);
        held.setBrainPaused(true, true); legacy.setBrainPaused(true);
        CellAdhesionService.settleForces(java.util.List.of(held), DT);
        assertEquals(-90, held.getSensorActors().getFirst().getForceStrength(), 1e-12);
        assertEquals(0, legacy.getSensorActors().getFirst().getForceStrength());
    }

    @Test void starvedCellInOpenWorldKeepsItsFieldWhileLegacyWorldDropsIt() {
        var noRespiration = new SimulationStepPolicy() {
            @Override public boolean keepRemains() { return true; }
            @Override public boolean useSunlight() { return false; }
            @Override public boolean useBasalBrainRespiration() { return false; }
            @Override public boolean useCellAdhesion() { return true; }
        };
        var legacy = new SimulationStepPolicy() {
            @Override public boolean useSunlight() { return false; }
            @Override public boolean useBasalBrainRespiration() { return false; }
        };
        for (SimulationStepPolicy policy : new SimulationStepPolicy[]{noRespiration, legacy}) {
            Cell cell = new Cell(new Vector2D(100, 100), 10, new BasalBrainRespirationTest.FixedBrain(10, 5, 3));
            cell.setType(BODY); cell.setEnergy(1); cell.setEnergy(EnergyType.SECONDARY, 0);
            for (SensorActor actor : cell.getSensorActors()) { actor.setForceStrength(-90); actor.settleForce(1); }
            Environment world = new Environment(300, 300); world.addCell(cell);
            var grid = new SpatialGridPartitioningStrategy(300, 300, 60); grid.build(world.getCellList());
            world.update(DT, grid, policy);
            assertTrue(cell.isBrainPaused());
            assertEquals(policy == legacy ? 0 : -90, cell.getSensorActors().getFirst().getForceStrength(), 1e-12);
        }
    }

    @Test void remainsFadeWithTheConfiguredHalfLifeOrKeepThemAsWood() {
        config.setRemainsForceHalfLife(300);
        Cell remains = cell(100, BODY, RED, -90); remains.becomeRemains(true);
        for (int i = 0; i < 300 * 60; i++) CellAdhesionService.settleForces(java.util.List.of(remains), DT);
        assertEquals(-45, remains.getSensorActors().getFirst().getForceStrength(), 1e-6);
        config.setRemainsForceHalfLife(Double.POSITIVE_INFINITY);
        Cell wood = cell(100, BODY, RED, -90); wood.becomeRemains(true);
        for (int i = 0; i < 600; i++) CellAdhesionService.settleForces(java.util.List.of(wood), DT);
        assertEquals(-90, wood.getSensorActors().getFirst().getForceStrength());
        Cell old = cell(100, BODY, RED, -90); old.becomeRemains();
        assertEquals(0, old.getSensorActors().getFirst().getForceStrength());
    }

    /** One step of the facing pair (gap 2): velocity of the left cell. */
    static double[] pairImpulse(CellType leftActors, CellType rightActors, CellType leftBody, CellType rightBody, boolean affinity) {
        Environment world = new Environment(300, 300);
        Cell left = cell(100, leftBody, leftActors, -90), right = cell(122, rightBody, rightActors, -90);
        world.addCell(left); world.addCell(right);
        var grid = new SpatialGridPartitioningStrategy(300, 300, 60); grid.build(world.getCellList());
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid, world,
                affinity ? SensorActorForceCellCalcService.Affinity.fromConfig() : null);
        left.updateForce(DT); right.updateForce(DT);
        return new double[]{left.getVelocity().getX(), right.getVelocity().getX()};
    }

    @Test void equalColoursAttractAsBeforeForeignColoursAreRejected() {
        double[] legacy = pairImpulse(RED, BLUE, BODY, BODY, false);
        double[] kin = pairImpulse(RED, RED, BODY, BODY, true);
        assertArrayEquals(pairImpulse(RED, RED, BODY, BODY, false), kin, 1e-12);
        assertTrue(kin[0] > 0 && kin[1] < 0, "kin attract");
        double[] foreign = pairImpulse(RED, BLUE, BODY, BODY, true);
        double a = Math.pow(RED.similarity(BLUE), config.getAdhesionSharpness());
        assertEquals(legacy[0] * (a - (1 - a) * config.getAdhesionRejection()), foreign[0], 1e-12);
        assertTrue(foreign[0] < 0 && foreign[1] > 0, "foreign colours repel");
        assertEquals(0, foreign[0] + foreign[1], 1e-12);
        // Mimicry of the actor colour does not hide a different body type.
        double[] mimic = pairImpulse(RED, RED, RED, BLUE, true);
        assertTrue(mimic[0] < 0, "same actor colour on a foreign body is still rejected");
    }

    /** Minimum surface gap of two foreign cells pushed together by an outer force for three seconds. */
    static double pushedGap(double radius, double push) {
        Environment world = new Environment(300, 300);
        Cell left = cell(100, radius, BODY, RED, -90), right = cell(100 + 2 * radius + 6, radius, BODY, BLUE, -90);
        world.addCell(left); world.addCell(right);
        var grid = new SpatialGridPartitioningStrategy(300, 300, 60);
        var affinity = SensorActorForceCellCalcService.Affinity.fromConfig();
        double gap = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 180; i++) {
            grid.build(world.getCellList());
            RepulsionCellCalcService.processRepulsiveForces(world.getCellList(), grid, world);
            SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid, world, affinity);
            for (Cell c : world.getCellList()) c.applyForce(c.getVelocity().multiply(-SimulationConfig.getInstance().getViscosity()), c.getPosition());
            left.applyForce(new Vector2D(push, 0)); right.applyForce(new Vector2D(-push, 0));
            for (Cell c : world.getCellList()) {
                c.updateForce(DT);
                c.setPosition(c.getPosition().add(c.getVelocity().multiply(DT)));
                c.setRotation(0); c.setAngularVelocity(0);
                for (SensorActor actor : c.getSensorActors()) actor.updateCachedPosition();
            }
            gap = Math.min(gap, right.getPosition().getX() - left.getPosition().getX() - 2 * radius);
        }
        return gap;
    }

    @Test void rejectionHoldsAgainstWeakPressureButYieldsToStrongPressure() {
        // Radius 20, default rejection: one cell weight (gravity ~29) is held off, about ten are not.
        assertEquals(.5, config.getAdhesionRejection());
        assertTrue(pushedGap(20, SimulationConfig.GRAVITY) > 1, "foreign cell held off against one cell weight");
        assertTrue(pushedGap(20, 2 * SimulationConfig.GRAVITY) > 0, "and against two");
        assertTrue(pushedGap(20, 10 * SimulationConfig.GRAVITY) < 0, "strong pressure brings them into contact");
    }
}
