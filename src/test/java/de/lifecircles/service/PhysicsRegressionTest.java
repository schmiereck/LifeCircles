package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Small headless worlds: no random brains, sunlight, automatic population or training. */
class PhysicsRegressionTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private double friction, sunRate;
    private int initialCount;

    @BeforeEach void saveConfig() {
        friction = config.getRotationalFriction();
        sunRate = config.getSunRayRate();
        initialCount = config.getInitialCellCount();
        config.setSunRayRate(0);
        config.setInitialCellCount(0);
    }

    @AfterEach void restoreConfig() {
        config.setRotationalFriction(friction);
        config.setSunRayRate(sunRate);
        config.setInitialCellCount(initialCount);
    }

    static class ConstantBrain implements CellBrainInterface {
        public int getSynapseCount() { return 0; }
        public long getProccessedSynapses() { return 0; }
        public boolean[] determineActiveHiddenLayers(int state) { return new boolean[3]; }
        public void setInputs(double[] inputs) {}
        public int getInputCount() { return CellFactory.calcInputCount(12); }
        public double getInputValue(int index) { return 0; }
        public double getOutputValue(int index) { return process()[index]; }
        public NeuralNetwork mutate(double rate, double strength) { throw new UnsupportedOperationException(); }
        public double[] process() {
            double[] outputs = new double[CellFactory.calcOutputCount(12)];
            outputs[GlobalOutputFeature.SIZE.ordinal()] = (10.0 - 6.0) / (20.0 - 6.0);
            for (int i = 0; i < 12; i++) {
                outputs[GlobalOutputFeature.values().length + i * ActorOutputFeature.values().length
                        + ActorOutputFeature.FORCE.ordinal()] = 0.5;
            }
            return outputs;
        }
    }

    private Cell add(Environment world, double x, double y) {
        Cell cell = new Cell(new Vector2D(x, y), 10, new ConstantBrain());
        world.addCell(cell);
        return cell;
    }

    private PartitioningStrategy partition(Environment world) {
        PartitioningStrategy grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 60);
        grid.build(world.getCellList());
        return grid;
    }

    @Test void coincidentCellsSeparateWithoutInvalidNumbers() {
        Environment world = new Environment(300, 300);
        Cell a = add(world, 150, 100), b = add(world, 150, 100);
        for (int i = 0; i < 60; i++) world.update(1.0 / 60, partition(world));
        for (Cell cell : world.getCellList()) {
            assertTrue(Double.isFinite(cell.getPosition().getX()));
            assertTrue(Double.isFinite(cell.getPosition().getY()));
            assertTrue(Double.isFinite(cell.getVelocity().length()));
        }
        assertTrue(a.getPosition().distance(b.getPosition()) > 10);
    }

    @Test void repulsionAtSeamMatchesInteriorAndConservesMomentum() {
        double[] interior = repulsion(100, 115);
        double[] seam = repulsion(295, 10);
        assertArrayEquals(interior, seam, 1e-10);
        assertTrue(seam[0] < 0);
        assertEquals(0, seam[0] + seam[1], 1e-10);
    }

    private double[] repulsion(double ax, double bx) {
        Environment world = new Environment(300, 300);
        Cell a = add(world, ax, 100), b = add(world, bx, 100);
        RepulsionCellCalcService.processRepulsiveForces(world.getCellList(), partition(world));
        a.updateForce(); b.updateForce();
        return new double[]{a.getVelocity().getX(), b.getVelocity().getX()};
    }

    @Test void gridIncludesEveryNearbyPairWithoutDuplicateBuckets() {
        Random random = new Random(1849);
        for (double width : new double[]{15, 25, 45, 105, 300}) {
            Environment world = new Environment(width, 105);
            for (int i = 0; i < 50; i++) add(world, random.nextDouble() * width, random.nextDouble() * 105);
            add(world, 1, 50);
            add(world, width - 17, 50);
            SpatialGrid grid = new SpatialGrid(width, 105, 20);
            grid.addCells(world.getCellList());
            for (Cell a : world.getCellList()) {
                List<Cell> neighbors = grid.getNeighbors(a);
                assertEquals(new HashSet<>(neighbors).size(), neighbors.size());
                for (Cell b : world.getCellList()) {
                    if (a.getPosition().getShortestDirectionWithWrapAround(b.getPosition(), width, 105).length() <= 20) {
                        assertTrue(neighbors.contains(b), "Missing neighbor in world width " + width);
                    }
                }
            }
        }
    }

    @Test void negativeFieldsAttractAndPositiveFieldsRepel() {
        assertTrue(fieldPair(-20, 100, 122)[0] > 0);
        assertTrue(fieldPair(20, 100, 122)[0] < 0);
    }

    @Test void fieldForcesAndTorqueAtSeamMatchInterior() {
        assertArrayEquals(fieldPair(-20, 100, 122), fieldPair(-20, 290, 12), 1e-10);
    }

    private double[] fieldPair(double strength, double ax, double bx) {
        Environment world = new Environment(300, 300);
        Cell a = add(world, ax, 100), b = add(world, bx, 102);
        b.getSensorActors().get(6).setForceStrength(strength);
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), partition(world));
        a.updateForce(); b.updateForce();
        assertEquals(0, a.getVelocity().getX() + b.getVelocity().getX(), 1e-10);
        assertEquals(0, a.getVelocity().getY() + b.getVelocity().getY(), 1e-10);
        Vector2D centers = a.getPosition().getShortestDirectionWithWrapAround(b.getPosition(), 300, 300);
        double torque = (a.getAngularVelocity() + b.getAngularVelocity())
                * SimulationConfig.CELL_ANGULAR_VELOCITY_DIFF * 100;
        double orbitalTorque = (centers.getX() * b.getVelocity().getY()
                - centers.getY() * b.getVelocity().getX()) / 0.08;
        assertEquals(0, torque + orbitalTorque, 1e-9, "Internal forces must conserve total angular momentum");
        return new double[]{a.getVelocity().getX(), a.getVelocity().getY(),
                a.getAngularVelocity(), b.getAngularVelocity()};
    }

    @Test void denseParallelFieldWorldIsRepeatableAndConservesMomentum() {
        double[] expected = denseWorld();
        for (int run = 0; run < 10; run++) assertArrayEquals(expected, denseWorld(), 0);
    }

    private double[] denseWorld() {
        Environment world = new Environment(300, 300);
        for (int i = 0; i < 36; i++) {
            Cell cell = add(world, 70 + (i % 6) * 19, 70 + (i / 6) * 19);
            for (int j = 0; j < 12; j++) cell.getSensorActors().get(j).setForceStrength((i + j) % 2 == 0 ? -20 : 15);
        }
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), partition(world));
        double[] result = new double[36 * 3];
        double vx = 0, vy = 0;
        for (int i = 0; i < 36; i++) {
            Cell cell = world.getCellList().get(i);
            cell.updateForce();
            result[i * 3] = cell.getVelocity().getX();
            result[i * 3 + 1] = cell.getVelocity().getY();
            result[i * 3 + 2] = cell.getAngularVelocity();
            vx += result[i * 3]; vy += result[i * 3 + 1];
        }
        assertEquals(0, vx, 1e-9); assertEquals(0, vy, 1e-9);
        return result;
    }

    @Test void rotationWithoutFrictionKeepsAngularVelocity() {
        config.setRotationalFriction(0);
        Cell cell = add(new Environment(300, 300), 100, 100);
        cell.setAngularVelocity(2);
        CellCalcService.updateCell(cell, 1.0 / 60);
        assertEquals(2, cell.getAngularVelocity(), 1e-12);
    }

    @Test void dampingAndSizeChangeDependOnSimulationTime() {
        config.setRotationalFriction(0.8);
        Cell a = add(new Environment(300, 300), 100, 100);
        Cell b = add(Environment.getInstance(), 150, 100);
        a.setAngularVelocity(2); b.setAngularVelocity(2);
        CellCalcService.updateCell(a, 0.1);
        CellCalcService.updateCell(b, 0.05); CellCalcService.updateCell(b, 0.05);
        assertEquals(a.getAngularVelocity(), b.getAngularVelocity(), 1e-12);
        a.setRadiusSize(20); a.applyDelayedSizeChange(0.1);
        b.setRadiusSize(20); b.applyDelayedSizeChange(0.05);
        b.setRadiusSize(20); b.applyDelayedSizeChange(0.05);
        assertTrue(a.getRadiusSize() > 10);
        assertEquals(a.getRadiusSize(), b.getRadiusSize(), 1e-12);
    }

    @Test void forceImpulseScalesWithTimeStep() {
        Cell a = add(new Environment(300, 300), 100, 100), b = add(Environment.getInstance(), 150, 100);
        a.applyForce(new Vector2D(5, 2)); a.updateForce(0.1);
        for (int i = 0; i < 2; i++) { b.applyForce(new Vector2D(5, 2)); b.updateForce(0.05); }
        assertEquals(a.getVelocity().getX(), b.getVelocity().getX(), 1e-12);
        assertEquals(a.getVelocity().getY(), b.getVelocity().getY(), 1e-12);
    }

    @Test void blockerEjectsInsideCellAndPreservesTangentialVelocity() {
        Environment world = new Environment(300, 300);
        Blocker wall = new Blocker(new Vector2D(150, 150), 40, 100, Color.GRAY, Blocker.BlockerType.WALL);
        Cell cell = add(world, 135, 150);
        cell.setVelocity(new Vector2D(4, 3));
        BlockerCellCalcService.handleBlockerCollisions(cell, List.of(wall));
        assertEquals(120, cell.getPosition().getX(), 1e-12);
        assertFalse(wall.containsPoint(cell.getPosition()));
        assertEquals(0, cell.getVelocity().getX(), 1e-12);
        assertEquals(3, cell.getVelocity().getY(), 1e-12);
    }

    @Test void blockerCornerUsesCircleGeometry() {
        Environment world = new Environment(300, 300);
        Blocker wall = new Blocker(new Vector2D(150, 150), 40, 40, Color.GRAY, Blocker.BlockerType.WALL);
        Cell cell = add(world, 125, 125);
        BlockerCellCalcService.handleBlockerCollisions(cell, List.of(wall));
        assertEquals(10, cell.getPosition().distance(new Vector2D(130, 130)), 1e-10);
    }

    @Test void fallingCellRemainsAboveGroundOverManySteps() {
        Environment world = new Environment(300, 300);
        world.addGroundBlocker();
        Cell cell = add(world, 150, 230);
        for (int step = 0; step < 240; step++) {
            world.update(1.0 / 60, partition(world));
            assertTrue(cell.getPosition().getY() + cell.getRadiusSize() <= 250 + 1e-9);
            assertTrue(Double.isFinite(cell.getVelocity().length()));
        }
    }

    @Test void blockerContactIsNotOverwrittenByCellSensing() {
        Environment world = new Environment(300, 300);
        Cell a = add(world, 100, 100); add(world, 122, 100);
        Blocker wall = new Blocker(new Vector2D(110, 100), 1, 1, Color.GRAY, Blocker.BlockerType.WALL);
        world.addBlocker(wall);
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), partition(world));
        assertSame(wall, a.getSensorActors().getFirst().getSensedCell());
    }

    @Test void energyTransferAndSensedDistanceMatchAcrossSeam() {
        assertArrayEquals(transfer(100, 122), transfer(290, 12), 1e-12);
    }

    private double[] transfer(double ax, double bx) {
        Environment world = new Environment(300, 300);
        Cell a = add(world, ax, 100), b = add(world, bx, 100);
        a.setEnergy(0.6); b.setEnergy(0.2);
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), partition(world));
        SensorActor sensor = a.getSensorActors().getFirst();
        assertSame(b, sensor.getSensedCell());
        sensor.setEnergyDelivery(0.001);
        double distanceFactor = CellBrainService.calcDistanceFactor(sensor, sensor.getSensedActor());
        EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList());
        assertEquals(0.8, a.getEnergy() + b.getEnergy(), 1e-12);
        return new double[]{a.getEnergy(), b.getEnergy(), distanceFactor};
    }

    @Test void allBrainsReadTheSameCompletedStep() {
        Environment world = new Environment(300, 300);
        List<double[]> observed = new ArrayList<>();
        class RecordingBrain extends ConstantBrain {
            final double[] record = new double[12];
            @Override public void setInputs(double[] inputs) {
                for (int i = 0; i < 12; i++) record[i] = inputs[GlobalInputFeature.values().length
                        + i * SensorInputFeature.values().length + SensorInputFeature.SENSED_CELL_AGE.ordinal()];
            }
        }
        for (int i = 0; i < 24; i++) {
            RecordingBrain brain = new RecordingBrain();
            observed.add(brain.record);
            world.addCell(new Cell(new Vector2D(60 + (i % 6) * 21, 60 + (i / 6) * 21), 10, brain));
        }
        for (int step = 0; step < 5; step++) {
            double expectedAge = world.getCellList().getFirst().getAge();
            world.update(1.0 / 60, partition(world));
            for (double[] record : observed) for (double age : record) {
                assertTrue(age == 0 || Math.abs(age - expectedAge) < 1e-12);
            }
        }
    }

    @Test void topSensorAndSunlightInputDescribeCurrentStep() {
        Environment world = new Environment(300, 300);
        Cell cell = add(world, 100, 100);
        for (SensorActor actor : cell.getSensorActors()) actor.updateCachedPosition();
        assertEquals(90, cell.getSensorActors().get(cell.getTopSensorIndex()).getCachedPosition().getY(), 1e-12);
        cell.notifySunRayHit();
        world.update(1.0 / 60, partition(world));
        assertFalse(cell.isSunRayHit());
    }
}
