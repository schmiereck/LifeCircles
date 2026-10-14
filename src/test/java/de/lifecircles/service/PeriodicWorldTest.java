package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.CellBrainService;
import de.lifecircles.service.partitioningStrategy.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PeriodicWorldTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private double sunRate;
    private int initialCount;
    @BeforeEach void configure() {
        sunRate = config.getSunRayRate(); initialCount = config.getInitialCellCount();
        config.setSunRayRate(0); config.setInitialCellCount(0);
    }
    @AfterEach void restore() {
        config.setSunRayRate(sunRate); config.setInitialCellCount(initialCount);
    }

    private Cell cell(double x, double y) {
        return new Cell(new Vector2D(x, y), 10, new PhysicsRegressionTest.ConstantBrain());
    }
    private Blocker blocker(double x, double y, double width, double height) {
        return new Blocker(new Vector2D(x, y), width, height, Color.GRAY, Blocker.BlockerType.WALL);
    }
    private PartitioningStrategy partition(Environment world, boolean tree, double range) {
        PartitioningStrategy result = tree ? new QuadTreePartitioningStrategy(world.getWidth(), world.getHeight(), range)
                : new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), range);
        result.build(world.getCellList());
        return result;
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void neighborSearchMatchesBruteForceAcrossBothSeamsAndCorners(boolean tree) {
        Random random = new Random(8172);
        for (double width : new double[]{15, 25, 105, 300}) {
            Environment world = new Environment(width, 105);
            for (int i = 0; i < 80; i++) world.addCell(cell(random.nextDouble() * width, random.nextDouble() * 105));
            world.addCell(cell(1, 1)); world.addCell(cell(width - 1, 104));
            // Include noncanonical positions without mutating the input cells while indexing.
            Cell outside = cell(2 + 3_000_000_000.0 * width, 2 - 3_000_000_000.0 * 105);
            world.addCell(outside);
            Vector2D before = outside.getPosition();
            for (double range : new double[]{20, 120, 400}) {
                PartitioningStrategy index = partition(world, tree, range);
                for (Cell a : world.getCellList()) {
                    List<Cell> neighbors = index.getNeighbors(a);
                    assertEquals(neighbors.size(), new HashSet<>(neighbors).size());
                    for (Cell b : world.getCellList()) {
                        double distance = a.getPosition().getShortestDirectionWithWrapAround(b.getPosition(), width, 105).length();
                        if (distance <= range) assertTrue(neighbors.contains(b), "Missing periodic neighbor");
                        if (tree && neighbors.contains(b)) assertTrue(distance <= range + 1e-10);
                    }
                }
            }
            assertSame(before, outside.getPosition());
        }
    }

    @Test void denseCoincidentQuadtreeTerminatesAndFindsEveryCellOnce() {
        Environment world = new Environment(300, 300);
        for (int i = 0; i < 500; i++) world.addCell(cell(0, 0));
        PartitioningStrategy tree = partition(world, true, 20);
        assertEquals(500, new HashSet<>(tree.getNeighbors(world.getCellList().getFirst())).size());
    }

    @ParameterizedTest @ValueSource(ints = {-3, -2, -1, 0, 1, 2, 3})
    void halfPeriodTieIsTranslationInvariantAndAntisymmetric(int periods) {
        Vector2D a = new Vector2D(0, 0), b = new Vector2D(150 + periods * 300, 100 + periods * 200);
        Vector2D ab = a.getShortestDirectionWithWrapAround(b, 300, 200);
        Vector2D ba = b.getShortestDirectionWithWrapAround(a, 300, 200);
        assertEquals(150, ab.getX()); assertEquals(100, ab.getY());
        assertEquals(-ab.getX(), ba.getX()); assertEquals(-ab.getY(), ba.getY());
    }

    @ParameterizedTest @ValueSource(doubles = {0, 1e-14, 299.99999999999994, 300, -1, -600, 901})
    void canonicalWrapIsBoundedAndPreservesInteriorCoordinates(double x) {
        double wrapped = Vector2D.wrapCoordinate(x, 300);
        assertTrue(wrapped >= 0 && wrapped < 300);
        if (x >= 0 && x < 300) assertEquals(x, wrapped);
        assertEquals(wrapped, Vector2D.wrapCoordinate(wrapped, 300));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void fieldForcesTorqueSensingAndTransferMatchTranslatedInterior(boolean tree) {
        double[] reference = interactions(100, 100, tree);
        for (double[] translation : new double[][]{{195, 0}, {0, 195}, {195, 195}, {-105, -105}}) {
            assertArrayEquals(reference, interactions(100 + translation[0], 100 + translation[1], tree), 1e-10);
        }
    }

    private double[] interactions(double x, double y, boolean tree) {
        Environment world = new Environment(300, 300);
        Cell a = cell(x, y), b = cell(x + 22, y + 2);
        a.setPosition(a.getPosition().wrapped(300, 300)); b.setPosition(b.getPosition().wrapped(300, 300));
        a.setEnergy(0.6); b.setEnergy(0.2);
        world.addCell(a); world.addCell(b);
        b.getSensorActors().get(6).setForceStrength(-20);
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), partition(world, tree, 60), world);
        a.updateForce(); b.updateForce();
        SensorActor actor = a.getSensorActors().getFirst();
        assertSame(b, actor.getSensedCell());
        double distanceFactor = CellBrainService.calcDistanceFactor(actor, actor.getSensedActor(), world);
        actor.setEnergyDelivery(0.001);
        EnergyTransferCellCalcService.processEnergyTransfers(world.getCellList(), world);
        return new double[]{a.getVelocity().getX(), a.getVelocity().getY(), a.getAngularVelocity(),
                b.getVelocity().getX(), b.getVelocity().getY(), b.getAngularVelocity(),
                distanceFactor, a.getEnergy(), b.getEnergy()};
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void periodicBlockerContactMatchesInteriorAndSpawnCheck(int axis) {
        Vector2D shift = axis == 0 ? new Vector2D(195, 0) : axis == 1 ? new Vector2D(0, 195) : new Vector2D(195, 195);
        Blocker interior = blocker(100, 100, 20, 20);
        Blocker seam = blocker(Vector2D.wrapCoordinate(100 + shift.getX(), 300),
                Vector2D.wrapCoordinate(100 + shift.getY(), 300), 20, 20);
        for (Vector2D point : List.of(new Vector2D(116, 100), new Vector2D(100, 116),
                new Vector2D(116, 116), new Vector2D(100, 100), new Vector2D(130, 130))) {
            Vector2D translated = point.add(shift).wrapped(300, 300);
            Vector2D expected = interior.getCellCenterOutside(point, 10).subtract(point);
            Vector2D actual = seam.getCellCenterOutside(translated, 10, 300, 300).subtract(translated);
            assertEquals(expected.getX(), actual.getX(), 1e-12);
            assertEquals(expected.getY(), actual.getY(), 1e-12);
            Environment world = new Environment(300, 300); world.addBlocker(seam);
            assertEquals(!interior.overlapsCircle(point, 10, 300, 300),
                    ReproductionManagerService.isFreePosition(world, translated, 10));
        }
    }

    @Test void blockerSensingAcrossSeamKeepsContactPriorityAndFullSignal() {
        Environment world = new Environment(300, 300);
        Cell cell = cell(295, 100); world.addCell(cell); world.addCell(cell(17, 100));
        Blocker blocker = blocker(5, 100, 2, 2); world.addBlocker(blocker);
        SensorActorForceCellCalcService.processInteractions(world.getCellList(), partition(world, false, 60), world);
        SensorActor actor = cell.getSensorActors().getFirst();
        assertSame(blocker, actor.getSensedCell());
        assertEquals(1, CellBrainService.calcDistanceFactor(actor, blocker, world));
    }

    @Test void fullWidthGroundHasNoSideExitAtSeam() {
        Environment world = new Environment(300, 300); world.addGroundBlocker();
        Blocker ground = world.getBlockerList().getFirst();
        for (double x : new double[]{0, 1, 150, 299, 300}) {
            Vector2D corrected = ground.getCellCenterOutside(new Vector2D(x, 255), 10, 300, 300);
            assertEquals(x, corrected.getX()); assertEquals(240, corrected.getY());
            assertFalse(ground.overlapsCircle(corrected, 10, 300, 300));
        }
        assertTrue(ground.overlapsCircle(new Vector2D(150, 5), 10, 300, 300));
        assertFalse(ReproductionManagerService.isFreePosition(world, new Vector2D(150, 5), 10));
    }

    @Test void completelyBlockedTorusRejectsSpawnWithoutProducingInvalidCoordinates() {
        Environment world = new Environment(300, 300);
        Blocker blocker = blocker(150, 150, 400, 400); world.addBlocker(blocker);
        assertFalse(ReproductionManagerService.isFreePosition(world, new Vector2D(0, 0), 0.5));
        Cell cell = cell(0, 0);
        BlockerCellCalcService.handleBlockerCollisions(cell, world.getBlockerList(), world);
        assertTrue(Double.isFinite(cell.getPosition().getX()));
        assertTrue(Double.isFinite(cell.getPosition().getY()));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void worldUsesOwnDimensionsEvenAfterAnotherWorldWasCreated(boolean tree) {
        Environment world = new Environment(300, 300);
        Cell a = cell(295, 100), b = cell(10, 100); world.addCell(a); world.addCell(b);
        new Environment(1000, 1000);
        world.update(1.0 / 60, partition(world, tree, 60));
        assertTrue(a.getVelocity().getX() < 0);
        assertEquals(-a.getVelocity().getX(), b.getVelocity().getX(), 1e-12);
    }

    @Test void worldCollisionAcrossSeamRemovesOnlyInwardVelocityAndUpdatesActors() {
        Environment world = new Environment(300, 300);
        world.addBlocker(blocker(5, 100, 10, 100));
        Cell cell = cell(295, 100); cell.setVelocity(new Vector2D(3, 2)); world.addCell(cell);
        world.update(0, partition(world, false, 60));
        assertEquals(290, cell.getPosition().getX(), 1e-12);
        assertEquals(0, cell.getVelocity().getX()); assertEquals(2, cell.getVelocity().getY());
        for (SensorActor actor : cell.getSensorActors()) {
            assertEquals(cell.getRadiusSize(), actor.getCachedPosition().distance(cell.getPosition()), 1e-12);
        }
    }

    @Test void reproductionRejectsPeriodicBlockerBeforeMutatingOrDebitingParent() {
        Environment world = new Environment(300, 300);
        // Actor zero points right: child center wraps from 310 to 10.
        Cell parent = cell(299, 100);
        SensorActor actor = parent.getSensorActors().getFirst();
        actor.setReproductionDesire(1); actor.setReproductionEnergyShareOutput(0.25);
        // Blocker at 299 extends across the seam up to 11.
        world.addBlocker(blocker(299, 100, 24, 10));
        double energy = parent.getEnergy();
        // ConstantBrain.mutate throws, so a mistaken acceptance cannot silently pass.
        assertNull(ReproductionManagerService.reproduce(config, world, parent));
        assertEquals(energy, parent.getEnergy());
        assertNull(ReproductionManagerService.createTrainingOffspring(config, world, parent, new Vector2D(310, 100), 0));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void translatedWorldsRemainEquivalentOverSixtyUpdates(boolean tree) {
        double[] reference = trajectory(0, 0, tree);
        for (double[] shift : new double[][]{{195, 0}, {0, 195}, {195, 195}}) {
            assertArrayEquals(reference, trajectory(shift[0], shift[1], tree), 1e-9);
        }
    }

    private double[] trajectory(double dx, double dy, boolean tree) {
        Environment world = new Environment(300, 300);
        Cell a = cell(100 + dx, 100 + dy), b = cell(115 + dx, 102 + dy);
        a.setPosition(a.getPosition().wrapped(300, 300)); b.setPosition(b.getPosition().wrapped(300, 300));
        a.setVelocity(new Vector2D(20, 3)); b.setVelocity(new Vector2D(20, 3));
        a.setAngularVelocity(2); b.setAngularVelocity(-2);
        world.addCell(a); world.addCell(b);
        for (int i = 0; i < 60; i++) world.update(1.0 / 60, partition(world, tree, 60));
        double[] result = new double[12]; int index = 0;
        for (Cell cell : world.getCellList()) {
            Vector2D position = cell.getPosition().subtract(new Vector2D(dx, dy)).wrapped(300, 300);
            result[index++] = position.getX(); result[index++] = position.getY();
            result[index++] = cell.getVelocity().getX(); result[index++] = cell.getVelocity().getY();
            result[index++] = cell.getRotation(); result[index++] = cell.getAngularVelocity();
        }
        assertEquals(12, index);
        return result;
    }
}
