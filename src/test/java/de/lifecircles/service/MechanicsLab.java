package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.*;

import java.util.*;

/** Fixed brains and external test loads: a mechanical assay, not an evolution experiment. */
final class MechanicsLab {
    record Parameters(String name, double attraction, double contact, double viscosity, double rotationDamping, double softening) {}
    record Result(Parameters parameters, String scenario, double dt, double shapeError, double heightRatio,
                  double minLinks, double finalLinks, double component, double peakBend, double residualBend,
                  double lateSpeed, double lateOmega, double overlap, double mobility, double stroke, double strokeResidual) {
        static String header() {
            return "profile,attraction,contact,viscosity,rotation_damping,softening,scenario,dt,shape_error_R,height_ratio,min_links,final_links,component,peak_bend_R,residual_bend_R,late_relative_speed,late_omega,overlap_fraction,mobility,stroke_R,stroke_residual_R\n";
        }
        String csv() {
            return String.format(Locale.ROOT, "%s,%.9f,%.9f,%.9f,%.9f,%.9f,%s,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f%n",
                    parameters.name, parameters.attraction, parameters.contact, parameters.viscosity, parameters.rotationDamping,
                    parameters.softening, scenario, dt, shapeError, heightRatio, minLinks, finalLinks, component, peakBend, residualBend,
                    lateSpeed, lateOmega, overlap, mobility, stroke, strokeResidual);
        }
    }

    static final class Settings implements AutoCloseable {
        private final SimulationConfig config = SimulationConfig.getInstance();
        private final double attraction = config.getCellActorMaxAttractiveForceStrength(), contact = config.getCellRepulsionStrength(),
                viscosity = config.getViscosity(), damping = config.getRotationalFriction(), sunlight = config.getSunRayRate(), softening = config.getActorFieldSofteningFraction();
        private final int count = config.getInitialCellCount();
        Settings() { config.setSunRayRate(0); config.setInitialCellCount(0); }
        public void close() {
            config.setCellActorMaxAttractiveForceStrength(attraction); config.setCellRepulsionStrength(contact);
            config.setViscosity(viscosity); config.setRotationalFriction(damping);
            config.setSunRayRate(sunlight); config.setInitialCellCount(count);
            config.setActorFieldSofteningFraction(softening);
        }
    }

    static void configure(Parameters p) {
        SimulationConfig c = SimulationConfig.getInstance();
        c.setCellActorMaxAttractiveForceStrength(p.attraction); c.setCellRepulsionStrength(p.contact);
        c.setViscosity(p.viscosity); c.setRotationalFriction(p.rotationDamping);
        c.setActorFieldSofteningFraction(p.softening);
    }

    static Environment fixture(String scenario) {
        Environment world = new Environment(1400, 800);
        if (scenario.startsWith("snowman")) {
            // These are exactly the first 15 body cells and optional 6 falling cells of TEST_A.
            new TestATrainStrategy().initialize(world);
            List<Cell> group = new ArrayList<>(world.getCellList().subList(0, scenario.equals("snowman-drop") ? 21 : 15));
            world.resetCells(group);
        } else if (scenario.equals("sizes")) {
            world.addGroundBlocker();
            add(world, 500, 730, 20); add(world, 500, 694, 16); add(world, 500, 666, 12);
        } else {
            int length = scenario.equals("actuator") ? 2 : 5;
            for (int i = 0; i < length; i++) add(world, 500 + i * 24, 350, 12);
            if (scenario.equals("strip")) {
                for (int i = 0; i < 4; i++) add(world, 512 + i * 24, 350 - Math.sqrt(3) * 12, 12);
            }
        }
        return world;
    }
    private static void add(Environment world, double x, double y, double radius) {
        TestTrainStrategyUtils.createAndAddCell(world, x, y, radius, true, false, false, false);
    }

    static double[][] positions(List<Cell> cells) {
        double[][] result = new double[cells.size()][2];
        for (int i = 0; i < cells.size(); i++) { result[i][0] = cells.get(i).getPosition().getX(); result[i][1] = cells.get(i).getPosition().getY(); }
        return result;
    }
    static double centerX(List<Cell> cells) { return cells.stream().mapToDouble(c -> c.getPosition().getX()).average().orElseThrow(); }
    static double height(List<Cell> cells) {
        return cells.stream().mapToDouble(c -> c.getPosition().getY() + c.getRadiusSize()).max().orElseThrow()
                - cells.stream().mapToDouble(c -> c.getPosition().getY() - c.getRadiusSize()).min().orElseThrow();
    }
    static double shapeError(double[][] reference, List<Cell> cells, double radius) {
        // Best 2-D rigid alignment: translation and rotation are not counted as deformation.
        double rx = 0, ry = 0, px = 0, py = 0;
        for (int i = 0; i < cells.size(); i++) { rx += reference[i][0]; ry += reference[i][1]; px += cells.get(i).getPosition().getX(); py += cells.get(i).getPosition().getY(); }
        rx /= cells.size(); ry /= cells.size(); px /= cells.size(); py /= cells.size();
        double dot = 0, cross = 0, sum = 0;
        for (int i = 0; i < cells.size(); i++) {
            double x = reference[i][0] - rx, y = reference[i][1] - ry;
            double u = cells.get(i).getPosition().getX() - px, v = cells.get(i).getPosition().getY() - py;
            dot += x * u + y * v; cross += x * v - y * u;
            sum += x * x + y * y + u * u + v * v;
        }
        return Math.sqrt(Math.max(0, (sum - 2 * Math.hypot(dot, cross)) / cells.size())) / radius;
    }
    static double bend(List<Cell> cells) {
        Vector2D a = cells.get(0).getPosition(), b = cells.get(4).getPosition(), middle = cells.get(2).getPosition();
        Vector2D axis = b.subtract(a);
        return (axis.getX() * (middle.getY() - a.getY()) - axis.getY() * (middle.getX() - a.getX()))
                / Math.max(1e-12, axis.length()) / 12;
    }
    static boolean connected(Cell a, Cell b) {
        double range = Math.max(SensorActorForceCellCalcService.calcSensorRadius(a.getRadiusSize(), a.getSensorActors().size()),
                SensorActorForceCellCalcService.calcSensorRadius(b.getRadiusSize(), b.getSensorActors().size()));
        return a.getPosition().distance(b.getPosition()) <= a.getRadiusSize() + b.getRadiusSize() + range;
    }
    static int largestComponent(List<Cell> cells) {
        boolean[] seen = new boolean[cells.size()]; int largest = 0;
        for (int i = 0; i < cells.size(); i++) {
            if (seen[i]) continue;
            ArrayDeque<Integer> queue = new ArrayDeque<>(); queue.add(i); seen[i] = true; int size = 0;
            while (!queue.isEmpty()) {
                int a = queue.remove(); size++;
                for (int b = 0; b < cells.size(); b++) if (!seen[b] && connected(cells.get(a), cells.get(b))) { seen[b] = true; queue.add(b); }
            }
            largest = Math.max(largest, size);
        }
        return largest;
    }

    static Result run(Parameters p, String scenario, double dt, StringBuilder trajectory) {
        configure(p);
        Environment world = fixture(scenario);
        List<Cell> body = new ArrayList<>(world.getCellList().subList(0, scenario.startsWith("snowman") ? 15 : world.getCellList().size()));
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < body.size(); i++) for (int j = i + 1; j < body.size(); j++) if (connected(body.get(i), body.get(j))) edges.add(new int[]{i, j});
        double[][] reference = positions(body);
        double originalHeight = height(body), radius = scenario.startsWith("snowman") || scenario.equals("sizes") ? 20 : 12;
        SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(1400, 800, 80);
        double minLinks = 1, finalLinks = 1, maxOverlap = 0, peakBend = 0, baseBend = 0, residualBend = 0,
                lateSpeed = 0, lateOmega = 0, mobilityStart = 0, strokeBase = 0, stroke = 0, strokeResidual = 0;
        int lateSamples = 0, frameStride = Math.max(1, (int) Math.round(0.1 / dt));
        boolean chain = scenario.equals("chain") || scenario.equals("strip");
        double duration = scenario.startsWith("snowman") ? 20 : 8;
        int steps = (int) Math.round(duration / dt);
        for (int step = 0; step <= steps; step++) {
            double time = step * dt;
            if (step == (int) Math.round(3 / dt)) {
                if (chain) baseBend = bend(body);
                if (scenario.equals("actuator")) strokeBase = body.get(0).getPosition().distance(body.get(1).getPosition());
            }
            if (step == (int) Math.round((duration - 1) / dt)) {
                mobilityStart = centerX(body);
                for (Cell c : body) c.setVelocity(c.getVelocity().add(new Vector2D(10, 0)));
            }
            if (trajectory != null && step % frameStride == 0) {
                for (int i = 0; i < world.getCellList().size(); i++) {
                    Cell c = world.getCellList().get(i);
                    trajectory.append(String.format(Locale.ROOT, "%s,%s,%.9f,%.5f,%d,%.9f,%.9f,%.9f%n", p.name, scenario, dt, time, i,
                            c.getPosition().getX(), c.getPosition().getY(), c.getRadiusSize()));
                }
            }
            if (step == steps) break;
            for (Cell c : world.getCellList()) {
                if (!scenario.startsWith("snowman") && !scenario.equals("sizes")) c.applyForce(SimulationConfig.GRAVITY_VECTOR.multiply(-1));
                if (scenario.equals("actuator")) {
                    double[] outputs = c.getBrain().process();
                    for (int i = 0; i < 12; i++) outputs[GlobalOutputFeature.values().length + i * ActorOutputFeature.values().length
                            + ActorOutputFeature.FORCE.ordinal()] = time >= 3 && time < 4 ? 0.325 : 0;
                    CellBrainService.think(c);
                }
            }
            if (chain && time >= 3 && time < 4) {
                body.get(2).applyForce(new Vector2D(0, 6));
                body.get(0).applyForce(new Vector2D(0, -3)); body.get(4).applyForce(new Vector2D(0, -3));
            }
            grid.build(world.getCellList()); world.update(dt, grid);
            if (world.getCellList().size() != (scenario.equals("snowman-drop") ? 21 : body.size())) throw new AssertionError("Unexpected biological population change");
            finalLinks = edges.isEmpty() ? 1 : edges.stream().filter(e -> connected(body.get(e[0]), body.get(e[1]))).count() / (double) edges.size();
            minLinks = Math.min(minLinks, finalLinks);
            for (int i = 0; i < body.size(); i++) {
                Cell a = body.get(i);
                if (!Double.isFinite(a.getPosition().getX()) || !Double.isFinite(a.getPosition().getY())) throw new AssertionError("Non-finite position");
                for (int j = i + 1; j < body.size(); j++) {
                    Cell b = body.get(j); double sumRadius = a.getRadiusSize() + b.getRadiusSize();
                    maxOverlap = Math.max(maxOverlap, Math.max(0, 1 - a.getPosition().distance(b.getPosition()) / sumRadius));
                }
            }
            if (chain && time >= 3 && time < 7) peakBend = Math.max(peakBend, Math.abs(bend(body) - baseBend));
            if (scenario.equals("actuator") && time >= 3 && time < 4) stroke = Math.max(stroke,
                    Math.abs(body.get(0).getPosition().distance(body.get(1).getPosition()) - strokeBase) / 12);
            if (time >= duration - 2 && time < duration - 1) {
                double vx = body.stream().mapToDouble(c -> c.getVelocity().getX()).average().orElseThrow();
                double vy = body.stream().mapToDouble(c -> c.getVelocity().getY()).average().orElseThrow();
                lateSpeed += Math.sqrt(body.stream().mapToDouble(c -> Math.pow(c.getVelocity().getX() - vx, 2) + Math.pow(c.getVelocity().getY() - vy, 2)).average().orElseThrow());
                lateOmega += Math.sqrt(body.stream().mapToDouble(c -> c.getAngularVelocity() * c.getAngularVelocity()).average().orElseThrow());
                lateSamples++;
                if (chain) residualBend = Math.abs(bend(body) - baseBend);
                if (scenario.equals("actuator")) strokeResidual = Math.abs(
                        body.get(0).getPosition().distance(body.get(1).getPosition()) - strokeBase) / 12;
            }
        }
        return new Result(p, scenario, dt, shapeError(reference, body, radius), height(body) / originalHeight,
                minLinks, finalLinks, largestComponent(body) / (double) body.size(), peakBend, residualBend,
                lateSpeed / lateSamples, lateOmega / lateSamples, maxOverlap, centerX(body) - mobilityStart, stroke, strokeResidual);
    }
}
