package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TestATrainStrategy;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TEST_A twin figures (FEDERFELD.md): every figure once classic (99 % attraction) and once as spring (100 %,
 * threshold 1.0) under gravity on the ground. Runs the strategy's own world headless for 20 s at 60 Hz.
 */
class TestASpringFiguresTest {
    record Twin(String name, String field, int cells, double shape, double heightRatio, double component, double sag) {
        String row() {
            return String.format(Locale.ROOT, "%-9s %-7s cells=%2d shape=%.3fR height=%.3f component=%.3f sag=%.2fR",
                    name, field, cells, shape, heightRatio, component, sag);
        }
    }

    private static List<Twin> simulate(double seconds) {
        return simulate(seconds, c -> {});
    }

    private static List<Twin> simulate(double seconds, java.util.function.Consumer<SimulationConfig> variant) {
        minimumGap = Double.POSITIVE_INFINITY;
        SimulationConfig config = SimulationConfig.getInstance();
        double width = config.getWidth(), height = config.getHeight(), scale = config.getScaleSimulation(), ray = config.getEnergyPerRay();
        try (SpringFieldLab.Settings spring = new SpringFieldLab.Settings()) {
            config.setBlockerFriction(true);
            TestATrainStrategy strategy = new TestATrainStrategy();
            Environment world = strategy.initializeEnvironment();
            variant.accept(config);
            world.addGroundBlocker();
            final double radius = config.getCellMaxRadiusSize();
            List<List<Cell>> figures = new ArrayList<>();
            for (int f = 0; f < TestATrainStrategy.TWIN_NAMES.length; f++)
                for (int side = 0; side < 2; side++) figures.add(TestATrainStrategy.addTwin(world, f, side));
            List<double[][]> reference = figures.stream().map(MechanicsLab::positions).toList();
            List<Double> heights = figures.stream().map(MechanicsLab::height).toList();
            List<Double> tips = figures.stream().map(TestASpringFiguresTest::tipY).toList();
            List<Cell> cells = world.getCellList();
            int count = cells.size();
            double dt = 1.0 / Integer.getInteger("life.springHz", 60);
            SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 80);
            for (int step = 0; step < Math.round(seconds / dt); step++) {
                grid.build(cells);
                world.update(dt, grid, strategy.stepPolicy());
                for (Cell c : cells) assertTrue(Double.isFinite(c.getPosition().getX()) && Double.isFinite(c.getPosition().getY()));
                if (step % 30 == 0) minimumGap = Math.min(minimumGap, gap(figures));
            }
            minimumGap = Math.min(minimumGap, gap(figures));
            assertEquals(count, cells.size(), "population changed");
            List<Twin> result = new ArrayList<>();
            for (int i = 0; i < figures.size(); i++) {
                List<Cell> figure = figures.get(i);
                // Falling beam: deformation after the impact, so the reference shape is the starting beam.
                result.add(new Twin(TestATrainStrategy.TWIN_NAMES[i / 2], i % 2 == 0 ? "classic" : "spring", figure.size(),
                        MechanicsLab.shapeError(reference.get(i), figure, radius),
                        MechanicsLab.height(figure) / heights.get(i),
                        MechanicsLab.largestComponent(figure) / (double) figure.size(),
                        (tipY(figure) - tips.get(i)) / radius));
            }
            return result;
        } finally {
            config.setWidth(width); config.setHeight(height); config.setScaleSimulation(scale); config.setEnergyPerRay(ray);
        }
    }

    /** Smallest x distance between cells of different figures, in radii (shortest periodic distance). */
    static double minimumGap;

    private static double gap(List<List<Cell>> figures) {
        double width = SimulationConfig.getInstance().getWidth(), radius = SimulationConfig.getInstance().getCellMaxRadiusSize();
        double min = Double.POSITIVE_INFINITY;
        for (int a = 0; a < figures.size(); a++) for (int b = a + 1; b < figures.size(); b++)
            for (Cell x : figures.get(a)) for (Cell y : figures.get(b)) {
                double dx = Math.abs(x.getPosition().getX() - y.getPosition().getX());
                min = Math.min(min, Math.min(dx, width - dx) / radius);
            }
        return min;
    }

    /** Mean y of the cells in the right-most column of the top row (cantilever and bridge tip / middle). */
    private static double tipY(List<Cell> figure) {
        double top = figure.stream().mapToDouble(c -> c.getPosition().getY()).min().orElseThrow();
        double right = figure.stream().filter(c -> c.getPosition().getY() < top + 1).mapToDouble(c -> c.getPosition().getX()).max().orElseThrow();
        return figure.stream().filter(c -> c.getPosition().getX() > right - 1).mapToDouble(c -> c.getPosition().getY()).average().orElseThrow();
    }

    @Test
    void springTwinsHoldTheirShapeAtLeastAsWellAsClassic() {
        List<Twin> twins = simulate(20);
        twins.forEach(t -> System.out.println(t.row()));
        System.out.printf(Locale.ROOT, "minimum gap between figures: %.1fR%n", minimumGap);
        // Cell centres 2 R apart touch; with the actor range (< 1 R) figures must stay clearly apart.
        assertTrue(minimumGap > 4, "figures came too close: " + minimumGap);
        for (int i = 0; i < twins.size(); i += 2) {
            Twin classic = twins.get(i), spring = twins.get(i + 1);
            String both = classic.row() + " / " + spring.row();
            assertEquals(1.0, spring.component(), both);
            if (classic.name().equals("Brücke") || classic.name().equals("Kragarm")) {
                // Bridge and cantilever: both sag (spring is softer near contact), neither breaks; spring not worse.
                assertTrue(spring.shape() <= classic.shape() + 0.25, both);
            } else {
                assertTrue(spring.shape() < 0.6 * classic.shape(), both);
            }
        }
        // The classic falling beam breaks on the pedestal, the spring beam stays whole.
        assertTrue(twins.get(6).component() < 1.0, twins.get(6).row());
    }

    /** Tower twins over a long run (-Dlife.towerTrace=true): height ratio, tilt of the top and mean spin every 5 s. */
    @Test
    void towerLongRunTrace() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("life.towerTrace"));
        SimulationConfig config = SimulationConfig.getInstance();
        double width = config.getWidth(), height = config.getHeight(), scale = config.getScaleSimulation(), ray = config.getEnergyPerRay();
        try (SpringFieldLab.Settings spring = new SpringFieldLab.Settings()) {
            config.setBlockerFriction(true);
            TestATrainStrategy strategy = new TestATrainStrategy();
            Environment world = strategy.initializeEnvironment();
            if (Boolean.getBoolean("life.towerVariant")) towerVariant(config);
            world.addGroundBlocker();
            int tower = Arrays.asList(TestATrainStrategy.TWIN_NAMES).indexOf("Turm");
            List<List<Cell>> figures = List.of(TestATrainStrategy.addTwin(world, tower, 0), TestATrainStrategy.addTwin(world, tower, 1));
            List<Double> heights = figures.stream().map(MechanicsLab::height).toList();
            List<Cell> cells = world.getCellList();
            int hz = Integer.getInteger("life.springHz", 60); double dt = 1.0 / hz, radius = config.getCellMaxRadiusSize();
            SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 80);
            int seconds = Integer.getInteger("life.towerSeconds", 80);
            long start = System.nanoTime();
            for (int step = 0; step <= seconds * hz; step++) {
                if (step % (5 * hz) == 0) for (int i = 0; i < 2; i++) {
                    List<Cell> f = figures.get(i);
                    double top = f.stream().mapToDouble(c -> c.getPosition().getY()).min().orElseThrow();
                    double bottom = f.stream().mapToDouble(c -> c.getPosition().getY()).max().orElseThrow();
                    double topX = f.stream().filter(c -> c.getPosition().getY() < top + radius).mapToDouble(c -> c.getPosition().getX()).average().orElseThrow();
                    double bottomX = f.stream().filter(c -> c.getPosition().getY() > bottom - radius).mapToDouble(c -> c.getPosition().getX()).average().orElseThrow();
                    double spin = f.stream().mapToDouble(Cell::getAngularVelocity).average().orElseThrow();
                    double rot = f.stream().mapToDouble(c -> Math.IEEEremainder(c.getRotation(), Math.PI / 3)).map(Math::abs).max().orElseThrow();
                    double midY = (top + bottom) / 2;
                    double midX = f.stream().filter(c -> Math.abs(c.getPosition().getY() - midY) < radius * 1.5).mapToDouble(c -> c.getPosition().getX()).average().orElse(Double.NaN);
                    List<Cell> base = f.stream().filter(c -> c.getPosition().getY() > bottom - radius).sorted(java.util.Comparator.comparingDouble(c -> c.getPosition().getX())).toList();
                    double baseTilt = base.size() < 2 ? 0 : (base.get(base.size() - 1).getPosition().getY() - base.get(0).getPosition().getY()) / radius;
                    System.out.printf(Locale.ROOT, "TOWER t=%3ds %-7s height=%.3f tilt=%6.2fR bend=%6.2fR base=%6.3fR spin=%8.5f maxRotOff=%.3f component=%.3f%n",
                            step / hz, i == 0 ? "classic" : "spring", MechanicsLab.height(f) / heights.get(i), (topX - bottomX) / radius,
                            (midX - (topX + bottomX) / 2) / radius, baseTilt,
                            spin, rot, MechanicsLab.largestComponent(f) / (double) f.size());
                }
                grid.build(cells);
                world.update(dt, grid, strategy.stepPolicy());
            }
            System.out.printf(Locale.ROOT, "TOWER runtime %.2fs%n", (System.nanoTime() - start) / 1e9);
        } finally {
            config.setWidth(width); config.setHeight(height); config.setScaleSimulation(scale); config.setEnergyPerRay(ray);
        }
    }

    /** Hook for tower variants in the trace (-Dlife.towerVariant=true). */
    private static void towerVariant(SimulationConfig config) {
        config.setSpringBending(Double.parseDouble(System.getProperty("life.springBending", "0")));
        config.setSpringBendingDamping(Double.parseDouble(System.getProperty("life.springBendingDamping", "0")));
        config.setSpringSolver(Boolean.getBoolean("life.springSolver"));
        config.setSpringSolverStiffness(Double.parseDouble(System.getProperty("life.springSolverStiffness", "Infinity")));
        config.setSpringSolverIterations(Integer.getInteger("life.springSolverIterations", 8));
    }

    /** All twins with and without XPBD hinges (FEDERFELD.md „XPBD“), only with -Dlife.springSolverStudy=true. */
    @Test
    void solverTwinsStudy() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("life.springSolverStudy"));
        double seconds = Double.parseDouble(System.getProperty("life.studySeconds", "20"));
        for (boolean solver : new boolean[]{false, true}) {
            long start = System.nanoTime();
            List<Twin> twins = simulate(seconds, c -> c.setSpringSolver(solver));
            double runtime = (System.nanoTime() - start) / 1e9;
            twins.forEach(t -> System.out.println("SOLVER " + solver + " " + t.row()));
            System.out.printf(Locale.ROOT, "SOLVER %s runtime %.2fs gap %.1fR%n", solver, runtime, minimumGap);
        }
    }

    /** Reinforcement variants (FEDERFELD.md „Verstärkung“), only with -Dlife.springVariants=true. */
    @Test
    void reinforcementVariantsStudy() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("life.springVariants"));
        Map<String, java.util.function.Consumer<SimulationConfig>> variants = new LinkedHashMap<>();
        variants.put("base", c -> {});
        variants.put("preload", c -> c.setSpringPreload(true));
        for (double d : new double[]{0.01, 0.03, 0.1, 0.3}) variants.put("damp" + d, c -> c.setSpringDamping(d));
        for (double k : new double[]{4, 6}) for (double d : new double[]{0, 0.03, 0.1}) {
            variants.put("k" + k + "+damp" + d, c -> { c.setSpringTearFactor(k); c.setSpringStiffnessLimit(k * 20); c.setSpringDamping(d); });
            variants.put("k" + k + "+damp" + d + "+preload", c -> { c.setSpringTearFactor(k); c.setSpringStiffnessLimit(k * 20); c.setSpringDamping(d); c.setSpringPreload(true); });
        }
        variants.forEach((name, v) -> {
            List<Twin> twins = simulate(20, v);
            for (int i = 1; i < twins.size(); i += 2) System.out.println("VARIANT " + name + " " + twins.get(i).row());
        });
    }
}
