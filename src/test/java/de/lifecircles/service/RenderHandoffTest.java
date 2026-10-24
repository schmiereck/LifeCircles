package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.dto.SimulationStateDto;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.World2TrainStrategy;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Rendering reads published snapshots without waiting for a step; other readers still get the lock while the
 * calculation runs flat out.
 */
class RenderHandoffTest {
    static CalculationService running(Environment world, de.lifecircles.service.trainStrategy.TrainStrategy strategy) {
        SimulationConfig config = SimulationConfig.getInstance();
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), (config.getCellMaxRadiusSize() + config.getCellActorMaxFieldRadius()) * 2);
        return new CalculationService(world, strategy, grid);
    }

    @Test void renderingNeverWaitsForAStepAndStillSeesNewSnapshots() throws Exception {
        SimulationConfig config = SimulationConfig.getInstance();
        double runTimeStep = config.getRunTimeStep();
        World2TrainStrategy strategy = new World2TrainStrategy(new Random(3));
        CalculationService service = running(strategy.initializeEnvironment(), strategy);
        try {
            config.setRunTimeStep(1e-6); // flat out
            service.start();
            Set<SimulationStateDto> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            long worst = 0, end = System.nanoTime() + 3_000_000_000L;
            while (System.nanoTime() < end) {
                long t = System.nanoTime();
                SimulationStateDto state = service.getLatestState(true);
                worst = Math.max(worst, System.nanoTime() - t);
                assertNotNull(state);
                seen.add(state);
                Thread.sleep(33);
            }
            assertTrue(seen.size() > 10, "new snapshots while running: " + seen.size());
            // A step takes milliseconds; reading the published snapshot must not wait for one.
            assertTrue(worst < 2_000_000L, "worst getLatestState " + worst / 1e6 + " ms");
            // Readers through the lock are served between steps (fair lock), not starved.
            long t = System.nanoTime();
            for (int i = 0; i < 20; i++) service.readSimulationState(() -> {});
            assertTrue((System.nanoTime() - t) / 20 < 200_000_000L, "lock readers starved");
            service.pause();
            // Paused: the snapshot follows changes immediately again (captured by the caller).
            assertNotNull(service.getLatestState(false));
        } finally { service.stop(); config.setRunTimeStep(runTimeStep); }
    }

    /**
     * Opt-in: steps per second of a saved population at full speed while a renderer polls snapshots at 30 FPS.
     * mvn test -Dtest=RenderHandoffTest#throughput -Dhandoff.bench.cells=target/trainmode-study/prof-world2.cells
     */
    @Test @EnabledIfSystemProperty(named = "handoff.bench.cells", matches = ".+")
    void throughput() throws Exception {
        SimulationConfig config = SimulationConfig.getInstance();
        double runTimeStep = config.getRunTimeStep();
        World2TrainStrategy strategy = new World2TrainStrategy(new Random(3));
        Environment world = strategy.initializeEnvironment();
        CalculationService service = running(world, strategy);
        world.resetCells(FileService.getInstance().loadCellsFromFile(System.getProperty("handoff.bench.cells")));
        try {
            config.setRunTimeStep(1e-6);
            service.start();
            Thread.sleep(5000); // warm-up
            AtomicLong frames = new AtomicLong();
            long steps = service.getStepCount(), start = System.nanoTime(), end = start + 20_000_000_000L;
            while (System.nanoTime() < end) { service.getLatestState(true); frames.incrementAndGet(); Thread.sleep(33); }
            double seconds = (System.nanoTime() - start) / 1e9;
            System.out.printf(Locale.ROOT, "HANDOFF %.1f steps/s with %.1f snapshot reads/s (%d cells)%n",
                    (service.getStepCount() - steps) / seconds, frames.get() / seconds, world.getCellList().size());
        } finally { service.stop(); config.setRunTimeStep(runTimeStep); }
    }
}
