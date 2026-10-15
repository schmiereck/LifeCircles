package de.lifecircles.service;

import de.lifecircles.model.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "performance.study", matches = "true")
class SunPerformanceStudyTest {
    @Test void measure() throws Exception {
        SimulationConfig config = SimulationConfig.getInstance();
        double rate = config.getSunRayRate(), spacing = config.getSunRaySpacingPx();
        Path output = Path.of(System.getProperty("performance.output", "target/performance"));
        Files.createDirectories(output);
        StringBuilder csv = new StringBuilder("layout,cells,repeat,indexed,steps,rays,total_ms\n");
        try {
            config.setSunRayRate(Double.parseDouble(System.getProperty("sun.rate", "120")));
            config.setSunRaySpacingPx(Double.parseDouble(System.getProperty("sun.spacing", "20")));
            for (String layout : System.getProperty("sun.layouts", "uniform").split(","))
            for (int count : new int[]{1024, 4096, 16384}) for (int repeat = 0; repeat < 3; repeat++) {
                List<Cell> cells = new ArrayList<>(); Random positions = new Random(81);
                int side = (int) Math.ceil(Math.sqrt(count));
                for (int i = 0; i < count; i++) cells.add(new Cell(layout.equals("uniform")
                        ? new Vector2D(positions.nextDouble() * 6400, positions.nextDouble() * 6400)
                        : new Vector2D(2000 + i % side * 24, 1000 + i / side * 24),
                        12, new PhysicsRegressionTest.ConstantBrain()));
                List<Blocker> blockers = new ArrayList<>();
                for (int i = 0; i < 100; i++) blockers.add(new Blocker(new Vector2D(positions.nextDouble() * 6400,
                        positions.nextDouble() * 6400), 150, 20, Color.GRAY, Blocker.BlockerType.PLATFORM));
                if (layout.equals("shade")) blockers.add(new Blocker(new Vector2D(3200, 500), 8000, 20,
                        Color.GRAY, Blocker.BlockerType.PLATFORM));
                double[] expected = null;
                for (boolean indexed : new boolean[]{false, true}) {
                    for (Cell cell : cells) cell.setEnergy(.1);
                    var sun = new EnergySunCalcService(new Random(71), indexed);
                    for (int i = 0; i < 15; i++) sun.calculateSunEnergy(cells, blockers, 6400, 6400, config, 1.0 / 60);
                    int rays = 0; long start = System.nanoTime();
                    for (int i = 0; i < 30; i++) rays += sun.calculateSunEnergy(cells, blockers, 6400, 6400, config, 1.0 / 60).size();
                    double elapsed = (System.nanoTime() - start) / 1e6;
                    double[] energies = cells.stream().mapToDouble(Cell::getEnergy).toArray();
                    if (expected == null) expected = energies; else assertArrayEquals(expected, energies);
                    csv.append(String.format(Locale.ROOT, "%s,%d,%d,%s,30,%d,%.6f%n", layout, count, repeat, indexed, rays, elapsed));
                }
            }
            Files.writeString(output.resolve("sun.csv"), csv);
        } finally { config.setSunRayRate(rate); config.setSunRaySpacingPx(spacing); }
    }
}
