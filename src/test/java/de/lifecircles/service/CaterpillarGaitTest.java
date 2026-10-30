package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A scripted caterpillar of three or six cells in a row on the race floor (RENNBAHN.md, Raupe): a wave runs from the
 * front to the rear, in each beat exactly one cell lets go of the ground and is pushed ahead by its rear neighbour
 * and pulled after by its front neighbour, while all others press their grey feet on and hold by friction. Checks
 * that the physics carries such a gait, and how terrain stickiness (which presses every foot, even a released one)
 * and missing friction change it.
 */
class CaterpillarGaitTest {
    static final double DT = 1.0 / 60;
    final SimulationConfig config = SimulationConfig.getInstance();
    boolean adhesion, recognition, ground, anchors, bonds, birthBonds, friction;
    double coefficient, rolling, stickiness, gravity;

    @BeforeEach void save() {
        adhesion = config.isCellAdhesion(); recognition = config.isAdhesionRecognition(); ground = config.isGroundAdhesion();
        anchors = config.isGroundAnchors(); bonds = config.isAdhesionBonds(); birthBonds = config.isAdhesionBirthBonds();
        friction = config.isBlockerFriction(); coefficient = config.getBlockerFrictionCoefficient();
        rolling = config.getBlockerRollingResistance(); stickiness = config.getGroundStickiness(); gravity = config.getGravity();
    }
    @AfterEach void restore() {
        config.setCellAdhesion(adhesion); config.setAdhesionRecognition(recognition); config.setGroundAdhesion(ground);
        config.setGroundAnchors(anchors); config.setAdhesionBonds(bonds); config.setAdhesionBirthBonds(birthBonds);
        config.setBlockerFriction(friction); config.setBlockerFrictionCoefficient(coefficient);
        config.setBlockerRollingResistance(rolling); config.setGroundStickiness(stickiness); config.setGravity(gravity);
    }

    /**
     * Hard-coded brain for all cells. A cycle has one beat per cell plus a hold beat; in beat k the cell n-1-k (front
     * first) moves: the gap behind it opens to STROKE, the gap in front of it (opened in the previous beat) closes.
     * Feet grip except on the moving cell; in the hold beat all grip and all gaps close.
     */
    static final class Caterpillar implements SimulationStepPolicy {
        final List<Cell> cells;
        final double beat;
        static final double STROKE = 4, GAIN = 80, DAMPING = 80;
        double time;
        final Map<Cell, double[]> lastGaps = new IdentityHashMap<>();
        Caterpillar(List<Cell> cells, double beat) { this.cells = cells; this.beat = beat; }
        @Override public boolean useSunlight() { return false; }
        @Override public boolean useEnergyConversion() { return false; }
        @Override public boolean useCellAdhesion() { return true; }
        @Override public BrainEnergyCosts brainEnergyCosts() { return BrainEnergyCosts.FREE; }
        @Override public boolean isDead(Cell cell) { return false; }
        @Override public boolean allowReproduction(Cell cell) { return false; }

        /** The cell moving in this beat, or -1 in the hold beat. */
        int moving() {
            int k = (int) (time / beat) % (cells.size() + 1);
            return k == cells.size() ? -1 : cells.size() - 1 - k;
        }
        /** Target gap between cell i and i+1: open behind the moving cell. */
        double targetGap(int i) { return i + 1 == moving() ? STROKE : 0; }

        @Override public void afterThinking(Cell cell) {
            cell.setRadiusSize(20); // the test brain would shrink the cells out of field reach
            int index = cells.indexOf(cell);
            double attract = -SimulationConfig.getInstance().getCellActorMaxAttractiveForceStrength();
            double repel = SimulationConfig.getInstance().getCellActorMaxRepulsiveForceStrength();
            double[] last = lastGaps.computeIfAbsent(cell, c -> new double[]{Double.NaN, Double.NaN});
            // Push or pull towards each neighbour with a damped controller on the gap (fields reach about 9 px).
            Vector2D[] toward = new Vector2D[2];
            double[] force = new double[2];
            for (int side = 0; side < 2; side++) {
                int other = index + (side == 0 ? -1 : 1), pair = Math.min(index, other);
                if (other < 0 || other >= cells.size()) continue;
                Cell partner = cells.get(other);
                Vector2D offset = partner.getPosition().subtract(cell.getPosition());
                double gap = offset.length() - cell.getRadiusSize() - partner.getRadiusSize();
                double opening = Double.isNaN(last[side]) ? 0 : (gap - last[side]) / DT;
                last[side] = gap;
                toward[side] = offset.normalize();
                force[side] = Math.max(attract, Math.min(repel, GAIN * (targetGap(pair) - gap) - DAMPING * opening));
            }
            boolean grips = index != moving();
            for (SensorActor actor : cell.getSensorActors()) {
                Vector2D offset = actor.calcPosition().subtract(cell.getPosition()).normalize();
                actor.setType(RaceGaitTest.GREY);
                double strength = 0;
                if (offset.getY() > .5) strength = grips ? attract : repel; // foot: press on or let go
                else for (int side = 0; side < 2; side++)
                    if (toward[side] != null && offset.getX() * toward[side].getX() + offset.getY() * toward[side].getY() > .5)
                        strength = force[side];
                actor.setForceStrength(strength);
            }
        }
    }

    /** Centre movement of an n-cell caterpillar over the given time. */
    double crawl(int n, double frictionCoefficient, double terrainStickiness, double beat, double seconds) {
        config.setCellAdhesion(true); config.setAdhesionRecognition(true);
        config.setBlockerFriction(frictionCoefficient > 0);
        if (frictionCoefficient > 0) config.setBlockerFrictionCoefficient(frictionCoefficient);
        config.setBlockerRollingResistance(.5);
        config.setGroundAdhesion(true); config.setGroundAnchors(false); config.setGroundStickiness(terrainStickiness);
        config.setAdhesionBonds(false); config.setAdhesionBirthBonds(false);
        Environment world = new Environment(2000, 400);
        world.addGroundBlocker();
        double floor = 400 - Environment.GroundBlockerHeight;
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < n; i++) cells.add(RaceGaitTest.cell(world, 700 + 41 * i, floor));
        Caterpillar gait = new Caterpillar(cells, beat);
        var grid = new SpatialGridPartitioningStrategy(2000, 400, 100);
        double start = centre(cells);
        for (int step = 0; step < Math.round(seconds / DT); step++) {
            grid.build(world.getCellList());
            world.update(DT, grid, gait);
            gait.time += DT;
        }
        return centre(cells) - start;
    }

    static double centre(List<Cell> cells) {
        return cells.stream().mapToDouble(cell -> cell.getPosition().getX()).average().orElseThrow();
    }

    /** Friction carries the gait for three and six cells; without friction the centre stays (forces cancel). */
    @Test void caterpillarsCrawlOnlyByFriction() {
        for (int n : new int[]{3, 6}) {
            double crawling = crawl(n, .5, 0, 1, 30), sliding = crawl(n, 0, 0, 1, 30);
            System.out.printf(Locale.ROOT, "caterpillar %d cells, beat 1 s, 30 s: friction %.1f px, without friction %.1f px%n",
                    n, crawling, sliding);
            assertTrue(crawling > 8, n + " cells: " + crawling);
            assertEquals(0, sliding, 1, n + " cells without friction");
        }
    }

    /**
     * Terrain stickiness pulls every foot actor near the ground, also the released one: the moving cell stays pressed
     * on and slides less. Settings of RACE-R1-all-2026-10-30-a01 (stickiness -22.4, gravity 17.57).
     */
    @Test void terrainStickinessSlowsTheCaterpillar() {
        config.setGravity(17.56610406959048);
        double free = crawl(3, .5, 0, 1, 30), sticky = crawl(3, .5, -22.4, 1, 30);
        System.out.printf(Locale.ROOT, "caterpillar 3 cells, gravity 17.57, 30 s: stickiness 0 %.1f px, -22.4 %.1f px%n", free, sticky);
        assertTrue(sticky > 5, "still crawls: " + sticky);
        assertTrue(sticky < free * .9, "slower: " + sticky + " vs " + free);
    }
}
