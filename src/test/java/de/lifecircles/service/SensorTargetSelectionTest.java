package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SensorTargetSelectionTest {
    private static class SerializableBrain extends PhysicsRegressionTest.ConstantBrain implements Serializable {
        private static final long serialVersionUID = 1L;
    }
    private Cell cell(Environment world, double x, double y) {
        Cell cell = new Cell(new Vector2D(x, y), 10, new SerializableBrain()); world.addCell(cell); return cell;
    }
    private void sense(Environment world, List<Cell> neighbors) {
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), new PartitioningStrategy() {
            public void build(List<Cell> cells) {}
            public List<Cell> getNeighbors(Cell cell) { return neighbors; }
        }, world);
    }

    @Test void closestTargetWinsInEitherNeighborOrder() {
        Environment world = new Environment(300, 300);
        Cell observer = cell(world, 100, 100), farther = cell(world, 124, 100), nearer = cell(world, 122, 100);
        for (List<Cell> order : List.of(List.of(observer, farther, nearer), List.of(nearer, farther, observer))) {
            sense(world, order);
            assertSame(nearer, observer.getSensorActors().getFirst().getSensedCell());
            assertSame(nearer.getSensorActors().get(6), observer.getSensorActors().getFirst().getSensedActor());
        }
    }

    @Test void equalDistanceTargetsRemainStableUnderWorldAndNeighborPermutations() {
        Environment world = new Environment(300, 300);
        Cell observer = cell(world, 100, 100), first = cell(world, 122, 100), second = cell(world, 122, 100);
        Random random = new Random(532);
        for (int i = 0; i < 100; i++) {
            Collections.shuffle(world.getCellList(), random);
            List<Cell> neighbors = new ArrayList<>(world.getCellList()); Collections.shuffle(neighbors, random);
            sense(world, neighbors);
            assertSame(first, observer.getSensorActors().getFirst().getSensedCell());
            assertSame(first.getSensorActors().get(6), observer.getSensorActors().getFirst().getSensedActor());
            assertNotEquals(first.getSensingId(), second.getSensingId());
        }
    }

    @Test void exactActorTieUsesFixedAngleRatherThanActorListOrder() {
        Environment world = new Environment(300, 300);
        Cell observer = cell(world, 100, 100), target = cell(world, 110, 100);
        // Two cardinal actors have exactly equal distance 10; their two-actor
        // field radius is 18, so both are valid targets without numerical near-ties.
        target.getSensorActors().clear();
        SensorActor upper = new SensorActor(target, -Math.PI / 2), lower = new SensorActor(target, Math.PI / 2);
        target.getSensorActors().add(lower); target.getSensorActors().add(upper);
        for (int i = 0; i < 10; i++) {
            Collections.reverse(target.getSensorActors()); sense(world, world.getCellList());
            assertSame(upper, observer.getSensorActors().getFirst().getSensedActor());
        }
    }

    @Test void overlappingBlockersKeepPriorityAndChooseSameBlockerAfterReordering() {
        Environment world = new Environment(300, 300);
        Cell observer = cell(world, 100, 100), neighbor = cell(world, 122, 100);
        Blocker first = new Blocker(new Vector2D(110, 100), 4, 4, Color.GRAY, Blocker.BlockerType.WALL);
        Blocker second = new Blocker(new Vector2D(110, 100), 2, 2, Color.RED, Blocker.BlockerType.PLATFORM);
        world.addBlocker(second); world.addBlocker(first);
        for (int i = 0; i < 10; i++) {
            Collections.reverse(world.getBlockerList()); Collections.reverse(world.getCellList());
            sense(world, world.getCellList());
            SensorActor actor = observer.getSensorActors().getFirst();
            assertSame(first, actor.getSensedCell()); assertSame(first, actor.getSensedActor());
        }
        world.getBlockerList().clear(); sense(world, world.getCellList());
        assertSame(neighbor, observer.getSensorActors().getFirst().getSensedCell());
    }

    @Test void sensingPriorityDoesNotSuppressPhysicalFieldForces() {
        Environment world = new Environment(300, 300);
        Cell observer = cell(world, 100, 100), target = cell(world, 122, 100);
        target.getSensorActors().get(6).setForceStrength(20);
        world.addBlocker(new Blocker(new Vector2D(110, 100), 2, 2, Color.GRAY, Blocker.BlockerType.WALL));
        sense(world, world.getCellList()); observer.updateForce(); target.updateForce();
        assertInstanceOf(Blocker.class, observer.getSensorActors().getFirst().getSensedCell());
        assertTrue(observer.getVelocity().getX() < 0);
        assertEquals(0, observer.getVelocity().getX() + target.getVelocity().getX(), 1e-12);
    }

    @Test void energyDeliveryUsesSameTieWinnerAfterReordering() {
        Environment world = new Environment(300, 300);
        Cell observer = cell(world, 100, 100), first = cell(world, 122, 100), second = cell(world, 122, 100);
        for (int i = 0; i < 2; i++) {
            observer.setEnergy(0.6); first.setEnergy(0.2); second.setEnergy(0.2);
            Collections.reverse(world.getCellList()); sense(world, world.getCellList());
            observer.getSensorActors().getFirst().setEnergyDelivery(0.001);
            EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList(), world);
            assertTrue(first.getEnergy() > 0.2); assertEquals(0.2, second.getEnergy());
            assertEquals(1, observer.getEnergy() + first.getEnergy() + second.getEnergy(), 1e-12);
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void tieWinnerIsStableAcrossPeriodicSeamAndPartitioners(boolean tree) {
        Environment world = new Environment(300, 300);
        Cell observer = cell(world, 295, 295), first = cell(world, 17, 295), second = cell(world, 17, 295);
        for (int i = 0; i < 10; i++) {
            Collections.reverse(world.getCellList());
            PartitioningStrategy partition = tree ? new QuadTreePartitioningStrategy(300, 300, 60)
                    : new SpatialGridPartitioningStrategy(300, 300, 60);
            partition.build(world.getCellList());
            SensorActorForceCellCalcService.processInteractions(world.getCellList(), partition, world);
            assertSame(first, observer.getSensorActors().getFirst().getSensedCell());
        }
        assertNotEquals(first.getSensingId(), second.getSensingId());
    }

    @Test void missingTargetsAreClearedOnNextSensingPass() {
        Environment world = new Environment(300, 300);
        Cell observer = cell(world, 100, 100), target = cell(world, 122, 100);
        sense(world, world.getCellList()); assertSame(target, observer.getSensorActors().getFirst().getSensedCell());
        target.setPosition(new Vector2D(200, 200)); sense(world, world.getCellList());
        assertNull(observer.getSensorActors().getFirst().getSensedCell());
        assertNull(observer.getSensorActors().getFirst().getSensedActor());
    }

    @Test void repeatedLoadsReceiveDistinctStableRuntimeIdentities() throws Exception {
        Environment world = new Environment(300, 300); Cell original = cell(world, 100, 100);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(original); }
        Set<Long> ids = new HashSet<>(); ids.add(original.getSensingId());
        for (int i = 0; i < 2; i++) {
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                Cell loaded = (Cell) in.readObject();
                assertTrue(ids.add(loaded.getSensingId()));
            }
        }
    }
}
