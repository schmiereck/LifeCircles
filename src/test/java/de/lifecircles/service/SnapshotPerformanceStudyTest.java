package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TrainStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "performance.study", matches = "true")
class SnapshotPerformanceStudyTest {
    @Test void measure() throws Exception {
        Path output = Path.of(System.getProperty("performance.output", "target/performance"));
        Files.createDirectories(output);
        StringBuilder csv = new StringBuilder("cells,repeat,actors,captures,total_ms\n");
        try (MechanicsLab.Settings ignored = new MechanicsLab.Settings()) {
            for (int count : new int[]{1024, 4096, 16384}) {
                Environment world = new Environment(6400, 6400);
                for (int i = 0; i < count; i++) world.addCell(new Cell(new Vector2D(30 + i % 128 * 24,
                        30 + i / 128 * 24), 12, new PhysicsRegressionTest.ConstantBrain()));
                TrainStrategy strategy = new TrainStrategy() {
                    public void initialize(Environment w) {}
                    public void selectAndMutate(Environment w) {}
                    public Environment initializeEnvironment() { throw new UnsupportedOperationException(); }
                };
                CalculationService service = new CalculationService(world, strategy, new SpatialGridPartitioningStrategy(6400, 6400, 60));
                for (int warm = 0; warm < 10; warm++) { service.getLatestState(false); service.getLatestState(true); }
                for (int repeat = 0; repeat < 3; repeat++) for (boolean actors : new boolean[]{true, false}) {
                    long nanos = 0;
                    for (int capture = 0; capture < 20; capture++) {
                        // Invalidate only the payload mode, keeping source geometry identical.
                        service.getLatestState(!actors);
                        long start = System.nanoTime(); var state = service.getLatestState(actors); nanos += System.nanoTime() - start;
                        assertSame(state, service.getLatestState(actors));
                        assertEquals(actors ? 12 : 0, state.getCells().getFirst().getActors().size());
                    }
                    csv.append(String.format(Locale.ROOT, "%d,%d,%s,20,%.6f%n", count, repeat, actors, nanos / 1e6));
                }
            }
            Files.writeString(output.resolve("snapshots.csv"), csv);
        }
    }
}
