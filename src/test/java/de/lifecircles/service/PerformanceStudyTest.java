package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.*;
import de.lifecircles.service.trainStrategy.TrainStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in short, seeded throughput study; timings are evidence, never pass/fail thresholds. */
@EnabledIfSystemProperty(named = "performance.study", matches = "true")
class PerformanceStudyTest {
    @Test void measure() throws Exception {
        Path output = Path.of(System.getProperty("performance.output", "target/performance"));
        Files.createDirectories(output);
        StringBuilder csv = new StringBuilder("layout,cells,repeat,phase,calls,total_ms,steps,total_step_ms,synapses\n");
        Random originalRandom = NeuralNetwork.getRandom();
        SimulationConfig config = SimulationConfig.getInstance();
        double rate = config.getSunRayRate(), spacing = config.getSunRaySpacingPx();
        int steps = Integer.getInteger("performance.steps", 30);
        try (MechanicsLab.Settings ignored = new MechanicsLab.Settings()) {
            config.setSunRayRate(10); config.setSunRaySpacingPx(60);
            for (String layout : new String[]{"sparse", "dense"})
                for (String countText : System.getProperty("performance.counts", "1024,4096").split(","))
                    for (int repeat = 0; repeat < 3; repeat++) {
                        int count = Integer.parseInt(countText), side = (int) Math.ceil(Math.sqrt(count));
                        double width = side * 100;
                        Environment world = new Environment(width, width);
                        NeuralNetwork.setRandom(new Random(731));
                        long synapses = 0;
                        for (int i = 0; i < count; i++) {
                            double gap = layout.equals("dense") ? 24 : 96;
                            var network = new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),
                                    CellFactory.calcInputCount(12), new int[]{8}, CellFactory.calcOutputCount(12), .15, 0);
                            synapses += network.getSynapseCount();
                            double[] controls = new PhysicsRegressionTest.ConstantBrain().process().clone();
                            controls[GlobalOutputFeature.SIZE.ordinal()] = (12 - config.getCellMinRadiusSize())
                                    / (config.getCellMaxRadiusSize() - config.getCellMinRadiusSize());
                            CellBrain brain = new CellBrain(network) {
                                @Override public double[] process() { super.process(); return controls; }
                            };
                            Cell cell = new Cell(new Vector2D(80 + (i % side) * gap, 80 + (i / side) * gap), 12, brain);
                            CellBrainService.think(cell); world.addCell(cell);
                        }
                        world.addGroundBlocker();
                        TrainStrategy strategy = new TrainStrategy() {
                            public void initialize(Environment w) {}
                            public void selectAndMutate(Environment w) {}
                            public Environment initializeEnvironment() { throw new UnsupportedOperationException(); }
                        };
                        CalculationService service = new CalculationService(world, strategy,
                                new SpatialGridPartitioningStrategy(width, width, 60));
                        for (int i = 0; i < 15; i++) service.update(1.0 / 60);
                        world.getProfiler().setEnabled(true); world.getProfiler().reset();
                        long begin = System.nanoTime();
                        for (int i = 0; i < steps; i++) {
                            service.update(1.0 / 60);
                            if (i % 2 == 0) {
                                var snapshot = service.getLatestState(); assertSame(snapshot, service.getLatestState());
                            }
                        }
                        double total = (System.nanoTime() - begin) / 1e6;
                        assertEquals(count, world.getCellList().size());
                        // Separate mutation sample: no offspring inserted, so throughput layout stays unchanged.
                        for (int i = 0; i < 8; i++) ReproductionManagerService.createTrainingOffspring(config, world,
                                world.getCellList().get(i), new Vector2D(200, width / 2), 0);
                        for (var entry : world.getProfiler().snapshot().entrySet()) {
                            csv.append(String.format(Locale.ROOT, "%s,%d,%d,%s,%d,%.6f,%d,%.6f,%d%n", layout,
                                    count, repeat, entry.getKey(), entry.getValue().calls(), entry.getValue().nanos() / 1e6,
                                    steps, total, synapses));
                        }
                        Files.writeString(output.resolve("phases.csv"), csv);
                        System.out.printf(Locale.ROOT, "%s %d repeat %d: %.3f ms/step%n", layout, count, repeat, total / steps);
                    }
        } finally {
            NeuralNetwork.setRandom(originalRandom); config.setSunRayRate(rate); config.setSunRaySpacingPx(spacing);
        }
    }
}
