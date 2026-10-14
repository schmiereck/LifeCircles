package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MechanicsRegressionTest {
    private MechanicsLab.Settings settings;
    private final SimulationConfig config = SimulationConfig.getInstance();
    @BeforeEach void save() { settings = new MechanicsLab.Settings(); }
    @AfterEach void restore() { settings.close(); }
    private MechanicsLab.Parameters balanced() { return new MechanicsLab.Parameters("balanced", 90, 125, 0.8, 3, 0.25); }

    private double fieldImpulse(double gap, double softening) {
        config.setActorFieldSofteningFraction(softening);
        Environment world = new Environment(300, 300);
        Cell a = new Cell(new Vector2D(100, 100), 10, new PhysicsRegressionTest.ConstantBrain());
        Cell b = new Cell(new Vector2D(120 + gap, 100), 10, new PhysicsRegressionTest.ConstantBrain());
        b.getSensorActors().get(6).setForceStrength(-90); world.addCell(a); world.addCell(b);
        SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(300, 300, 60); grid.build(world.getCellList());
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid, world);
        a.updateForce(); b.updateForce();
        assertEquals(0, a.getVelocity().getX() + b.getVelocity().getX(), 1e-12);
        return a.getVelocity().getX();
    }

    @ParameterizedTest @ValueSource(doubles = {-0.0001, 0, 0.0001})
    void nearCoincidentActorForceApproachesZeroContinuously(double gap) {
        double range = SensorActorForceCellCalcService.calcSensorRadius(10, 12);
        double expected = 90 * (1 - Math.abs(gap) / range) * gap / (range * 0.25) * 0.25 * 0.08;
        assertEquals(expected, fieldImpulse(gap, 0.25), 1e-12);
        assertEquals(expected / 10, fieldImpulse(gap / 10, 0.25), 1e-8);
    }

    @Test void forcesOutsideCoreKeepFormerStrength() {
        assertEquals(fieldImpulse(2, 0), fieldImpulse(2, 0.25), 1e-12);
    }

    @Test void invalidMechanicalParametersAreRejected() {
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1}) {
            assertThrows(IllegalArgumentException.class, () -> config.setActorFieldSofteningFraction(invalid));
            assertThrows(IllegalArgumentException.class, () -> config.setCellRepulsionStrength(invalid));
            assertThrows(IllegalArgumentException.class, () -> config.setCellActorMaxAttractiveForceStrength(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> config.setActorFieldSofteningFraction(1.1));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void snowmanSettlesAndStaysConnectedEvenWithFallingCells(boolean drops) {
        var result = MechanicsLab.run(balanced(), drops ? "snowman-drop" : "snowman", 1.0 / 60, null);
        assertEquals(1, result.component()); assertTrue(result.minLinks() >= 0.8);
        assertTrue(result.heightRatio() > 0.85); assertTrue(result.overlap() < 0.15);
        // After impacts, <1 world unit/s is still <5% of a body-cell radius/s.
        assertTrue(result.lateSpeed() < (drops ? 1 : 0.01)); assertTrue(result.mobility() > 1);
    }

    @Test void chainCanBendWhileTriangulatedStripResistsSameLoad() {
        var chain = MechanicsLab.run(balanced(), "chain", 1.0 / 60, null);
        var strip = MechanicsLab.run(balanced(), "strip", 1.0 / 60, null);
        assertEquals(1, chain.minLinks()); assertEquals(1, strip.minLinks());
        assertTrue(chain.peakBend() > 0.5); assertTrue(strip.peakBend() < 0.1);
        assertTrue(chain.peakBend() > 10 * strip.peakBend());
        assertTrue(strip.residualBend() < 0.005); assertTrue(strip.lateSpeed() < 0.05);
    }

    @Test void changingAttractionProducesReversibleStrokeWithoutDetachingOrFreezing() {
        var result = MechanicsLab.run(balanced(), "actuator", 1.0 / 60, null);
        assertEquals(1, result.minLinks()); assertTrue(result.stroke() > 0.02);
        assertTrue(result.lateSpeed() < 0.05); assertTrue(result.mobility() > 1);
        assertTrue(result.strokeResidual() < 0.001);
        assertTrue(result.shapeError() < 0.03);
    }

    @Test void balancedMechanicalResponseConvergesAcrossStepSizes() {
        var coarse = MechanicsLab.run(balanced(), "strip", 1.0 / 60, null);
        for (double dt : new double[]{1.0 / 120, 1.0 / 240}) {
            var fine = MechanicsLab.run(balanced(), "strip", dt, null);
            assertEquals(coarse.peakBend(), fine.peakBend(), 0.0005);
            assertEquals(coarse.shapeError(), fine.shapeError(), 0.00001);
            assertEquals(1, fine.minLinks());
        }
    }

    @Test void fixedWorldIsExactlyRepeatable() {
        assertEquals(MechanicsLab.run(balanced(), "strip", 1.0 / 60, null),
                MechanicsLab.run(balanced(), "strip", 1.0 / 60, null));
    }

    @Test void balancedPresetAppliesMeasuredParameters() {
        MechanicsPreset.BALANCED.apply(config);
        assertEquals(90, config.getCellActorMaxAttractiveForceStrength());
        assertEquals(125, config.getCellRepulsionStrength()); assertEquals(0.8, config.getViscosity());
        assertEquals(3, config.getRotationalFriction()); assertEquals(0.25, config.getActorFieldSofteningFraction());
    }
}
