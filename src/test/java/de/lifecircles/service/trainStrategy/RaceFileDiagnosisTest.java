package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.service.FileService;
import de.lifecircles.service.SensorActorForceCellCalcService;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.service.SimulationSettingsFile;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Opt-in diagnosis of a saved RACE population (RENNBAHN.md, Diagnose): loads the cells file with its settings into
 * the race, runs one episode and reports per lane whether the cells try to hold the ground (attracting foot actors,
 * their colour affinity to the grey ground, anchors), whether they lie on it, how much they pulse, and how far they
 * get. With -Drace.diagnosis.stickiness=x the terrain stickiness is replaced for comparison.
 *
 * mvn test -Dtest=RaceFileDiagnosisTest -Drace.diagnosis.file=...cells
 */
class RaceFileDiagnosisTest {
    static final double DT = 1.0 / 60;

    /** Per-lane sums over the run phase, sampled every step. */
    static final class Stats {
        int cells, samples; double score, groups, stacked; long ready, readyDown, readyBlocked, readySmall, cellSteps, footSteps, footAttracting, footInReach, onGround, anchors;
        double maxDesire, ownPull, stickinessPull, affinity, minRadius = Double.MAX_VALUE, maxRadius;
        final List<Double> progress = new ArrayList<>();
    }

    @Test
    @EnabledIfSystemProperty(named = "race.diagnosis.file", matches = ".+")
    void diagnose() throws Exception {
        String file = System.getProperty("race.diagnosis.file");
        SimulationConfig config = SimulationConfig.getInstance();
        var loaded = SimulationSettingsFile.load(file, config);
        System.out.println("settings problems: " + loaded.problems());
        String stickiness = System.getProperty("race.diagnosis.stickiness");
        if (stickiness != null) config.setGroundStickiness(Double.parseDouble(stickiness));
        List<Cell> cells = FileService.getInstance().loadCellsFromFile(file);
        var strategy = new RaceTrainStrategy(new Random(Long.getLong("race.diagnosis.seed", 1)));
        Environment world = strategy.initializeEnvironment();
        strategy.onPopulationImported(world, cells, false);
        double episode = config.trainingDuration(RaceTrainStrategy.EPISODE_SECONDS, RaceTrainStrategy.MIN_EPISODE_SECONDS);
        System.out.printf(Locale.ROOT, "file %s: %d cells, episode %.0f s, gravity %.2f, stickiness %.2f, friction %s %.2f/%.2f, "
                        + "anchors %s, max attraction %.0f, sharpness %.1f%n", file, cells.size(), episode, config.getGravity(),
                config.getGroundStickiness(), config.isBlockerFriction(), config.getBlockerFrictionCoefficient(),
                config.getBlockerRollingResistance(), config.isGroundAnchors(), config.getCellActorMaxAttractiveForceStrength(),
                config.getAdhesionSharpness());

        Blocker ground = world.getBlockerList().getFirst();
        double floor = world.getHeight() - Environment.GroundBlockerHeight;
        var field = new SensorActorForceCellCalcService.Ground(config.getGroundStickiness(), config.getAdhesionSharpness());
        Map<Integer, Stats> lanes = new TreeMap<>();
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 100);
        double time = 0;
        for (int step = 0; step < Math.round(episode / DT); step++) {
            strategy.beforeStep(world, DT);
            grid.build(world.getCellList());
            world.update(DT, grid, strategy.stepPolicy());
            strategy.selectAndMutate(world, DT);
            time += DT;
            if (time < RaceTrainStrategy.GROWTH_SECONDS) {
                for (RaceTrainStrategy.Body body : strategy.bodies()) {
                    Stats stats = lanes.computeIfAbsent(body.lane, lane -> new Stats());
                    for (Cell cell : body.cells) {
                        stats.maxDesire = Math.max(stats.maxDesire, cell.getMaxReproductionDesire());
                        if (!de.lifecircles.service.ReproductionManagerService.canReproduce(config, cell)) continue;
                        // Ready to divide: does the most eager actor point into the ground (no free place for the child)?
                        stats.ready++;
                        SensorActor eager = cell.getSensorActors().stream()
                                .max(Comparator.comparingDouble(SensorActor::getReproductionDesire)).orElseThrow();
                        if (eager.calcPosition().getY() > cell.getPosition().getY() + .5 * cell.getRadiusSize()) stats.readyDown++;
                        if (cell.getRadiusSize() < 5) stats.readySmall++;
                        // The child's place as ReproductionManagerService computes it: blocked by the ground?
                        Vector2D direction = eager.calcPosition().subtract(cell.getPosition()).normalize();
                        Vector2D child = cell.getPosition().add(direction.multiply(cell.getRadiusSize() / Math.sqrt(2)
                                + config.getCellMinGrowRadiusSize() * 2));
                        if (!de.lifecircles.service.ReproductionManagerService.isFreePosition(world, child, config.getCellMinGrowRadiusSize()))
                            stats.readyBlocked++;
                    }
                }
            }
            if (time < RaceTrainStrategy.RUN_START_SECONDS) continue;
            boolean sample = step % 30 == 0;
            for (RaceTrainStrategy.Body body : strategy.bodies()) {
                Stats stats = lanes.computeIfAbsent(body.lane, lane -> new Stats());
                if (sample) {
                    stats.progress.add(body.progress);
                    stats.samples++;
                    stats.groups += groups(body.cells, world.getWidth(), world.getHeight());
                    stats.stacked += stacked(body.cells, floor, world.getWidth(), world.getHeight());
                }
                if (time < episode - DT / 2) { stats.cells = Math.max(stats.cells, body.cells.size()); stats.score = body.score(); }
                for (Cell cell : body.cells) {
                    stats.cellSteps++;
                    double r = cell.getRadiusSize();
                    stats.minRadius = Math.min(stats.minRadius, r); stats.maxRadius = Math.max(stats.maxRadius, r);
                    if (cell.getPosition().getY() + r >= floor - 1) stats.onGround++;
                    double range = SensorActorForceCellCalcService.calcSensorRadius(r, cell.getSensorActors().size());
                    for (SensorActor actor : cell.getSensorActors()) {
                        if (actor.getAnchor() != null) stats.anchors++;
                        // Foot: the lower third of the rim, facing the ground.
                        if (actor.getCachedPosition().getY() - cell.getPosition().getY() < .5 * r) continue;
                        stats.footSteps++;
                        double a = field.affinity(actor.getType(), ground);
                        stats.affinity += a;
                        double own = Math.min(0, actor.getForceStrength()) * a;
                        if (actor.getForceStrength() < 0) stats.footAttracting++;
                        if (floor - actor.getCachedPosition().getY() < range) {
                            stats.footInReach++;
                            stats.ownPull += own;
                            stats.stickinessPull += config.getGroundStickiness();
                        }
                    }
                }
            }
        }
        System.out.println("lane cells groups stacked onGround% foot:attracting% inReach% affinity ownPull stickiness anchors/cell radius desire ready/down/blocked/r<5 progress(every 5 s) score");
        for (var entry : lanes.entrySet()) {
            Stats s = entry.getValue();
            StringBuilder progress = new StringBuilder();
            for (int i = 0; i < s.progress.size(); i += 10) progress.append(String.format(Locale.ROOT, " %.1f", s.progress.get(i)));
            System.out.printf(Locale.ROOT, "%2d %d %.1f %.1f %3.0f%% %3.0f%% %3.0f%% %.2f %6.1f %6.1f %.2f %.1f-%.1f %.2f %d/%d/%d/%d%s | %.1f%n",
                    entry.getKey(), s.cells, s.groups / Math.max(1, s.samples), s.stacked / Math.max(1, s.samples), 100.0 * s.onGround / s.cellSteps,
                    100.0 * s.footAttracting / Math.max(1, s.footSteps), 100.0 * s.footInReach / Math.max(1, s.footSteps),
                    s.affinity / Math.max(1, s.footSteps), s.ownPull / Math.max(1, s.footInReach),
                    s.stickinessPull / Math.max(1, s.footInReach), (double) s.anchors / s.cellSteps,
                    s.minRadius, s.maxRadius, s.maxDesire, s.ready, s.readyDown, s.readyBlocked, s.readySmall, progress, s.score);
        }
    }

    /** Cells closer than 3 px rim to rim count as touching. */
    static boolean touching(Cell a, Cell b, double width, double height) {
        return a.getPosition().getShortestDirectionWithWrapAround(b.getPosition(), width, height).length()
                < a.getRadiusSize() + b.getRadiusSize() + 3;
    }

    /** Number of touching groups within a body. */
    static int groups(List<Cell> cells, double width, double height) {
        int[] group = new int[cells.size()];
        Arrays.fill(group, -1);
        int count = 0;
        for (int i = 0; i < cells.size(); i++) {
            if (group[i] >= 0) continue;
            Deque<Integer> open = new ArrayDeque<>(List.of(i));
            group[i] = count;
            while (!open.isEmpty()) {
                int c = open.pop();
                for (int j = 0; j < cells.size(); j++)
                    if (group[j] < 0 && touching(cells.get(c), cells.get(j), width, height)) { group[j] = count; open.push(j); }
            }
            count++;
        }
        return count;
    }

    /** Cells off the ground resting on a lower cell of the body (centre at least half a radius higher). */
    static int stacked(List<Cell> cells, double floor, double width, double height) {
        int count = 0;
        for (Cell cell : cells) {
            if (cell.getPosition().getY() + cell.getRadiusSize() >= floor - 1) continue;
            for (Cell below : cells)
                if (below != cell && below.getPosition().getY() > cell.getPosition().getY() + .5 * cell.getRadiusSize()
                        && touching(cell, below, width, height)) { count++; break; }
        }
        return count;
    }
}
