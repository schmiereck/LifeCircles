package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** From NN output to actual impulse, with one isolated interacting actor pair. */
class ForceSignTest {
    private static class FieldBrain extends PhysicsRegressionTest.ConstantBrain {
        final int actor;
        final double output;
        FieldBrain(int actor, double output) { this.actor = actor; this.output = output; }
        @Override public double[] process() {
            double[] outputs = super.process();
            outputs[GlobalOutputFeature.values().length + actor * ActorOutputFeature.values().length
                    + ActorOutputFeature.FORCE.ordinal()] = output;
            return outputs;
        }
    }

    @ParameterizedTest @ValueSource(doubles = {-0.5, 0, 0.25, 0.5, 0.75, 1, 1.5})
    void outputMappingAndPhysicalDirectionAgreeOnBothAxesAndAcrossSeams(double output) {
        SimulationConfig config = SimulationConfig.getInstance();
        double normalizedStrength = Math.max(-1, Math.min(1, output * 2 - 1));
        double expectedStrength = normalizedStrength * (output < 0.5
                ? config.getCellActorMaxAttractiveForceStrength() : config.getCellActorMaxRepulsiveForceStrength());
        for (boolean vertical : new boolean[]{false, true}) {
            for (boolean seam : new boolean[]{false, true}) {
                for (boolean leftEmits : new boolean[]{false, true}) {
                    Environment world = new Environment(300, 300);
                    double a = seam ? 295 : 100, b = seam ? 17 : 122;
                    Cell left = new Cell(vertical ? new Vector2D(100, a) : new Vector2D(a, 100),
                            10, new FieldBrain(0, leftEmits ? output : 0.5));
                    Cell right = new Cell(vertical ? new Vector2D(100, b) : new Vector2D(b, 100),
                            10, new FieldBrain(6, leftEmits ? 0.5 : output));
                    if (vertical) { left.setRotation(Math.PI / 2); right.setRotation(Math.PI / 2); }
                    world.addCell(left); world.addCell(right);
                    CellBrainService.think(left); CellBrainService.think(right);
                    SensorActor emitter = leftEmits ? left.getSensorActors().getFirst() : right.getSensorActors().get(6);
                    assertEquals(expectedStrength, emitter.getForceStrength(), 1e-12);
                    SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(300, 300, 60);
                    grid.build(world.getCellList());
                    SensorActorForceCellCalcService.processInteractions(world.getCellList(), grid, world);
                    left.updateForce(1.0 / 60); right.updateForce(1.0 / 60);
                    double leftVelocity = vertical ? left.getVelocity().getY() : left.getVelocity().getX();
                    double rightVelocity = vertical ? right.getVelocity().getY() : right.getVelocity().getX();
                    // Pair gap = 2, field range = 2*r*sin(pi/12)*factor,
                    // size scale = r^2 / maxRadius^2; impulse scale = .08 at 60 Hz.
                    double range = 20 * Math.sin(Math.PI / 12) * SimulationConfig.cellActorMaxFieldRadiusFactor;
                    double expectedImpulse = -expectedStrength * (1 - 2 / range)
                            * 100 / Math.pow(config.getCellMaxRadiusSize(), 2) * 0.08;
                    assertEquals(expectedImpulse, leftVelocity, 1e-11);
                    assertEquals(-expectedImpulse, rightVelocity, 1e-11);
                    assertEquals(0, leftVelocity + rightVelocity, 1e-11);
                    if (output < 0.5) assertTrue(leftVelocity > 0 && rightVelocity < 0, "Negative strength must attract");
                    else if (output > 0.5) assertTrue(leftVelocity < 0 && rightVelocity > 0, "Positive strength must repel");
                    else { assertEquals(0, leftVelocity, 1e-14); assertEquals(0, rightVelocity, 1e-14); }
                }
            }
        }
    }
}
