package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import java.util.*;

/**
 * Race lanes (RENNBAHN.md): each lane grows one small body from a single founder by clonal division and scores how far
 * the body moves itself to the right. Forces between cells cancel and the flat ground has no friction, so only grip on
 * the terrain (anchors) moves a body. Progress counts only the motion of cells already present in the previous step:
 * placing children ahead or pushing by numbers does not count, and the body stays capped at a few cells.
 */
public class RaceTrainStrategy implements TrainStrategy, SimulationStepPolicy {
    static final int LANES = 16, CELL_LIMIT = 6;
    static final double LANE_WIDTH = 1600, START = 400, HEIGHT = 800, EPISODE_SECONDS = 60, WINDOW_SECONDS = 10,
            SAMPLE_SECONDS = .5, ENERGY = .75;
    private final SimulationConfig config = SimulationConfig.getInstance();
    private final Random random;
    private final List<Body> bodies = new ArrayList<>();
    private final Map<Cell, Body> owners = new IdentityHashMap<>();
    private int generation = 1, sampleNumber;
    private double elapsed;
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
        int windowSamples;
        boolean invalid;
        Body(int lane, Cell genome) { this.lane = lane; this.genome = genome; }
        /** Mean progress over the final window; an escaped body scores nothing. */
        double score() { return invalid || windowSamples == 0 ? 0 : windowSum / windowSamples; }
    }

    public RaceTrainStrategy() { this(new Random()); }
    RaceTrainStrategy(Random random) { this.random = Objects.requireNonNull(random); }
    public int getGeneration() { return generation; }
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
        // Grip on the floor is the only propulsion; lane walls do not hold.
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
        for (int i = 0; i < LANES; i++) genomes.add(CellFactory.createCell(new Vector2D(0, 0), config.getCellMaxRadiusSize() / 2, .5, .01, .025));
        restart(world, genomes);
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
        generation = 1; lastEvaluation = null;
        startEpisode(world, genomes, false);
    }

    /** Lanes with walls, one fresh founder per lane; mutate gives every lane but the first a mutated genome. */
    void startEpisode(Environment world, List<Cell> genomes, boolean mutate) {
        world.getBlockerList().clear();
        world.addGroundBlocker();
        for (int lane = 0; lane < LANES; lane++) world.addWallBlocker(lane * LANE_WIDTH, world.getHeight(), Environment.GroundBlockerHeight);
        bodies.clear(); owners.clear();
        List<Cell> population = new ArrayList<>();
        for (int lane = 0; lane < LANES; lane++) {
            Cell parent = genomes.get(lane);
            double radius = config.getCellMaxRadiusSize() / 2;
            Vector2D at = new Vector2D(lane * LANE_WIDTH + START, world.getHeight() - Environment.GroundBlockerHeight - radius);
            Cell founder = ReproductionManagerService.createTrainingFounder(config, world, parent, at, 0, mutate && lane > 0);
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
        return body != null && body.cells.size() < CELL_LIMIT;
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
        track(world);
        if (elapsed + 1e-9 >= (sampleNumber + 1) * SAMPLE_SECONDS) {
            sampleNumber++;
            if (elapsed + 1e-9 > EPISODE_SECONDS - WINDOW_SECONDS)
                for (Body body : bodies) { body.windowSum += body.progress; body.windowSamples++; }
            updateStatus();
        }
        if (elapsed + 1e-9 < EPISODE_SECONDS) return;
        List<Body> ranked = new ArrayList<>(bodies);
        ranked.sort(Comparator.comparingDouble(Body::score).reversed().thenComparingInt(b -> b.lane));
        Body best = ranked.getFirst();
        lastEvaluation = new Evaluation(generation, best.score(), best.progress, best.cells.size());
        System.out.printf(Locale.ROOT, "RACE generation=%d score=%.3f progress=%.3f cells=%d median=%.3f%n",
                generation, best.score(), best.progress, best.cells.size(), ranked.get(LANES / 2).score());
        // Elite in lane 0, the others from the best three, mutated at the start of the next episode.
        List<Cell> next = new ArrayList<>();
        for (int lane = 0; lane < LANES; lane++) next.add(ranked.get(lane == 0 ? 0 : random.nextInt(3)).genome);
        generation++;
        startEpisode(world, next, true);
    }

    /**
     * Adds the mean horizontal step of the cells present before and after this step. With equal cell masses, forces
     * between members cancel in this mean; what remains is grip on the terrain (and lane walls, which are far away).
     */
    void track(Environment world) {
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
            if (moved > 0) body.progress += sum / moved;
        }
    }

    private void updateStatus() {
        double best = 0;
        for (Body body : bodies) if (!body.invalid) best = Math.max(best, body.progress);
        status = String.format(Locale.ROOT, "RACE · G%d · %.0f/%.0f s · vorn %.1f px · letzte Bestwertung %s",
                generation, elapsed, EPISODE_SECONDS, best,
                lastEvaluation == null ? "—" : String.format(Locale.ROOT, "%.1f px", lastEvaluation.score()));
    }
}
