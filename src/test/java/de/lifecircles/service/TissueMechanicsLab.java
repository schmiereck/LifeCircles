package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.GlobalOutputFeature;
import de.lifecircles.model.neural.CellBrainService;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TestTrainStrategyUtils;
import java.util.*;

/** Material probes use real world updates, no bonds, position clamps or auxiliary springs. */
final class TissueMechanicsLab {
    static final double R = 12;
    record Probe(String geometry, int columns, int layers, String loading, double force, double dt) {}
    record Result(Probe probe, int cells, double peakShape, double residualShape, double peakBend,
                  double peakStrain, double minLinks, double minComponent, double residualSpeed,
                  double leftShape, double rightShape, double stroke1, double stroke2, double stroke3,
                  double residualStrain, double peakWork) {
        static String header() {
            return "geometry,columns,layers,loading,force,dt,cells,peak_shape_R,residual_shape_R,peak_bend_R,peak_strain,min_links,min_component,residual_speed,left_shape_R,right_shape_R,stroke1,stroke2,stroke3,residual_strain,peak_work\n";
        }
        String csv() {
            return String.format(Locale.ROOT,
                    "%s,%d,%d,%s,%.9f,%.9f,%d,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f%n",
                    probe.geometry, probe.columns, probe.layers, probe.loading, probe.force, probe.dt, cells,
                    peakShape, residualShape, peakBend, peakStrain, minLinks, minComponent, residualSpeed,
                    leftShape, rightShape, stroke1, stroke2, stroke3, residualStrain, peakWork);
        }
    }
    private record Fixture(Environment world, List<Cell> left, List<Cell> middle, List<Cell> right,
                           List<Cell> leftLobe, List<Cell> rightLobe) {}

    private static Fixture fixture(Probe p) {
        if (p.columns < 5 || p.columns % 2 == 0 || p.layers < 1 || p.layers > 3
                || !Set.of("bar", "neck").contains(p.geometry)
                || !Set.of("bend", "tension", "compression", "active").contains(p.loading)
                || !Double.isFinite(p.force) || p.force < 0 || !(p.dt > 0) || !Double.isFinite(p.dt))
            throw new IllegalArgumentException("Invalid material probe");
        Environment world = new Environment(14000, 8000);
        List<Cell> left = new ArrayList<>(), middle = new ArrayList<>(), right = new ArrayList<>(),
                leftLobe = new ArrayList<>(), rightLobe = new ArrayList<>();
        int center = p.columns / 2;
        for (int row = 0; row < p.layers; row++) for (int col = 0; col < p.columns; col++) {
            if (p.geometry.equals("neck") && row != p.layers / 2 && Math.abs(col - center) <= 1) continue;
            TestTrainStrategyUtils.createAndAddCell(world, 7000 + 2 * R * col + (row % 2) * R,
                    4000 + Math.sqrt(3) * R * row, R, true, false, false, false);
            Cell cell = world.getCellList().getLast();
            if (col == 0) left.add(cell);
            if (col == center) middle.add(cell);
            if (col == p.columns - 1) right.add(cell);
            if (col < center - 1) leftLobe.add(cell);
            if (col > center + 1) rightLobe.add(cell);
        }
        return new Fixture(world, left, middle, right, leftLobe, rightLobe);
    }
    private static Vector2D center(List<Cell> cells) {
        return new Vector2D(cells.stream().mapToDouble(c -> c.getPosition().getX()).average().orElseThrow(),
                cells.stream().mapToDouble(c -> c.getPosition().getY()).average().orElseThrow());
    }
    private static double span(Fixture f) { return center(f.left).distance(center(f.right)); }
    private static double bend(Fixture f) {
        Vector2D a = center(f.left), axis = center(f.right).subtract(a), m = center(f.middle).subtract(a);
        return (axis.getX() * m.getY() - axis.getY() * m.getX()) / Math.max(axis.length(), 1e-12) / R;
    }
    private static void force(List<Cell> group, Vector2D force) {
        for (Cell c : group) c.applyForce(force.multiply(1.0 / group.size()));
    }
    private static void bendLoad(Fixture f, double magnitude) {
        Vector2D axis = center(f.right).subtract(center(f.left));
        double length = Math.max(axis.length(), 1e-12);
        Vector2D middle = center(f.middle).subtract(center(f.left));
        double fraction = (middle.getX() * axis.getX() + middle.getY() * axis.getY()) / (length * length);
        Vector2D transverse = new Vector2D(-axis.getY(), axis.getX()).multiply(magnitude / length);
        force(f.middle, transverse);
        force(f.left, transverse.multiply(fraction - 1));
        force(f.right, transverse.multiply(-fraction));
    }

    static Result run(Probe p) {
        MechanicsLab.configure(new MechanicsLab.Parameters("balanced", 90, 125, .8, 3, .25));
        Fixture f = fixture(p);
        List<Cell> cells = f.world.getCellList();
        int count = cells.size();
        SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(14000, 8000, 80);
        boolean active = p.loading.equals("active");
        double endLoad = active ? 13 : 6, duration = endLoad + 6;
        int start = (int) Math.round(4 / p.dt), end = (int) Math.round(endLoad / p.dt);
        int steps = (int) Math.round(duration / p.dt);
        double[][] reference = null, leftReference = null, rightReference = null;
        List<int[]> edges = new ArrayList<>();
        double baseSpan = 0, baseBend = 0, peakShape = 0, peakBend = 0, peakStrain = 0,
                minLinks = 1, minComponent = 1, speed = 0, leftShape = 0, rightShape = 0, peakWork = 0;
        double[] minimum = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] maximum = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        int speedSamples = 0;
        for (int step = 0; step < steps; step++) {
            double time = step * p.dt;
            if (step == start) {
                reference = MechanicsLab.positions(cells);
                leftReference = MechanicsLab.positions(f.leftLobe); rightReference = MechanicsLab.positions(f.rightLobe);
                baseSpan = span(f); baseBend = bend(f);
                for (int a = 0; a < count; a++) for (int b = a + 1; b < count; b++)
                    if (MechanicsLab.connected(cells.get(a), cells.get(b))) edges.add(new int[]{a, b});
            }
            boolean loaded = step >= start && step < end;
            for (Cell c : cells) {
                c.applyForce(SimulationConfig.GRAVITY_VECTOR.multiply(-1));
                if (active) {
                    // Smooth prescribed brain output; radius stays within existing limits.
                    double radius = R + (loaded ? 2 * Math.pow(Math.sin(Math.PI * (time - 4) / 3), 2) : 0);
                    SimulationConfig config = SimulationConfig.getInstance();
                    c.getBrain().process()[GlobalOutputFeature.SIZE.ordinal()] =
                            (radius - config.getCellMinRadiusSize()) / (config.getCellMaxRadiusSize() - config.getCellMinRadiusSize());
                    CellBrainService.think(c);
                }
            }
            if (loaded) {
                if (p.loading.equals("bend")) {
                    bendLoad(f, p.force);
                } else {
                    double sign = p.loading.equals("tension") ? 1 : -1;
                    // Follow the current end-to-end axis: equal opposite forces introduce no net torque.
                    Vector2D axis = center(f.right).subtract(center(f.left));
                    Vector2D applied = axis.multiply(sign * p.force / Math.max(axis.length(), 1e-12));
                    force(f.right, applied); force(f.left, applied.multiply(-1));
                    if (!p.loading.equals("tension")) {
                        // A 1% transverse disturbance prevents artificial stability from exact symmetry.
                        bendLoad(f, .01 * p.force);
                    }
                }
            }
            grid.build(cells); f.world.update(p.dt, grid);
            if (f.world.getCellList().size() != count) throw new AssertionError("Population changed in mechanical probe");
            for (Cell c : cells) {
                if (!Double.isFinite(c.getPosition().getX()) || !Double.isFinite(c.getPosition().getY())
                        || !Double.isFinite(c.getVelocity().length()) || !Double.isFinite(c.getAngularVelocity()))
                    throw new AssertionError("Non-finite mechanical state");
                // The assay deliberately stays away from periodic boundaries.
                if (c.getPosition().getX() < 100 || c.getPosition().getX() > 13900
                        || c.getPosition().getY() < 100 || c.getPosition().getY() > 7900)
                    throw new AssertionError("Probe left the measurement region: " + p + " time=" + time);
            }
            if (step < start) continue;
            double strain = (span(f) - baseSpan) / baseSpan;
            peakShape = Math.max(peakShape, MechanicsLab.shapeError(reference, cells, R));
            peakBend = Math.max(peakBend, Math.abs(bend(f) - baseBend));
            peakStrain = Math.max(peakStrain, Math.abs(strain));
            leftShape = Math.max(leftShape, MechanicsLab.shapeError(leftReference, f.leftLobe, R));
            rightShape = Math.max(rightShape, MechanicsLab.shapeError(rightReference, f.rightLobe, R));
            double retained = edges.stream().filter(e -> MechanicsLab.connected(cells.get(e[0]), cells.get(e[1]))).count() / (double) edges.size();
            minLinks = Math.min(minLinks, retained);
            minComponent = Math.min(minComponent, MechanicsLab.largestComponent(cells) / (double) count);
            if (active && loaded) {
                int cycle = Math.min(2, (int) ((time - 4) / 3));
                minimum[cycle] = Math.min(minimum[cycle], strain); maximum[cycle] = Math.max(maximum[cycle], strain);
                peakWork = Math.max(peakWork, p.force * (span(f) - baseSpan));
            }
            if (time >= duration - 1) {
                Vector2D mean = new Vector2D(cells.stream().mapToDouble(c -> c.getVelocity().getX()).average().orElseThrow(),
                        cells.stream().mapToDouble(c -> c.getVelocity().getY()).average().orElseThrow());
                speed += Math.sqrt(cells.stream().mapToDouble(c -> Math.pow(c.getVelocity().subtract(mean).length(), 2)).average().orElseThrow());
                speedSamples++;
            }
        }
        return new Result(p, count, peakShape, MechanicsLab.shapeError(reference, cells, R), peakBend,
                peakStrain, minLinks, minComponent, speed / speedSamples, leftShape, rightShape,
                active ? maximum[0] - minimum[0] : 0, active ? maximum[1] - minimum[1] : 0,
                active ? maximum[2] - minimum[2] : 0, (span(f) - baseSpan) / baseSpan, peakWork);
    }
}
