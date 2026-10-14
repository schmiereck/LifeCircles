package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RotationTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private double friction, sunRate;
    private int initialCount;

    @BeforeEach void configure() {
        friction = config.getRotationalFriction();
        sunRate = config.getSunRayRate();
        initialCount = config.getInitialCellCount();
        config.setRotationalFriction(0.8);
        config.setSunRayRate(0);
        config.setInitialCellCount(0);
    }

    @AfterEach void restore() {
        config.setRotationalFriction(friction);
        config.setSunRayRate(sunRate);
        config.setInitialCellCount(initialCount);
    }

    private Cell cell(double omega) {
        Cell cell = new Cell(new Vector2D(150, 150), 10, new PhysicsRegressionTest.ConstantBrain());
        cell.setRotation(1);
        cell.setAngularVelocity(omega);
        return cell;
    }

    private double normalized(double angle) {
        return angle - Math.floor(angle / (2 * Math.PI)) * (2 * Math.PI);
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 10, 60, 600})
    void angleAndVelocityMatchAnalyticDampingAtEveryStepSize(int steps) {
        for (double omega : new double[]{-20, -2, 0, 2, 20}) {
            Cell cell = cell(omega);
            for (int i = 1; i <= steps; i++) {
                CellCalcService.updateCell(cell, 1.0 / steps);
                double time = (double) i / steps;
                assertEquals(omega * Math.exp(-0.8 * time), cell.getAngularVelocity(), 1e-11);
                double angle = 1 + omega / 0.8 * (1 - Math.exp(-0.8 * time));
                assertEquals(normalized(angle), cell.getRotation(), 1e-11);
                assertTrue(cell.getRotation() >= 0 && cell.getRotation() < 2 * Math.PI);
            }
        }
    }

    @ParameterizedTest @ValueSource(doubles = {-100, -2, 0, 2, 100})
    void zeroFrictionPreservesRotationSpeedInBothDirections(double omega) {
        config.setRotationalFriction(0);
        Cell cell = cell(omega);
        CellCalcService.updateCell(cell, 2);
        assertEquals(omega, cell.getAngularVelocity());
        assertEquals(normalized(1 + 2 * omega), cell.getRotation(), 1e-12);
    }

    @Test void tinyFrictionApproachesUndampedMotion() {
        config.setRotationalFriction(1e-16);
        Cell cell = cell(2);
        CellCalcService.updateCell(cell, 0.25);
        assertEquals(1.5, cell.getRotation(), 1e-15);
        assertEquals(2, cell.getAngularVelocity(), 1e-15);
    }

    @Test void strongDampingStopsWithoutReversalOrOvershootingTotalRotation() {
        config.setRotationalFriction(1000);
        Cell cell = cell(-2);
        CellCalcService.updateCell(cell, 1);
        assertEquals(0, cell.getAngularVelocity(), 0.0);
        assertEquals(0.998, cell.getRotation(), 1e-15);
    }

    @Test void zeroTimeDoesNotRotateOrDamp() {
        Cell cell = cell(-20);
        CellCalcService.updateCell(cell, 0);
        assertEquals(1, cell.getRotation());
        assertEquals(-20, cell.getAngularVelocity());
    }

    @Test void tinyNegativeAngleStillStaysInsideHalfOpenInterval() {
        config.setRotationalFriction(0);
        Cell cell = cell(-Double.MIN_VALUE);
        cell.setRotation(0);
        CellCalcService.updateCell(cell, 1);
        assertTrue(cell.getRotation() >= 0 && cell.getRotation() < 2 * Math.PI);
    }

    @ParameterizedTest @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidDampingIsRejectedWithoutChangingConfiguration(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> config.setRotationalFriction(invalid));
        assertEquals(0.8, config.getRotationalFriction());
    }

    @Test void forceThroughCenterOrAlongRadiusDoesNotRotate() {
        Cell cell = cell(0);
        cell.applyForce(new Vector2D(0, 100), cell.getPosition());
        cell.applyForce(new Vector2D(100, 0), cell.getPosition().add(new Vector2D(10, 0)));
        cell.updateForce(0.1);
        assertEquals(0, cell.getAngularVelocity());
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 60})
    void torqueImpulseScalesWithSimulationTimeAndClearsAccumulator(int steps) {
        Cell cell = cell(0);
        Vector2D point = cell.getPosition().add(new Vector2D(10, 0));
        for (int i = 0; i < steps; i++) {
            cell.applyForce(new Vector2D(0, 2), point);
            cell.updateForce(0.1 / steps);
        }
        // torque = 10*2 / 20; inertia = 10^2; impulse calibrated to 60 Hz.
        assertEquals(0.06, cell.getAngularVelocity(), 1e-14);
        cell.updateForce(0.1);
        assertEquals(0.06, cell.getAngularVelocity(), 1e-14);
        cell.applyForce(new Vector2D(0, -2), point);
        cell.updateForce(0.1);
        assertEquals(0, cell.getAngularVelocity(), 1e-14);
    }

    @Test void smallestNewbornHasFiniteTorqueResponse() {
        Cell cell = cell(0);
        cell.startGrowthProcess();
        double radius = cell.getRadiusSize();
        cell.applyForce(new Vector2D(0, 2), cell.getPosition().add(new Vector2D(radius, 0)));
        cell.updateForce(1.0 / 60);
        assertEquals(2 / (SimulationConfig.CELL_ANGULAR_VELOCITY_DIFF * radius), cell.getAngularVelocity(), 1e-14);
        CellCalcService.updateCell(cell, 1.0 / 60);
        assertTrue(Double.isFinite(cell.getAngularVelocity()));
        assertTrue(Double.isFinite(cell.getRotation()));
    }

    @ParameterizedTest @ValueSource(ints = {1, 60})
    void fullWorldUpdatesRotationAndSensorPositions(int steps) {
        Environment world = new Environment(300, 300);
        Cell cell = cell(-20);
        world.addCell(cell);
        SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(300, 300, 60);
        for (int i = 0; i < steps; i++) {
            grid.build(world.getCellList());
            world.update(1.0 / steps, grid);
        }
        double angle = normalized(1 - 20 / 0.8 * (1 - Math.exp(-0.8)));
        assertEquals(angle, cell.getRotation(), 1e-11);
        Vector2D actorPosition = cell.getSensorActors().getFirst().getCachedPosition();
        assertEquals(cell.getPosition().getX() + 10 * Math.cos(angle), actorPosition.getX(), 1e-10);
        assertEquals(cell.getPosition().getY() + 10 * Math.sin(angle), actorPosition.getY(), 1e-10);
    }
}
