package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import java.util.*;

/**
 * Race lanes (RENNBAHN.md): each lane grows one small body from a single founder by clonal division and scores how far
 * the body moves itself, in either direction: it starts in the middle of its lane and the distance counts, not the
 * side (since 26.10.2026; before, only motion to the right counted). Forces between cells cancel, so only grip on the terrain (friction where cells
 * press on, anchors) moves a body. Progress counts only the motion of cells already present in the previous step:
 * placing children ahead or pushing by numbers does not count, and the body stays capped at a few cells. Each episode
 * starts with a growth phase; progress counts only after it, when the push of the last births has died away (with
 * friction a newborn's overlap would push the body ahead).
 *
 * <p>Evolution with standard tools against local optima: the lanes form islands that select separately (elite and
 * tournament), every founder starts with a random turn and offset, and each generation runs every genome from
 * EVALUATIONS such starts and ranks it by the mean (a lucky jolt from one start is no gait; the elite is re-evaluated
 * and keeps its place only by moving again), the best of each island
 * migrates to the next one now and then, and an island whose record has not improved for a long time is wiped and
 * refilled with fresh random genomes (catastrophe). The record is the island's best moving mean over RECORD_WINDOW
 * generations, so a lucky generation cannot set a mark nobody beats again, and the island leading by that mean is
 * never wiped.
 */
public class RaceTrainStrategy implements TrainStrategy, SimulationStepPolicy {
    static final int ISLANDS = 4, ISLAND_LANES = 8, LANES = ISLANDS * ISLAND_LANES, CELL_LIMIT = 6, TOURNAMENT = 3,
            MIGRATION_INTERVAL = 10, STAGNATION_GENERATIONS = 30, EVALUATIONS = 3, RECORD_WINDOW = 10;
    static final double LANE_WIDTH = 1600, START = LANE_WIDTH / 2, HEIGHT = 800, EPISODE_SECONDS = 60, WINDOW_SECONDS = 10,
            SAMPLE_SECONDS = .5, ENERGY = .75, GROWTH_SECONDS = 30, RUN_START_SECONDS = 35, START_JITTER = 40,
            // A new island record must beat the old one by this much (noise from the random start is no progress).
            RECORD_MARGIN = .1, RECORD_FACTOR = 1.1;
    private final SimulationConfig config = SimulationConfig.getInstance();
    private final Random random;
    private final List<Body> bodies = new ArrayList<>();
    private final Map<Cell, Body> owners = new IdentityHashMap<>();
    int generation = 1;
    private int sampleNumber, catastrophes, evaluation;
    /** Scores of each lane's genome over the episodes of this generation. */
    private final double[] scoreSums = new double[LANES];
    private double elapsed;
    private final Island[] islands = new Island[ISLANDS];
    /** Tests switch the random turn and offset off to place founders exactly. */
    boolean randomStart = true;
    private volatile String status = "";
    public record Evaluation(int generation, double score, double progress, int cells) {}
    private volatile Evaluation lastEvaluation;

    /** One lane: its cells, their positions after the previous step and the progress so far. */
    static final class Body {
        final int lane;
        final Cell genome;
        final List<Cell> cells = new ArrayList<>();
        final Map<Cell, Double> lastX = new IdentityHashMap<>();
        double progress, windowSum;
        /** Mean score over the generation's episodes, set before selection. */
        double fitness;
        int windowSamples;
        boolean invalid;
        Body(int lane, Cell genome) { this.lane = lane; this.genome = genome; }
        int island() { return lane / ISLAND_LANES; }
        /** Distance of the mean progress over the final window, left or right; an escaped body scores nothing. */
        double score() { return invalid || windowSamples == 0 ? 0 : Math.abs(windowSum / windowSamples); }
    }

    /** One island: its recent best scores, its best moving mean so far and the generations since that record last improved. */
    static final class Island {
        final double[] recent = new double[RECORD_WINDOW];
        int scores;
        double record;
        int stagnant;
        /** Adds this generation's best score and returns the mean of the last RECORD_WINDOW (fewer at the start). */
        double add(double score) {
            recent[scores++ % RECORD_WINDOW] = score;
            return mean();
        }
        double mean() {
            int n = Math.min(scores, RECORD_WINDOW);
            double sum = 0;
            for (int i = 0; i < n; i++) sum += recent[i];
            return n == 0 ? 0 : sum / n;
        }
    }

    /** A lane's founder genome for the next episode; mutate false keeps an elite or migrant unchanged. */
    record Seed(Cell genome, boolean mutate) {}

    public RaceTrainStrategy() { this(new Random()); }
    RaceTrainStrategy(Random random) {
        this.random = Objects.requireNonNull(random);
        for (int i = 0; i < ISLANDS; i++) islands[i] = new Island();
    }
    public int getGeneration() { return generation; }
    int getCatastrophes() { return catastrophes; }
    Island island(int index) { return islands[index]; }
    public Evaluation getLastEvaluation() { return lastEvaluation; }
    List<Body> bodies() { return bodies; }
    @Override public String getStatus() { return status; }
    @Override public SimulationStepPolicy stepPolicy() { return this; }
    // Anchors need adhesion: the race follows the main switch like the open worlds.
    @Override public boolean useCellAdhesion() { return config.isCellAdhesion(); }
    @Override public boolean useSunlight() { return false; }
    @Override public boolean useEnergyConversion() { return false; }
    // Secured energy: division is limited by the cell cap, never by the energy needed for moving.
    @Override public BrainEnergyCosts brainEnergyCosts() { return BrainEnergyCosts.FREE; }
    @Override public boolean isDead(Cell cell) { return false; }

    @Override
    public Environment initializeEnvironment() {
        config.setWidth(LANES * LANE_WIDTH); config.setHeight(HEIGHT);
        config.setScaleSimulation(LANES * LANE_WIDTH / 1000);
        // Grip on the floor is the only propulsion: friction where cells press on (BODENREIBUNG.md) and anchors;
        // lane walls do not anchor.
        config.setBlockerFriction(true);
        config.setCellAdhesion(true);
        config.setGroundAdhesion(true);
        config.setGroundAnchors(true);
        config.setGroundAnchorsGroundOnly(true);
        config.setAdhesionBonds(true);
        config.setAdhesionBirthBonds(true);
        return new Environment(config.getWidth(), config.getHeight());
    }

    @Override
    public void initialize(Environment world) {
        List<Cell> genomes = new ArrayList<>();
        for (int i = 0; i < LANES; i++) genomes.add(randomGenome());
        restart(world, genomes);
    }

    private Cell randomGenome() {
        return CellFactory.createCell(new Vector2D(0, 0), config.getCellMaxRadiusSize() / 2, .5, .01, .025);
    }

    /** Imported cells provide genomes, one per lane where available. */
    @Override
    public void onPopulationImported(Environment world, List<Cell> imported, boolean merge) {
        List<Cell> candidates = new ArrayList<>(imported);
        if (merge) candidates.addAll(world.getCellList());
        Map<Integer, Cell> representatives = new LinkedHashMap<>();
        for (Cell cell : candidates) {
            if (!(cell.getBrain() instanceof CellBrain) || cell.isRemains()) continue;
            representatives.putIfAbsent((int) (cell.getPosition().getX() / LANE_WIDTH), cell);
        }
        if (representatives.isEmpty()) { if (!merge) initialize(world); return; }
        List<Cell> sources = new ArrayList<>(representatives.values()), genomes = new ArrayList<>();
        for (int i = 0; i < LANES; i++) genomes.add(sources.get(i % sources.size()));
        restart(world, genomes);
    }

    private void restart(Environment world, List<Cell> genomes) {
        generation = 1; catastrophes = 0; evaluation = 0; Arrays.fill(scoreSums, 0); lastEvaluation = null;
        for (int i = 0; i < ISLANDS; i++) islands[i] = new Island();
        startEpisode(world, genomes.stream().map(genome -> new Seed(genome, false)).toList());
    }

    /** Lanes with walls, one fresh founder per lane, placed with a random turn and offset. */
    void startEpisode(Environment world, List<Seed> seeds) {
        world.getBlockerList().clear();
        world.addGroundBlocker();
        for (int lane = 0; lane < LANES; lane++) world.addWallBlocker(lane * LANE_WIDTH, world.getHeight(), Environment.GroundBlockerHeight);
        bodies.clear(); owners.clear();
        List<Cell> population = new ArrayList<>();
        for (int lane = 0; lane < LANES; lane++) {
            Seed seed = seeds.get(lane);
            double radius = config.getCellMaxRadiusSize() / 2;
            double offset = randomStart ? (random.nextDouble() * 2 - 1) * START_JITTER : 0;
            Vector2D at = new Vector2D(lane * LANE_WIDTH + START + offset, world.getHeight() - Environment.GroundBlockerHeight - radius);
            Cell founder = ReproductionManagerService.createTrainingFounder(config, world, seed.genome(), at, 0, seed.mutate());
            if (randomStart) founder.setRotation(random.nextDouble() * 2 * Math.PI);
            for (SensorActor actor : founder.getSensorActors()) actor.updateCachedPosition();
            founder.setEnergy(ENERGY);
            Body body = new Body(lane, founder);
            body.cells.add(founder); owners.put(founder, body); bodies.add(body);
            population.add(founder);
        }
        world.resetCells(population);
        for (Body body : bodies) for (Cell cell : body.cells) body.lastX.put(cell, cell.getPosition().getX());
        elapsed = 0; sampleNumber = 0;
        updateStatus();
    }

    @Override
    public void beforeStep(Environment world, double dt) {
        for (Cell cell : world.getCellList()) cell.setEnergy(ENERGY);
    }

    @Override public boolean allowReproduction(Cell cell) {
        Body body = owners.get(cell);
        return body != null && body.cells.size() < CELL_LIMIT && elapsed < GROWTH_SECONDS;
    }
    // One genome per body: division copies it unchanged, selection acts on the founder's genome.
    @Override public Cell reproduce(Environment world, Cell parent) { return ReproductionManagerService.reproduceClonally(config, world, parent); }
    @Override public void onBirth(Cell parent, Cell child) {
        Body body = owners.get(parent);
        body.cells.add(child); owners.put(child, body);
        child.setEnergy(ENERGY);
        // A child inherits the parent's velocity, which adds momentum from nothing: a body dividing during a forward
        // jerk would coast ahead. Here parent and child share the momentum (equal masses: half the velocity each).
        Vector2D shared = parent.getVelocity().multiply(.5);
        parent.setVelocity(shared); child.setVelocity(shared);
        // Counted from its birth position: the placement is no progress, but its first push on the parent is
        // internal and must cancel like every other (otherwise a child dropped behind shoves the parent ahead for free).
        body.lastX.put(child, child.getPosition().getX());
    }

    @Override public void selectAndMutate(Environment world) { selectAndMutate(world, config.getCalcTimeStep()); }

    @Override
    public void selectAndMutate(Environment world, double dt) {
        elapsed += dt;
        track(world, elapsed + 1e-9 >= RUN_START_SECONDS);
        if (elapsed + 1e-9 >= (sampleNumber + 1) * SAMPLE_SECONDS) {
            sampleNumber++;
            if (elapsed + 1e-9 > EPISODE_SECONDS - WINDOW_SECONDS)
                for (Body body : bodies) { body.windowSum += body.progress; body.windowSamples++; }
            updateStatus();
        }
        if (elapsed + 1e-9 < EPISODE_SECONDS) return;
        for (Body body : bodies) scoreSums[body.lane] += body.score();
        if (++evaluation < EVALUATIONS) {
            // The same founders again (unchanged copies) from new random starts.
            startEpisode(world, bodies.stream().map(body -> new Seed(body.genome, false)).toList());
            return;
        }
        for (Body body : bodies) body.fitness = scoreSums[body.lane] / EVALUATIONS;
        evaluation = 0; Arrays.fill(scoreSums, 0);
        List<Body> ranked = new ArrayList<>(bodies);
        ranked.sort(Comparator.comparingDouble((Body b) -> b.fitness).reversed().thenComparingInt(b -> b.lane));
        Body best = ranked.getFirst();
        lastEvaluation = new Evaluation(generation, best.fitness, best.progress, best.cells.size());
        List<List<Body>> islandRanks = new ArrayList<>();
        for (int i = 0; i < ISLANDS; i++) { int index = i; islandRanks.add(ranked.stream().filter(b -> b.island() == index).toList()); }
        StringBuilder line = new StringBuilder();
        for (List<Body> island : islandRanks) line.append(String.format(Locale.ROOT, " %.3f", island.getFirst().fitness));
        System.out.printf(Locale.ROOT, "RACE generation=%d score=%.3f lastProgress=%.3f cells=%d median=%.3f islands%s catastrophes=%d%n",
                generation, best.fitness, best.progress, best.cells.size(), ranked.get(LANES / 2).fitness, line, catastrophes);
        startEpisode(world, nextSeeds(islandRanks));
        generation++;
        updateStatus();
    }

    /**
     * Next founders island by island, ranked by fitness: the elite unchanged in the island's first lane, the rest won by tournaments
     * and mutated. Every MIGRATION_INTERVAL generations the best of each island replaces the last lane of the next
     * one. The island stagnating longest past STAGNATION_GENERATIONS is wiped and refilled at random (one per
     * generation, so the islands do not all start over together); records are moving means, and the island with the
     * highest moving mean is spared.
     */
    List<Seed> nextSeeds(List<List<Body>> islandRanks) {
        int wiped = -1, leader = 0;
        double[] means = new double[ISLANDS];
        for (int i = 0; i < ISLANDS; i++) {
            means[i] = islands[i].add(islandRanks.get(i).getFirst().fitness);
            if (means[i] > means[leader]) leader = i;
        }
        for (int i = 0; i < ISLANDS; i++) {
            Island island = islands[i];
            if (means[i] > island.record * RECORD_FACTOR + RECORD_MARGIN) { island.record = means[i]; island.stagnant = 0; }
            else if (++island.stagnant >= STAGNATION_GENERATIONS && i != leader
                    && (wiped < 0 || island.stagnant > islands[wiped].stagnant)) wiped = i;
        }
        boolean migrate = generation % MIGRATION_INTERVAL == 0;
        List<Seed> seeds = new ArrayList<>();
        for (int i = 0; i < ISLANDS; i++) {
            List<Body> ranks = islandRanks.get(i);
            for (int lane = 0; lane < ISLAND_LANES; lane++) {
                if (i == wiped) seeds.add(new Seed(randomGenome(), false));
                else if (lane == 0) seeds.add(new Seed(ranks.getFirst().genome, false));
                else if (migrate && lane == ISLAND_LANES - 1)
                    seeds.add(new Seed(islandRanks.get((i + ISLANDS - 1) % ISLANDS).getFirst().genome, false));
                else seeds.add(new Seed(tournament(ranks).genome, true));
            }
        }
        if (wiped >= 0) {
            islands[wiped] = new Island();
            catastrophes++;
            System.out.printf(Locale.ROOT, "RACE catastrophe: island %d starts over from random genomes%n", wiped);
        }
        return seeds;
    }

    private Body tournament(List<Body> ranks) {
        Body winner = null;
        for (int k = 0; k < TOURNAMENT; k++) {
            Body candidate = ranks.get(random.nextInt(ranks.size()));
            if (winner == null || candidate.fitness > winner.fitness) winner = candidate;
        }
        return winner;
    }

    /**
     * Adds the mean horizontal step of the cells present before and after this step (when counting, i.e. after the
     * growth phase). With equal cell masses, forces between members cancel in this mean; what remains is grip on the
     * terrain (and lane walls, which are far away).
     */
    void track(Environment world, boolean count) {
        double width = world.getWidth();
        for (Body body : bodies) {
            double sum = 0;
            int moved = 0;
            for (Cell cell : body.cells) {
                double x = cell.getPosition().getX();
                Double last = body.lastX.put(cell, x);
                if (last != null) { sum += Math.IEEEremainder(x - last, width); moved++; }
                if ((int) (x / LANE_WIDTH) != body.lane) body.invalid = true;
            }
            if (count && moved > 0) body.progress += sum / moved;
        }
    }

    private void updateStatus() {
        double best = 0;
        for (Body body : bodies) if (!body.invalid) best = Math.max(best, Math.abs(body.progress));
        status = String.format(Locale.ROOT, "RACE · G%d · Start %d/%d · %.0f/%.0f s · %s · weitester %.1f px · letzte Bestwertung %s · %d Katastrophen",
                generation, evaluation + 1, EVALUATIONS, elapsed, EPISODE_SECONDS,
                elapsed < GROWTH_SECONDS ? "Wachstum" : elapsed < RUN_START_SECONDS ? "Ruhe" : "Lauf", best,
                lastEvaluation == null ? "—" : String.format(Locale.ROOT, "%.1f px", lastEvaluation.score()), catastrophes);
    }
}
