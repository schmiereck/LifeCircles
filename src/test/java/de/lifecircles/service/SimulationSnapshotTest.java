package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.dto.SimulationStateDto;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TrainStrategy;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class SimulationSnapshotTest {
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

    private Cell cell(double x) {
        Cell cell = new Cell(new Vector2D(x, 100), 10, new PhysicsRegressionTest.ConstantBrain());
        cell.setAngularVelocity(2);
        return cell;
    }

    private static class Strategy implements TrainStrategy {
        public void initialize(Environment world) {}
        public void selectAndMutate(Environment world) {}
        public Environment initializeEnvironment() { throw new UnsupportedOperationException(); }
    }

    private CalculationService service(Environment world, TrainStrategy strategy) {
        return new CalculationService(world, strategy, new SpatialGridPartitioningStrategy(300, 300, 60));
    }

    private void assertGeometry(SimulationStateDto snapshot) {
        for (var cell : snapshot.getCells()) {
            assertEquals(12, cell.getActors().size());
            for (var actor : cell.getActors()) {
                assertEquals(cell.getRadiusSize(), actor.getPosition().distance(cell.getPosition()), 1e-11);
            }
            Vector2D actor = cell.getActors().getFirst().getPosition();
            assertEquals(cell.getPosition().getX() + cell.getRadiusSize() * Math.cos(cell.getRotation()), actor.getX(), 1e-11);
            assertEquals(cell.getPosition().getY() + cell.getRadiusSize() * Math.sin(cell.getRotation()), actor.getY(), 1e-11);
        }
    }

    @Test void snapshotsAreReusedUntilSimulationChangesAndNeverWriteActorCaches() {
        Environment world = new Environment(300, 300); Cell cell = cell(100); world.addCell(cell);
        CalculationService service = service(world, new Strategy());
        SimulationStateDto before = service.getLatestState();
        assertSame(before, service.getLatestState());
        assertNull(cell.getSensorActors().getFirst().getCachedPosition());
        service.update(0.1);
        SimulationStateDto after = service.getLatestState();
        assertNotSame(before, after); assertSame(after, service.getLatestState());
        assertEquals(0, before.getCells().getFirst().getAge());
        assertEquals(0.1, after.getCells().getFirst().getAge());
        assertEquals(1, service.getStepCount());
        assertGeometry(before); assertGeometry(after);
    }

    @Test void overviewSnapshotsOmitActorsAndRestoreDetailsWithoutAdvancingSimulation() {
        Environment world = new Environment(300, 300); Cell cell = cell(100); world.addCell(cell);
        CalculationService service = service(world, new Strategy());
        world.getProfiler().setEnabled(true); world.getProfiler().reset();
        var overview = service.getLatestState(false);
        assertTrue(overview.getCells().getFirst().getActors().isEmpty());
        assertSame(overview, service.getLatestState(false));
        assertEquals(1, world.getProfiler().snapshot().get(SimulationProfiler.Phase.SNAPSHOT).calls());
        var full = service.getLatestState();
        assertGeometry(full); assertSame(full, service.getLatestState());
        assertTrue(overview.getCells().getFirst().getActors().isEmpty());
        assertEquals(0, service.getStepCount());
        service.update(.1);
        var nextOverview = service.getLatestState(false);
        assertNotSame(overview, nextOverview); assertEquals(.1, nextOverview.getCells().getFirst().getAge());
        assertTrue(nextOverview.getCells().getFirst().getActors().isEmpty());
        assertGeometry(service.getLatestState());
    }

    @Test void profilingIsOptionalAndCountsCompletePhases() {
        Environment world = new Environment(300, 300); world.addCell(cell(100));
        CalculationService service = service(world, new Strategy());
        world.getProfiler().setEnabled(false); world.getProfiler().reset();
        service.update(.01); service.getLatestState();
        assertTrue(world.getProfiler().snapshot().values().stream().allMatch(t -> t.calls() == 0));
        world.getProfiler().setEnabled(true); service.update(.01); service.getLatestState();
        for (var phase : new SimulationProfiler.Phase[]{SimulationProfiler.Phase.GRID, SimulationProfiler.Phase.SUN,
                SimulationProfiler.Phase.BRAINS, SimulationProfiler.Phase.INTERACTIONS, SimulationProfiler.Phase.SNAPSHOT}) {
            var timing = world.getProfiler().snapshot().get(phase);
            assertEquals(1, timing.calls()); assertTrue(timing.nanos() > 0);
        }
    }

    @Test void dtoCopiesMutablePositionsOnInputAndOutputAndColorsOnOutput() {
        Vector2D cellPos = new Vector2D(10, 20), actorPos = new Vector2D(30, 40);
        double[] rgb = {0.1, 0.2, 0.3};
        var actor = new SimulationStateDto.ActorStateDto(actorPos, rgb, 2);
        List<SimulationStateDto.ActorStateDto> actors = new ArrayList<>(List.of(actor));
        var cell = new SimulationStateDto.CellStateDto(cellPos, 1, 10, rgb, actors, 0.5, 2, 3);
        cellPos.setX(999); actorPos.setX(999); rgb[0] = 999; actors.clear();
        cell.getPosition().setX(888); actor.getPosition().setX(888);
        cell.getTypeRGB()[0] = 888; actor.getTypeRGB()[0] = 888;
        assertEquals(10, cell.getPosition().getX()); assertEquals(30, actor.getPosition().getX());
        assertEquals(0.1, cell.getTypeRGB()[0]); assertEquals(0.1, actor.getTypeRGB()[0]);
        assertEquals(1, cell.getActors().size());
        assertThrows(UnsupportedOperationException.class, () -> cell.getActors().clear());
    }

    @Test void dtoDetachesBlockersRaysAndAllCollections() {
        Vector2D blockerPos = new Vector2D(20, 30), start = new Vector2D(1, 2), end = new Vector2D(3, 4);
        List<Blocker> blockers = new ArrayList<>(List.of(new Blocker(blockerPos, 10, 20, Color.GRAY, Blocker.BlockerType.WALL)));
        List<SunRay> rays = new ArrayList<>(List.of(new SunRay(start, end)));
        List<SimulationStateDto.CellStateDto> cells = new ArrayList<>();
        var snapshot = new SimulationStateDto(cells, blockers, rays, 300, 200);
        blockerPos.setX(999); start.setX(999); end.setX(999); blockers.clear(); rays.clear();
        assertEquals(20, snapshot.getBlockers().getFirst().getX());
        assertEquals(1, snapshot.getSunRays().getFirst().getStartX());
        assertEquals(3, snapshot.getSunRays().getFirst().getEndX());
        assertEquals(300, snapshot.getWidth()); assertEquals(200, snapshot.getHeight());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getCells().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getBlockers().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getSunRays().clear());
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(5, TimeUnit.SECONDS), "Worker did not reach checkpoint"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    @Test void snapshotWaitsForEntirePhysicsAndTrainingTransaction() throws Exception {
        Environment world = new Environment(300, 300); Cell cell = cell(100); world.addCell(cell);
        CountDownLatch halfway = new CountDownLatch(1), release = new CountDownLatch(1), reading = new CountDownLatch(1);
        CalculationService service = service(world, new Strategy() {
            @Override public void selectAndMutate(Environment world) {
                cell.setPosition(new Vector2D(200, 100));
                halfway.countDown(); await(release);
                cell.setAge(7); cell.setRotation(1); cell.setRealRadiusSize(15);
                world.addBlocker(new Blocker(new Vector2D(50, 50), 10, 10, Color.GRAY, Blocker.BlockerType.WALL));
            }
        });
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> update = workers.submit(() -> service.update(0.1)); await(halfway);
            Future<SimulationStateDto> snapshot = workers.submit(() -> { reading.countDown(); return service.getLatestState(); });
            await(reading);
            assertThrows(TimeoutException.class, () -> snapshot.get(50, TimeUnit.MILLISECONDS));
            release.countDown(); update.get(5, TimeUnit.SECONDS);
            var state = snapshot.get(5, TimeUnit.SECONDS);
            assertEquals(200, state.getCells().getFirst().getPosition().getX());
            assertEquals(7, state.getCells().getFirst().getAge());
            assertEquals(15, state.getCells().getFirst().getRadiusSize());
            assertEquals(1, state.getBlockers().size()); assertGeometry(state);
        } finally { release.countDown(); workers.shutdownNow(); }
    }

    @Test void resetCannotInterleaveWithTrainingAndOldSnapshotStaysValid() throws Exception {
        Environment world = new Environment(300, 300); world.addCell(cell(100));
        CountDownLatch halfway = new CountDownLatch(1), release = new CountDownLatch(1), resetting = new CountDownLatch(1);
        CalculationService service = service(world, new Strategy() {
            @Override public void initialize(Environment world) { if (world.getCellList().isEmpty()) world.addCell(cell(50)); }
            @Override public void selectAndMutate(Environment world) { halfway.countDown(); await(release); }
        });
        var before = service.getLatestState();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> update = workers.submit(() -> service.update(0.1)); await(halfway);
            Future<?> reset = workers.submit(() -> { resetting.countDown(); service.resetSimulation(); }); await(resetting);
            assertThrows(TimeoutException.class, () -> reset.get(50, TimeUnit.MILLISECONDS));
            release.countDown(); update.get(5, TimeUnit.SECONDS); reset.get(5, TimeUnit.SECONDS);
            var after = service.getLatestState();
            assertEquals(100, before.getCells().getFirst().getPosition().getX());
            assertEquals(0, before.getCells().getFirst().getAge());
            assertEquals(50, after.getCells().getFirst().getPosition().getX());
            assertEquals(0, after.getCells().getFirst().getAge()); assertGeometry(after);
        } finally { release.countDown(); workers.shutdownNow(); }
    }

    @Test void detailReadExcludesSimulationUpdatesAndExceptionReleasesLock() throws Exception {
        Environment world = new Environment(300, 300); world.addCell(cell(100));
        CalculationService service = service(world, new Strategy());
        CountDownLatch reading = new CountDownLatch(1), release = new CountDownLatch(1), updating = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> reader = workers.submit(() -> service.readSimulationState(() -> { reading.countDown(); await(release); }));
            await(reading);
            Future<?> update = workers.submit(() -> { updating.countDown(); service.update(0.1); }); await(updating);
            assertThrows(TimeoutException.class, () -> update.get(50, TimeUnit.MILLISECONDS));
            release.countDown(); reader.get(5, TimeUnit.SECONDS); update.get(5, TimeUnit.SECONDS);
            assertThrows(IllegalStateException.class, () -> service.readSimulationState(() -> { throw new IllegalStateException(); }));
            workers.submit(() -> service.update(0.1)).get(5, TimeUnit.SECONDS);
            assertEquals(2, service.getStepCount());
        } finally { release.countDown(); workers.shutdownNow(); }
    }

    @Test void parallelSnapshotReadsKeepAgesAndActorGeometryConsistent() throws Exception {
        Environment world = new Environment(300, 300); world.addCell(cell(50)); world.addCell(cell(150));
        CalculationService service = service(world, new Strategy());
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> writer = workers.submit(() -> { await(start); for (int i = 0; i < 300; i++) service.update(0.001); });
            Future<?> reader = workers.submit(() -> {
                await(start);
                for (int i = 0; i < 500; i++) {
                    var state = service.getLatestState(); assertGeometry(state);
                    assertEquals(2, state.getCells().size());
                    assertEquals(state.getCells().getFirst().getAge(), state.getCells().getLast().getAge());
                }
            });
            start.countDown(); writer.get(10, TimeUnit.SECONDS); reader.get(10, TimeUnit.SECONDS);
            assertEquals(300, service.getStepCount());
        } finally { start.countDown(); workers.shutdownNow(); }
    }

    @Test void failedUpdateDoesNotPublishPartiallyMutatedWorld() {
        Environment world = new Environment(300, 300); Cell cell = cell(100); world.addCell(cell);
        CalculationService service = service(world, new Strategy() {
            int calls;
            @Override public void selectAndMutate(Environment world) {
                if (++calls <= 2) return;
                cell.setPosition(new Vector2D(200, 200)); throw new IllegalStateException("injected failure");
            }
        });
        var before = service.getLatestState();
        // There are completed steps not captured yet when a later update fails.
        service.update(0.1); service.update(0.1);
        assertThrows(IllegalStateException.class, () -> service.update(0.1));
        assertSame(before, service.getLatestState()); assertEquals(2, service.getStepCount());
        service.readSimulationState(() -> fail("Details must not read an incomplete failed update"));
        assertNull(service.findCellAt(200, 200));
    }

    @Test void clickSearchIncludesPeriodicCellImages() {
        Environment world = new Environment(300, 300); Cell cell = cell(299); world.addCell(cell);
        CalculationService service = service(world, new Strategy());
        assertSame(cell, service.findCellAt(1, 100)); assertNull(service.findCellAt(150, 100));
    }
}
