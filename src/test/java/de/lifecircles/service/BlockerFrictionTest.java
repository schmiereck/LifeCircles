package de.lifecircles.service;

import de.lifecircles.model.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** BODENREIBUNG.md: Coulomb friction and rolling resistance where a cell presses on terrain. */
class BlockerFrictionTest {
    static final double DT = 1.0 / 60, R = 20, WIDTH = 2000, HEIGHT = 400;
    final SimulationConfig config = SimulationConfig.getInstance();
    double gravity, viscosity;

    @BeforeEach void save() { gravity = config.getGravity(); viscosity = config.getViscosity(); }
    @AfterEach void restore() { config.setGravity(gravity); config.setViscosity(viscosity); }

    Environment world() {
        Environment world = new Environment(WIDTH, HEIGHT);
        world.addGroundBlocker();
        return world;
    }

    Cell resting() {
        Cell cell = new Cell(new Vector2D(500, HEIGHT - Environment.GroundBlockerHeight - R), R, new PhysicsRegressionTest.ConstantBrain());
        cell.setRadiusSize(R);
        return cell;
    }

    /** One step as in Environment.update for a lone cell, with an extra force at the centre. */
    void step(Environment world, Cell cell, double friction, double rolling, Vector2D extra) {
        cell.applyForce(cell.getVelocity().multiply(-config.getViscosity()), cell.getPosition());
        cell.applyForce(new Vector2D(0, config.getGravity()).add(extra), cell.getPosition());
        cell.updateForce(DT);
        cell.setPosition(cell.getPosition().add(cell.getVelocity().multiply(DT)));
        cell.setRotation(cell.getRotation() + cell.getAngularVelocity() * DT);
        cell.setAngularVelocity(cell.getAngularVelocity() * Math.exp(-config.getRotationalFriction() * DT));
        BlockerCellCalcService.handleBlockerCollisions(cell, world.getBlockerList(), world, friction, rolling, DT);
    }

    double slide(double friction, double seconds) {
        Environment world = world();
        Cell cell = resting();
        cell.setVelocity(new Vector2D(100, 0));
        double start = cell.getPosition().getX();
        for (int i = 0; i < Math.round(seconds / DT); i++) step(world, cell, friction, .5, new Vector2D(0, 0));
        return cell.getPosition().getX() - start;
    }

    @Test void aSlidingCellStopsOnlyWithFriction() {
        config.setViscosity(0);
        double free = slide(0, 3), braked = slide(1, 3);
        System.out.printf(Locale.ROOT, "slide 100 px/s for 3 s without viscosity: free %.1f px, friction 1: %.1f px%n", free, braked);
        assertTrue(free > 250, "rolling resistance alone does not stop a sliding cell: " + free);
        // Deceleration at most the weight: 100^2 / (2 * 0.08 * g * 60) = 35 px; part of the speed goes into spin.
        assertTrue(braked > 5 && braked < 45, "friction stops it: " + braked);
        config.setGravity(0);
        assertEquals(slide(0, 3), slide(1, 3), 1e-9, "nothing presses the cell on: no friction");
    }

    /** Distance moved in 3 s under a sideways pull while pressed on by gravity plus an extra force (both in g). */
    double pull(double sideways, double press) {
        Environment world = world();
        Cell cell = resting();
        double start = cell.getPosition().getX(), g = config.getGravity();
        for (int i = 0; i < 180; i++) step(world, cell, 1, .5, new Vector2D(sideways * g, press * g));
        return cell.getPosition().getX() - start;
    }

    @Test void pressingOnHoldsAgainstASidewaysPull() {
        System.out.printf(Locale.ROOT, "pull 0.4 g: %.3f px; 0.8 g: %.1f px; 0.8 g pressed on with 1 g more: %.3f px%n",
                pull(.4, 0), pull(.8, 0), pull(.8, 1));
        // A pull at the centre tips the cell over the edge of its contact patch above 0.5 of the load.
        assertEquals(0, pull(.4, 0), 1e-6, "the contact holds below the rolling resistance");
        assertTrue(pull(.8, 0) > 20, "a stronger pull rolls it away");
        assertEquals(0, pull(.8, 1), 1e-6, "pressing on (e.g. an attracting foot) holds it again");
    }

    double spin(double friction, double rolling, double[] slip) {
        Environment world = world();
        Cell cell = resting();
        cell.setAngularVelocity(2);
        double start = cell.getPosition().getX();
        for (int i = 0; i < 120; i++) step(world, cell, friction, rolling, new Vector2D(0, 0));
        slip[0] = cell.getVelocity().getX() - R * cell.getAngularVelocity();
        return cell.getPosition().getX() - start;
    }

    @Test void spinTurnsIntoRollingWithFriction() {
        double[] slip = new double[1];
        assertEquals(0, spin(0, 0, slip), 1e-9, "without friction a spinning cell stays");
        double wheel = spin(1, 0, slip);
        // Positive spin turns from +x towards +y (down): the contact point moves back, the cell rolls ahead.
        assertTrue(wheel > 5, "rolls: " + wheel);
        assertEquals(0, slip[0], 1e-6, "without slipping");
        double patch = spin(1, .5, slip);
        System.out.printf(Locale.ROOT, "spin 2 rad/s, 2 s: wheel %.1f px, with rolling resistance %.1f px%n", wheel, patch);
        assertTrue(patch > 0 && patch < wheel / 2, "the contact patch brakes the rolling: " + patch);
    }
}
