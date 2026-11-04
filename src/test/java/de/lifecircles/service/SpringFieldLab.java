package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TestTrainStrategyUtils;
import java.util.*;

/**
 * Hard-wired brains (fixed attraction level, no division) in a gravity-free world: bones (packed bars) and
 * armour (rings) under external loads, with the classic actor field or the tension-spring field (FEDERFELD.md).
 */
final class SpringFieldLab {
    /** Field mode: spring off, or on with threshold and tear factor; level = commanded share of the maximum attraction. */
    record Field(String name, boolean spring, double threshold, double tear, double level, double stiffnessLimit) {
        static Field classic(double level) { return new Field("classic", false, .75, 3, level, 36); }
        /** Without the 60 Hz stiffness cap, to measure the raw stability limit. */
        Field uncapped() { return new Field(name + "-raw", spring, threshold, tear, level, 1e9); }
        static Field spring(double threshold, double tear, double level) {
            return new Field(String.format(Locale.ROOT, "spring%.0f-k%.1f", threshold * 100, tear), true, threshold, tear, level, 36);
        }
    }
    /** geometry: bar (columns × layers) or ring (layers 1–2); loading: none, tension, compression, bend. */
    record Probe(String geometry, int columns, int layers, String loading, double force, double radius, double dt) {
        Probe(String geometry, int columns, int layers, String loading, double force) { this(geometry, columns, layers, loading, force, 12, 1.0 / 60); }
    }
    record Result(Field field, Probe probe, int cells, double peakShape, double residualShape, double peakStrain,
                  double residualStrain, double minLinks, double minComponent, double finalComponent,
                  double maxOverlap, double residualSpeed) {
        static String header() {
            return "field,level,geometry,columns,layers,loading,force,radius,dt,cells,peak_shape_R,residual_shape_R,peak_strain,residual_strain,min_links,min_component,final_component,max_overlap,residual_speed\n";
        }
        String csv() {
            return String.format(Locale.ROOT, "%s,%.2f,%s,%d,%d,%s,%.1f,%.1f,%.6f,%d,%.6f,%.6f,%.6f,%.6f,%.4f,%.4f,%.4f,%.4f,%.6f%n",
                    field.name, field.level, probe.geometry, probe.columns, probe.layers, probe.loading, probe.force, probe.radius, probe.dt, cells,
                    peakShape, residualShape, peakStrain, residualStrain, minLinks, minComponent, finalComponent, maxOverlap, residualSpeed);
        }
        boolean intact() { return minComponent == 1 && finalComponent == 1; }
    }

    static final class Settings implements AutoCloseable {
        private final MechanicsLab.Settings mechanics = new MechanicsLab.Settings();
        private final SimulationConfig config = SimulationConfig.getInstance();
        private final boolean spring = config.isSpringAdhesion();
        private final double threshold = config.getSpringAdhesionThreshold(), tear = config.getSpringTearFactor(), limit = config.getSpringStiffnessLimit();
        public void close() {
            config.setSpringAdhesion(spring); config.setSpringAdhesionThreshold(threshold); config.setSpringTearFactor(tear); config.setSpringStiffnessLimit(limit);
            mechanics.close();
        }
    }

    private record Fixture(Environment world, List<Cell> a, List<Cell> b, List<Cell> middle) {}

    private static Fixture fixture(Probe p) {
        final double R = p.radius;
        Environment world = new Environment(14000, 8000);
        List<Cell> a = new ArrayList<>(), b = new ArrayList<>(), middle = new ArrayList<>();
        if (p.geometry.equals("bar")) {
            int center = p.columns / 2;
            for (int row = 0; row < p.layers; row++) for (int col = 0; col < p.columns; col++) {
                Cell cell = add(world, 7000 + 2 * R * col + (row % 2) * R, 4000 + Math.sqrt(3) * R * row, R);
                if (col == 0) a.add(cell);
                if (col == p.columns - 1) b.add(cell);
                if (col == center) middle.add(cell);
            }
        } else if (p.geometry.equals("ring")) {
            // Touching neighbours on the outer circle; a second layer sits in the gaps one packing row further in.
            int outer = p.columns;
            double outerRadius = R / Math.sin(Math.PI / outer);
            for (int layer = 0; layer < p.layers; layer++) {
                double radius = outerRadius - layer * Math.sqrt(3) * R;
                int count = layer == 0 ? outer : (int) Math.floor(Math.PI / Math.asin(R / radius));
                for (int i = 0; i < count; i++) {
                    double angle = 2 * Math.PI * (i + .5 * layer) / count;
                    Cell cell = add(world, 7000 + radius * Math.cos(angle), 4000 + radius * Math.sin(angle), R);
                    // Load groups: top and bottom 30° caps, left cap as middle (lateral disturbance).
                    double up = Math.sin(angle), side = Math.cos(angle);
                    if (up < -Math.cos(Math.PI / 6)) a.add(cell);
                    if (up > Math.cos(Math.PI / 6)) b.add(cell);
                    if (side < -Math.cos(Math.PI / 6)) middle.add(cell);
                }
            }
        } else throw new IllegalArgumentException(p.geometry);
        return new Fixture(world, a, b, middle);
    }
    private static Cell add(Environment world, double x, double y, double radius) {
        TestTrainStrategyUtils.createAndAddCell(world, x, y, radius, true, false, false, false);
        return world.getCellList().getLast();
    }
    /** Hard-wired command: every actor attracts with level × maximum attraction. */
    private static void attract(Cell cell, double level) {
        double[] outputs = cell.getBrain().process();
        for (int i = 0; i < SimulationConfig.CELL_SENSOR_ACTOR_COUNT; i++)
            outputs[GlobalOutputFeature.values().length + i * ActorOutputFeature.values().length + ActorOutputFeature.FORCE.ordinal()] = (1 - level) / 2;
        CellBrainService.think(cell);
    }
    private static Vector2D center(List<Cell> cells) {
        return new Vector2D(cells.stream().mapToDouble(c -> c.getPosition().getX()).average().orElseThrow(),
                cells.stream().mapToDouble(c -> c.getPosition().getY()).average().orElseThrow());
    }
    private static void force(List<Cell> group, Vector2D force) {
        for (Cell c : group) c.applyForce(force.multiply(1.0 / group.size()));
    }
    private static double overlap(List<Cell> cells) {
        double max = 0;
        for (int i = 0; i < cells.size(); i++) for (int j = i + 1; j < cells.size(); j++) {
            Cell x = cells.get(i), y = cells.get(j);
            double sum = x.getRadiusSize() + y.getRadiusSize();
            max = Math.max(max, 1 - x.getPosition().distance(y.getPosition()) / sum);
        }
        return max;
    }

    static Result run(Field field, Probe p) {
        final double dt = p.dt, R = p.radius;
        MechanicsLab.configure(new MechanicsLab.Parameters("balanced", 90, 125, .8, 3, .25));
        SimulationConfig config = SimulationConfig.getInstance();
        config.setSpringAdhesion(field.spring); config.setSpringAdhesionThreshold(field.threshold); config.setSpringTearFactor(field.tear); config.setSpringStiffnessLimit(field.stiffnessLimit);
        Fixture f = fixture(p);
        List<Cell> cells = f.world.getCellList();
        for (Cell c : cells) attract(c, field.level);
        int count = cells.size();
        SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(14000, 8000, 80);
        // Run-in 4 s, load 2 s, release 6 s.
        int start = (int) Math.round(4 / dt), end = (int) Math.round(6 / dt), steps = (int) Math.round(12 / dt);
        double[][] reference = null;
        List<int[]> edges = new ArrayList<>();
        double baseSpan = 0, peakShape = 0, peakStrain = 0, minLinks = 1, minComponent = 1, maxOverlap = 0, speed = 0;
        int speedSamples = 0;
        for (int step = 0; step < steps; step++) {
            double time = step * dt;
            if (step == start) {
                reference = MechanicsLab.positions(cells);
                baseSpan = center(f.a).distance(center(f.b));
                for (int i = 0; i < count; i++) for (int j = i + 1; j < count; j++)
                    if (MechanicsLab.connected(cells.get(i), cells.get(j))) edges.add(new int[]{i, j});
            }
            for (Cell c : cells) c.applyForce(SimulationConfig.GRAVITY_VECTOR.multiply(-1));
            if (step >= start && step < end && !p.loading.equals("none")) {
                Vector2D axis = center(f.b).subtract(center(f.a));
                Vector2D unit = axis.multiply(1 / Math.max(axis.length(), 1e-12));
                Vector2D transverse = new Vector2D(-unit.getY(), unit.getX());
                switch (p.loading) {
                    case "tension" -> { force(f.b, unit.multiply(p.force)); force(f.a, unit.multiply(-p.force)); }
                    case "compression" -> {
                        force(f.b, unit.multiply(-p.force)); force(f.a, unit.multiply(p.force));
                        // 1 % lateral disturbance against artificial stability from exact symmetry.
                        force(f.middle, transverse.multiply(.01 * p.force));
                        force(f.a, transverse.multiply(-.005 * p.force)); force(f.b, transverse.multiply(-.005 * p.force));
                    }
                    case "bend" -> {
                        force(f.middle, transverse.multiply(p.force));
                        force(f.a, transverse.multiply(-p.force / 2)); force(f.b, transverse.multiply(-p.force / 2));
                    }
                    default -> throw new IllegalArgumentException(p.loading);
                }
            }
            grid.build(cells); f.world.update(dt, grid);
            if (cells.size() != count) throw new AssertionError("Population changed in spring probe");
            for (Cell c : cells) if (!Double.isFinite(c.getPosition().getX()) || !Double.isFinite(c.getPosition().getY())
                    || c.getPosition().getX() < 100 || c.getPosition().getX() > 13900
                    || c.getPosition().getY() < 100 || c.getPosition().getY() > 7900)
                throw new AssertionError("Probe left the measurement region: " + field + " " + p + " time=" + time);
            if (step < start) continue;
            peakShape = Math.max(peakShape, MechanicsLab.shapeError(reference, cells, R));
            peakStrain = Math.max(peakStrain, Math.abs(center(f.a).distance(center(f.b)) / baseSpan - 1));
            double retained = edges.stream().filter(e -> MechanicsLab.connected(cells.get(e[0]), cells.get(e[1]))).count() / (double) edges.size();
            minLinks = Math.min(minLinks, retained);
            minComponent = Math.min(minComponent, MechanicsLab.largestComponent(cells) / (double) count);
            maxOverlap = Math.max(maxOverlap, overlap(cells));
            if (time >= 11) {
                Vector2D mean = new Vector2D(cells.stream().mapToDouble(c -> c.getVelocity().getX()).average().orElseThrow(),
                        cells.stream().mapToDouble(c -> c.getVelocity().getY()).average().orElseThrow());
                speed += Math.sqrt(cells.stream().mapToDouble(c -> Math.pow(c.getVelocity().subtract(mean).length(), 2)).average().orElseThrow());
                speedSamples++;
            }
        }
        return new Result(field, p, count, peakShape, MechanicsLab.shapeError(reference, cells, R), peakStrain,
                center(f.a).distance(center(f.b)) / baseSpan - 1, minLinks, minComponent,
                MechanicsLab.largestComponent(cells) / (double) count, maxOverlap, speed / speedSamples);
    }
}
