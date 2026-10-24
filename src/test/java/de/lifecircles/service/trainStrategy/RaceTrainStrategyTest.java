package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** RENNBAHN.md: lanes, cell cap, progress without pushing by numbers, selection. */
class RaceTrainStrategyTest {
    final SimulationConfig config = SimulationConfig.getInstance();
    double width, height, scale, rate, strength, age;
    boolean adhesion, ground, anchors, groundOnly, bonds, birthBonds, friction;
    Random previous;

    @BeforeEach void save() {
        width = config.getWidth(); height = config.getHeight(); scale = config.getScaleSimulation();
        rate = config.getMutationRate(); strength = config.getMutationStrength(); age = config.getReproductionAgeThreshold();
        adhesion = config.isCellAdhesion(); ground = config.isGroundAdhesion(); anchors = config.isGroundAnchors();
        groundOnly = config.isGroundAnchorsGroundOnly(); bonds = config.isAdhesionBonds(); birthBonds = config.isAdhesionBirthBonds();
        friction = config.isBlockerFriction();
        config.setMutationRate(0); config.setMutationStrength(0); config.setReproductionAgeThreshold(0);
        previous = NeuralNetwork.getRandom(); NeuralNetwork.setRandom(new Random(573));
    }
    @AfterEach void restore() {
        config.setWidth(width); config.setHeight(height); config.setScaleSimulation(scale);
        config.setMutationRate(rate); config.setMutationStrength(strength); config.setReproductionAgeThreshold(age);
        config.setCellAdhesion(adhesion); config.setGroundAdhesion(ground); config.setGroundAnchors(anchors);
        config.setGroundAnchorsGroundOnly(groundOnly); config.setAdhesionBonds(bonds); config.setAdhesionBirthBonds(birthBonds);
        config.setBlockerFriction(friction);
        NeuralNetwork.setRandom(previous);
    }

    /** Fixed outputs: neutral actors, size 20; with births every cell divides towards actor 0 (to the right). */
    static Cell genome(boolean births) {
        var network = new NeuralNetwork(new ValuesNeuronValueFunctionFactory(), CellFactory.calcInputCount(12), new int[0], CellFactory.calcOutputCount(12), 0, 0);
        var outputs = network.getNeuralNet().getOutputNeuronArr();
        for (var output : outputs) output.setBias(0, -12);
        outputs[GlobalOutputFeature.SIZE.ordinal()].setBias(0, 12);
        for (int a = 0; a < 12; a++) {
            int b = GlobalOutputFeature.values().length + a * ActorOutputFeature.values().length;
            outputs[b + ActorOutputFeature.FORCE.ordinal()].setBias(0, 0);
            if (births && a == 0) outputs[b + ActorOutputFeature.REPRODUCTION_DESIRE.ordinal()].setBias(0, 12);
            outputs[b + ActorOutputFeature.REPRODUCTION_ENERGY_SHARE.ordinal()].setBias(0, 0);
        }
        Cell cell = new Cell(new Vector2D(0, 0), 20, new CellBrain(network));
        cell.setType(new CellType(0, 1, 0));
        return cell;
    }

    RaceTrainStrategy strategy() { return new RaceTrainStrategy(new Random(98)); }

    Environment setup(RaceTrainStrategy strategy, Cell genome) {
        Environment world = strategy.initializeEnvironment();
        strategy.startEpisode(world, Collections.nCopies(RaceTrainStrategy.LANES, genome), false);
        return world;
    }

    static void run(RaceTrainStrategy strategy, Environment world, double seconds) {
        double dt = 1.0 / 60;
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 100);
        for (int i = 0; i < Math.round(seconds / dt); i++) {
            strategy.beforeStep(world, dt);
            grid.build(world.getCellList());
            world.update(dt, grid, strategy.stepPolicy());
            strategy.selectAndMutate(world, dt);
        }
    }

    @Test void oneFounderPerWalledLaneWithGripSwitchedOn() {
        var strategy = strategy();
        Environment world = setup(strategy, genome(false));
        assertSame(RaceTrainStrategy.class, CalculationService.strategyFor(TrainMode.RACE).getClass());
        assertEquals(RaceTrainStrategy.LANES * RaceTrainStrategy.LANE_WIDTH, world.getWidth());
        assertEquals(RaceTrainStrategy.LANES, world.getCellList().size());
        long walls = world.getBlockerList().stream().filter(b -> b.getBlockerType() == Blocker.BlockerType.WALL).count();
        assertEquals(RaceTrainStrategy.LANES, walls, "one wall at every lane start, the world wraps around");
        for (int lane = 0; lane < RaceTrainStrategy.LANES; lane++) {
            Cell founder = world.getCellList().get(lane);
            assertEquals(lane, (int) (founder.getPosition().getX() / RaceTrainStrategy.LANE_WIDTH));
        }
        assertTrue(config.isCellAdhesion() && config.isGroundAdhesion() && config.isGroundAnchors() && config.isGroundAnchorsGroundOnly()
                && config.isBlockerFriction());
        assertTrue(strategy.stepPolicy().useCellAdhesion());
        assertFalse(strategy.stepPolicy().useSunlight());
    }

    @Test void bodiesDivideUpToTheCapAndPushingByNumbersIsNoProgress() {
        var strategy = strategy();
        Environment world = setup(strategy, genome(true));
        double startFront = world.getCellList().getFirst().getPosition().getX();
        run(strategy, world, 20);
        for (RaceTrainStrategy.Body body : strategy.bodies()) assertEquals(RaceTrainStrategy.CELL_LIMIT, body.cells.size());
        assertEquals(RaceTrainStrategy.LANES * RaceTrainStrategy.CELL_LIMIT, world.getCellList().size());
        RaceTrainStrategy.Body first = strategy.bodies().getFirst();
        double front = first.cells.stream().mapToDouble(c -> c.getPosition().getX()).max().orElseThrow();
        System.out.printf(Locale.ROOT, "births ahead: front +%.1f px, progress %.3f px%n", front - startFront, first.progress);
        assertTrue(front - startFront > 60, "children placed ahead move the front");
        assertEquals(0, first.progress, 0, "growth phase: nothing counts");
        Cell any = first.cells.getFirst();
        run(strategy, world, RaceTrainStrategy.GROWTH_SECONDS - 20 + .1);
        assertFalse(strategy.allowReproduction(any), "no births after the growth phase");
        run(strategy, world, RaceTrainStrategy.RUN_START_SECONDS - RaceTrainStrategy.GROWTH_SECONDS);
        double before = first.progress;
        run(strategy, world, 10);
        System.out.printf(Locale.ROOT, "afterwards 10 s: progress %.3f px%n", first.progress - before);
        assertEquals(0, first.progress - before, .05, "once the births have settled the body does not move itself");
    }

    @Test void birthsPushOffWithFrictionWhichIsWhyTheGrowthPhaseDoesNotCount() {
        var strategy = strategy();
        Environment world = setup(strategy, genome(true));
        double dt = 1.0 / 60;
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 100);
        for (int i = 0; i < Math.round(20 / dt); i++) {
            strategy.beforeStep(world, dt);
            grid.build(world.getCellList());
            world.update(dt, grid, strategy.stepPolicy());
            strategy.track(world, true);
        }
        double pushed = strategy.bodies().getFirst().progress;
        System.out.printf(Locale.ROOT, "births counted with friction: %.3f px%n", pushed);
        assertTrue(pushed > .5, "growth pressure moves the body: " + pushed);
    }

    @Test void progressCountsMovedCellsButNotNewcomers() {
        var strategy = strategy();
        Environment world = setup(strategy, genome(false));
        RaceTrainStrategy.Body body = strategy.bodies().get(3);
        Cell founder = body.cells.getFirst();
        founder.setPosition(founder.getPosition().add(new Vector2D(10, 0)));
        strategy.track(world, true);
        assertEquals(10, body.progress, 1e-9);
        Cell child = new Cell(founder.getPosition().add(new Vector2D(200, 0)), 10, founder.getBrain());
        founder.setVelocity(new Vector2D(30, 0));
        strategy.onBirth(founder, child);
        assertEquals(15, founder.getVelocity().getX(), 1e-12, "momentum shared, not doubled");
        assertEquals(15, child.getVelocity().getX(), 1e-12);
        strategy.track(world, true);
        assertEquals(10, body.progress, 1e-9, "a newcomer far ahead adds nothing");
        assertEquals(child.getPosition().getX(), body.lastX.get(child), "but it counts from its birth position on");
        founder.setPosition(founder.getPosition().add(new Vector2D(4, 0)));
        child.setPosition(child.getPosition().add(new Vector2D(-2, 0)));
        strategy.track(world, true);
        assertEquals(11, body.progress, 1e-9, "then it counts like every member: mean of +4 and -2");
        founder.setPosition(founder.getPosition().add(new Vector2D(6, 0)));
        strategy.track(world, false);
        assertEquals(11, body.progress, 1e-9, "growth phase: positions follow, nothing counts");
        // Leaving the lane disqualifies the body.
        founder.setPosition(new Vector2D(4 * RaceTrainStrategy.LANE_WIDTH + 100, founder.getPosition().getY()));
        strategy.track(world, true);
        assertTrue(body.invalid);
        assertEquals(0, body.score());
    }

    @Test void theFarthestBodyIsKeptUnchangedInLaneZero() {
        var strategy = strategy();
        Environment world = setup(strategy, genome(false));
        RaceTrainStrategy.Body winner = strategy.bodies().get(7);
        winner.genome.setGeneration(77);
        // Pushed by hand for the whole episode: a steady 1 px per step.
        double dt = 1.0 / 60;
        for (int i = 0; i < Math.round(RaceTrainStrategy.EPISODE_SECONDS / dt) - 1; i++) {
            for (Cell cell : winner.cells) cell.setPosition(cell.getPosition().add(new Vector2D(dt * 10, 0)));
            strategy.selectAndMutate(world, dt);
        }
        assertEquals(1, strategy.getGeneration());
        strategy.selectAndMutate(world, dt);
        assertEquals(2, strategy.getGeneration());
        RaceTrainStrategy.Evaluation evaluation = strategy.getLastEvaluation();
        // Counted from 35 s at 10 px/s: 150 px at 50 s, 250 px at 60 s.
        assertTrue(evaluation.score() > 150 && evaluation.score() < 250, "mean over the last ten seconds: " + evaluation.score());
        assertEquals(77, world.getCellList().getFirst().getGeneration(), "elite founder, not mutated");
        assertEquals(RaceTrainStrategy.LANES, world.getCellList().size(), "fresh founders");
        assertEquals(0, strategy.bodies().getFirst().progress);
    }

    /**
     * Opt-in: generations of the real race with random founders, one line per generation.
     * mvn test -Dtest=RaceTrainStrategyTest#study -Drace.study.generations=30
     */
    @Test @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "race.study.generations", matches = "[0-9]+")
    void study() {
        config.setMutationRate(rate); config.setMutationStrength(strength); config.setReproductionAgeThreshold(age);
        var strategy = new RaceTrainStrategy(new Random(Long.getLong("race.study.seed", 4711)));
        Environment world = strategy.initializeEnvironment();
        strategy.initialize(world);
        int generations = Integer.getInteger("race.study.generations");
        for (int g = 0; g < generations; g++) {
            long anchorsSeen = 0;
            StringBuilder elite = new StringBuilder();
            for (int s = 0; s < 120; s++) {
                run(strategy, world, .5);
                anchorsSeen += world.getCellList().stream().flatMap(c -> c.getSensorActors().stream()).filter(a -> a.getAnchor() != null).count();
                // Lane 0 replays last generation's best unchanged: its progress over the run phase.
                // The last sample (60 s) already belongs to the next episode.
                if (s >= 69 && s % 10 == 9 && s < 119 || s == 118) elite.append(String.format(Locale.ROOT, " %.0fs:%.2f", (s + 1) * .5, strategy.bodies().getFirst().progress));
            }
            System.out.printf(Locale.ROOT, "RACE study anchors/sample %.1f cells %d elite%s%n", anchorsSeen / 120.0, world.getCellList().size(), elite);
        }
    }
}
