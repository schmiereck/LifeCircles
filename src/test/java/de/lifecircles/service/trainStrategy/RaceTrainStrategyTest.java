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

    RaceTrainStrategy strategy() {
        var strategy = new RaceTrainStrategy(new Random(98));
        strategy.randomStart = false;
        return strategy;
    }

    Environment setup(RaceTrainStrategy strategy, Cell genome) {
        Environment world = strategy.initializeEnvironment();
        strategy.startEpisode(world, Collections.nCopies(RaceTrainStrategy.LANES, new RaceTrainStrategy.Seed(genome, false)));
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
        strategy.bodies().get(7).genome.setGeneration(77);
        // Pushed by hand in every episode of the generation: a steady 10 px/s, the same founder from each start.
        double dt = 1.0 / 60;
        long steps = Math.round(RaceTrainStrategy.EPISODE_SECONDS / dt);
        for (int episode = 0; episode < RaceTrainStrategy.EVALUATIONS; episode++) {
            RaceTrainStrategy.Body winner = strategy.bodies().get(7);
            assertEquals(77, winner.genome.getGeneration(), "replayed unchanged");
            for (int i = 0; i < steps - 1; i++) {
                for (Cell cell : winner.cells) cell.setPosition(cell.getPosition().add(new Vector2D(dt * 10, 0)));
                strategy.selectAndMutate(world, dt);
            }
            assertEquals(1, strategy.getGeneration());
            strategy.selectAndMutate(world, dt);
        }
        assertEquals(2, strategy.getGeneration());
        RaceTrainStrategy.Evaluation evaluation = strategy.getLastEvaluation();
        // Counted from 35 s at 10 px/s: 150 px at 50 s, 250 px at 60 s.
        assertTrue(evaluation.score() > 150 && evaluation.score() < 250, "mean over the last ten seconds: " + evaluation.score());
        assertEquals(77, world.getCellList().getFirst().getGeneration(), "elite founder, not mutated");
        assertEquals(RaceTrainStrategy.LANES, world.getCellList().size(), "fresh founders");
        assertEquals(0, strategy.bodies().getFirst().progress);
    }

    @Test void aSteadyMoverBeatsAJoltFromOneStart() {
        var strategy = strategy();
        Environment world = setup(strategy, genome(false));
        strategy.bodies().get(3).genome.setGeneration(33);
        double dt = 1.0 / 60;
        long steps = Math.round(RaceTrainStrategy.EPISODE_SECONDS / dt);
        for (int episode = 0; episode < RaceTrainStrategy.EVALUATIONS; episode++) {
            RaceTrainStrategy.Body steady = strategy.bodies().get(3), lucky = strategy.bodies().get(5);
            for (int i = 0; i < steps; i++) {
                for (Cell cell : steady.cells) cell.setPosition(cell.getPosition().add(new Vector2D(dt * 4, 0)));
                if (episode == 0) for (Cell cell : lucky.cells) cell.setPosition(cell.getPosition().add(new Vector2D(dt * 10, 0)));
                strategy.selectAndMutate(world, dt);
            }
        }
        assertEquals(2, strategy.getGeneration());
        // Steady: 4 px/s, about 80 px in every episode. Lucky: about 200 px once, a third of that as the mean.
        assertEquals(80, strategy.getLastEvaluation().score(), 20);
        assertEquals(33, world.getCellList().getFirst().getGeneration(), "the steady mover is the elite");
    }

    @Test void bodiesStartMidLaneAndEitherDirectionCounts() {
        var strategy = strategy();
        Environment world = setup(strategy, genome(false));
        RaceTrainStrategy.Body first = strategy.bodies().getFirst();
        assertEquals(RaceTrainStrategy.LANE_WIDTH / 2, first.cells.getFirst().getPosition().getX(), 1e-9);
        strategy.bodies().get(3).genome.setGeneration(33);
        double dt = 1.0 / 60;
        long steps = Math.round(RaceTrainStrategy.EPISODE_SECONDS / dt);
        for (int episode = 0; episode < RaceTrainStrategy.EVALUATIONS; episode++) {
            // Lane 3 walks left in the first start and right in the others, lane 5 right at half the speed.
            RaceTrainStrategy.Body walker = strategy.bodies().get(3), slow = strategy.bodies().get(5);
            double speed = episode == 0 ? -10 : 10;
            for (int i = 0; i < steps; i++) {
                for (Cell cell : walker.cells) cell.setPosition(cell.getPosition().add(new Vector2D(dt * speed, 0)));
                for (Cell cell : slow.cells) cell.setPosition(cell.getPosition().add(new Vector2D(dt * 5, 0)));
                strategy.selectAndMutate(world, dt);
            }
        }
        // 10 px/s from 35 s: about 200 px over the last window in every start, whichever way.
        assertEquals(200, strategy.getLastEvaluation().score(), 30);
        assertEquals(33, world.getCellList().getFirst().getGeneration(), "the faster walker is the elite");
    }

    /** Ranked bodies per island: island i scores base[i], its lanes a little less each. */
    static List<List<RaceTrainStrategy.Body>> ranks(double... base) {
        List<List<RaceTrainStrategy.Body>> islands = new ArrayList<>();
        for (int i = 0; i < RaceTrainStrategy.ISLANDS; i++) {
            List<RaceTrainStrategy.Body> island = new ArrayList<>();
            for (int l = 0; l < RaceTrainStrategy.ISLAND_LANES; l++) {
                var body = new RaceTrainStrategy.Body(i * RaceTrainStrategy.ISLAND_LANES + l, genome(false));
                body.fitness = base[i] - l * .01;
                island.add(body);
            }
            islands.add(island);
        }
        return islands;
    }

    static boolean from(List<RaceTrainStrategy.Body> island, Cell genome) {
        return island.stream().anyMatch(b -> b.genome == genome);
    }

    @Test void islandsSelectSeparatelyAndTheirBestMigrates() {
        var strategy = strategy();
        var islands = ranks(1, 2, 3, 4);
        List<RaceTrainStrategy.Seed> seeds = strategy.nextSeeds(islands);
        assertEquals(RaceTrainStrategy.LANES, seeds.size());
        for (int i = 0; i < RaceTrainStrategy.ISLANDS; i++)
            for (int l = 0; l < RaceTrainStrategy.ISLAND_LANES; l++) {
                var seed = seeds.get(i * RaceTrainStrategy.ISLAND_LANES + l);
                if (l == 0) assertSame(islands.get(i).getFirst().genome, seed.genome(), "elite of its own island");
                assertEquals(l > 0, seed.mutate());
                assertTrue(from(islands.get(i), seed.genome()), "tournament within the island");
            }
        strategy.generation = RaceTrainStrategy.MIGRATION_INTERVAL;
        seeds = strategy.nextSeeds(islands);
        for (int i = 0; i < RaceTrainStrategy.ISLANDS; i++) {
            var migrant = seeds.get(i * RaceTrainStrategy.ISLAND_LANES + RaceTrainStrategy.ISLAND_LANES - 1);
            assertSame(islands.get((i + RaceTrainStrategy.ISLANDS - 1) % RaceTrainStrategy.ISLANDS).getFirst().genome, migrant.genome(),
                    "the best of the previous island competes here");
            assertFalse(migrant.mutate());
        }
    }

    @Test void aStagnantIslandStartsOverFromRandomGenomesOneAtATime() {
        var strategy = strategy();
        var islands = ranks(1, 2, 3, 4);
        strategy.nextSeeds(islands); // sets the records
        // Islands 0-2 repeat their score (or gain within the noise margin), island 3 keeps improving by 20 %.
        for (int g = 1; g < RaceTrainStrategy.STAGNATION_GENERATIONS; g++)
            strategy.nextSeeds(ranks(1, 2, 1.1 * 3, 4 * Math.pow(1.2, g)));
        assertEquals(0, strategy.getCatastrophes());
        assertEquals(RaceTrainStrategy.STAGNATION_GENERATIONS - 1, strategy.island(2).stagnant, "within the margin is no record");
        assertEquals(0, strategy.island(3).stagnant, "a new record beyond the margin resets the count");
        List<RaceTrainStrategy.Seed> seeds = strategy.nextSeeds(islands);
        assertEquals(1, strategy.getCatastrophes());
        for (int l = 0; l < RaceTrainStrategy.ISLAND_LANES; l++) {
            var seed = seeds.get(l);
            assertFalse(islands.stream().anyMatch(island -> from(island, seed.genome())), "fresh random genome");
        }
        assertTrue(from(islands.get(1), seeds.get(RaceTrainStrategy.ISLAND_LANES).genome()), "only one island per generation");
        assertEquals(0, strategy.island(0).record);
        strategy.nextSeeds(islands);
        assertEquals(2, strategy.getCatastrophes(), "next generation the next stagnant island");
        assertEquals(1, strategy.island(0).record, "the wiped island starts a new record");
    }

    @Test void foundersStartWithARandomTurnAndOffset() {
        var strategy = strategy();
        strategy.randomStart = true;
        Environment world = setup(strategy, genome(false));
        Set<Double> turns = new HashSet<>();
        for (RaceTrainStrategy.Body body : strategy.bodies()) {
            Cell founder = body.cells.getFirst();
            turns.add(founder.getRotation());
            double x = founder.getPosition().getX() - body.lane * RaceTrainStrategy.LANE_WIDTH - RaceTrainStrategy.START;
            assertTrue(Math.abs(x) <= RaceTrainStrategy.START_JITTER, "offset " + x);
        }
        assertEquals(RaceTrainStrategy.LANES, turns.size(), "the elite is re-evaluated from another start, not replayed");
    }

    /**
     * Opt-in: generations of the real race with random founders, one line per generation.
     * mvn test -Dtest=RaceTrainStrategyTest#study -Drace.study.generations=30
     */
    @Test @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "race.study.generations", matches = "[0-9]+")
    void study() {
        config.setMutationRate(rate); config.setMutationStrength(strength); config.setReproductionAgeThreshold(age);
        double gravity = config.getGravity();
        // Optional share of the default gravity, e.g. -Drace.study.gravity=0.1
        config.setGravity(gravity * Double.parseDouble(System.getProperty("race.study.gravity", "1")));
        try { study(gravity); } finally { config.setGravity(gravity); }
    }

    /**
     * A gait needs actors (or the size) to change back and forth. Per generation the study counts, in the first
     * start's run phase (35-60 s), the bodies whose founder flips an actor between pulling and pushing at least
     * four times, and those whose founder's radius varies by more than 2 px.
     */
    private void study(double gravity) {
        var strategy = new RaceTrainStrategy(new Random(Long.getLong("race.study.seed", 4711)));
        Environment world = strategy.initializeEnvironment();
        strategy.initialize(world);
        int generations = Integer.getInteger("race.study.generations");
        for (int g = 0; g < generations; g++) {
            long anchorsSeen = 0;
            StringBuilder elite = new StringBuilder();
            int[][] flips = new int[RaceTrainStrategy.LANES][12];
            double[][] signs = new double[RaceTrainStrategy.LANES][12];
            double[] minRadius = new double[RaceTrainStrategy.LANES], maxRadius = new double[RaceTrainStrategy.LANES];
            Arrays.fill(minRadius, Double.MAX_VALUE);
            int samples = 120 * RaceTrainStrategy.EVALUATIONS;
            for (int s = 0; s < samples; s++) {
                run(strategy, world, .5);
                anchorsSeen += world.getCellList().stream().flatMap(c -> c.getSensorActors().stream()).filter(a -> a.getAnchor() != null).count();
                // Lane 0 replays last generation's best unchanged: its progress over the run phase.
                // The last sample (60 s) already belongs to the next episode.
                if (s >= 69 && s < 119) for (RaceTrainStrategy.Body body : strategy.bodies()) {
                    Cell founder = body.cells.getFirst();
                    minRadius[body.lane] = Math.min(minRadius[body.lane], founder.getRadiusSize());
                    maxRadius[body.lane] = Math.max(maxRadius[body.lane], founder.getRadiusSize());
                    List<SensorActor> actors = founder.getSensorActors();
                    for (int a = 0; a < actors.size(); a++) {
                        double force = actors.get(a).getForceStrength();
                        if (Math.abs(force) < .05) continue;
                        if (signs[body.lane][a] != 0 && Math.signum(force) != signs[body.lane][a]) flips[body.lane][a]++;
                        signs[body.lane][a] = Math.signum(force);
                    }
                }
                if (s >= 69 && s % 10 == 9 && s < 119 || s == 118) elite.append(String.format(Locale.ROOT, " %.0fs:%.2f", (s + 1) * .5, strategy.bodies().getFirst().progress));
            }
            int oscillating = 0, pulsing = 0;
            for (int lane = 0; lane < RaceTrainStrategy.LANES; lane++) {
                if (Arrays.stream(flips[lane]).max().orElse(0) >= 4) oscillating++;
                if (maxRadius[lane] - minRadius[lane] > 2) pulsing++;
            }
            System.out.printf(Locale.ROOT, "RACE study anchors/sample %.1f cells %d oscillating %d pulsing %d elite (first start)%s%n",
                    anchorsSeen / (double) samples, world.getCellList().size(), oscillating, pulsing, elite);
        }
    }
}
