package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Locomotion physics behind the race training (RENNBAHN.md): forces between cells cancel, flat ground has no
 * friction, so a body moves sideways only by gripping the terrain (anchors). A scripted two-cell inchworm shows that
 * this is possible at all, and that the same gait without anchors, or pushing by births, does not count.
 */
class RaceGaitTest {
    static final double DT = 1.0 / 60;
    static final CellType GREY = new CellType(.5, .5, .5);
    final SimulationConfig config = SimulationConfig.getInstance();
    boolean adhesion, recognition, ground, anchors, bonds, birthBonds, friction;
    double coefficient, rolling;

    @BeforeEach void save() {
        adhesion = config.isCellAdhesion(); recognition = config.isAdhesionRecognition(); ground = config.isGroundAdhesion();
        anchors = config.isGroundAnchors(); bonds = config.isAdhesionBonds(); birthBonds = config.isAdhesionBirthBonds();
        friction = config.isBlockerFriction(); coefficient = config.getBlockerFrictionCoefficient();
        rolling = config.getBlockerRollingResistance();
    }
    @AfterEach void restore() {
        config.setCellAdhesion(adhesion); config.setAdhesionRecognition(recognition); config.setGroundAdhesion(ground);
        config.setGroundAnchors(anchors); config.setAdhesionBonds(bonds); config.setAdhesionBirthBonds(birthBonds);
        config.setBlockerFriction(friction); config.setBlockerFrictionCoefficient(coefficient);
        config.setBlockerRollingResistance(rolling);
    }

    /**
     * Rear and front cell take turns: one grips with its foot, the other lets go and is pushed or pulled. Anchors lock
     * only after 0.5 s, so the front may still grip while it is pushed. Friction grips as the foot presses on, so the
     * friction gait lets the moving cell go and keeps a hold beat between push and pull while the grip builds up
     * (tonus): push the front (rear grips), both grip, pull the rear in (front grips), both grip.
     */
    static final class Inchworm implements SimulationStepPolicy {
        final Cell rear, front;
        final double period;
        final boolean frictionGait;
        static final double STROKE = 4, GAIN = 80, DAMPING = 80;
        double time;
        final Map<Cell, Double> lastGap = new IdentityHashMap<>();
        Inchworm(Cell rear, Cell front, double period, boolean frictionGait) {
            this.rear = rear; this.front = front; this.period = period; this.frictionGait = frictionGait;
        }
        @Override public boolean useSunlight() { return false; }
        @Override public boolean useEnergyConversion() { return false; }
        @Override public boolean useCellAdhesion() { return true; }
        @Override public BrainEnergyCosts brainEnergyCosts() { return BrainEnergyCosts.FREE; }
        @Override public boolean isDead(Cell cell) { return false; }
        @Override public boolean allowReproduction(Cell cell) { return false; }
        @Override public void afterThinking(Cell cell) {
            cell.setRadiusSize(20); // the test brain would shrink the cells out of field reach
            // Four beats: rear grips; push the front ahead; front grips too; rear lets go and is pulled in.
            int beat = (int) (time % period / (period / 4));
            boolean grips = cell == rear ? beat <= 2 : beat >= 1;
            boolean open = beat == 1 || beat == 2;
            if (frictionGait) { grips = cell == rear ? beat != 2 : beat != 0; open = beat <= 1; }
            Cell partner = cell == rear ? front : rear;
            double attract = -SimulationConfig.getInstance().getCellActorMaxAttractiveForceStrength();
            double repel = SimulationConfig.getInstance().getCellActorMaxRepulsiveForceStrength();
            Vector2D toPartner = partner.getPosition().subtract(cell.getPosition());
            // The actor fields reach only about 9 px beyond the rim: hold the gap with a damped controller,
            // a few px while pushing apart, closed while pulling in.
            double gap = toPartner.length() - cell.getRadiusSize() - partner.getRadiusSize();
            toPartner = toPartner.normalize();
            // Opening speed from the gap actually moved (velocities still carry pushes that friction will cancel).
            Double last = lastGap.put(cell, gap);
            double opening = last == null ? 0 : (gap - last) / DT;
            double toward = Math.max(attract, Math.min(repel, GAIN * ((open ? STROKE : 0) - gap) - DAMPING * opening));
            for (SensorActor actor : cell.getSensorActors()) {
                Vector2D offset = actor.calcPosition().subtract(cell.getPosition()).normalize();
                actor.setType(GREY);
                if (offset.getY() > .5) actor.setForceStrength(grips ? attract : repel); // foot: grip or let go
                else if (offset.getX() * toPartner.getX() + offset.getY() * toPartner.getY() > .5)
                    actor.setForceStrength(toward); // towards the partner: push apart, pull in
                else actor.setForceStrength(0);
            }
        }
    }

    /** Horizontal movement of the pair's centre over the given time. */
    double crawl(boolean withAnchors, double seconds) { return crawl(withAnchors, 0, seconds); }

    double crawl(boolean withAnchors, double friction, double seconds) { return crawl(withAnchors, friction, false, 4, seconds); }

    double crawl(boolean withAnchors, double friction, boolean frictionGait, double period, double seconds) {
        config.setCellAdhesion(true); config.setAdhesionRecognition(true);
        config.setBlockerFriction(friction > 0); if (friction > 0) config.setBlockerFrictionCoefficient(friction);
        config.setBlockerRollingResistance(.5);
        config.setGroundAdhesion(true); config.setGroundAnchors(withAnchors);
        config.setAdhesionBonds(false); config.setAdhesionBirthBonds(false);
        Environment world = new Environment(2000, 400);
        world.addGroundBlocker();
        double floor = 400 - Environment.GroundBlockerHeight;
        Cell rear = cell(world, 500, floor), front = cell(world, 541, floor);
        Inchworm gait = new Inchworm(rear, front, period, frictionGait);
        var grid = new SpatialGridPartitioningStrategy(2000, 400, 100);
        double start = (rear.getPosition().getX() + front.getPosition().getX()) / 2;
        for (int step = 0; step < Math.round(seconds / DT); step++) {
            grid.build(world.getCellList());
            world.update(DT, grid, gait);
            gait.time += DT;
        }
        return (rear.getPosition().getX() + front.getPosition().getX()) / 2 - start;
    }

    static Cell cell(Environment world, double x, double floor) {
        Cell cell = new Cell(new Vector2D(x, floor - 20), 20, new PhysicsRegressionTest.ConstantBrain());
        cell.setType(GREY); cell.setEnergy(1);
        for (SensorActor actor : cell.getSensorActors()) actor.updateCachedPosition();
        world.addCell(cell);
        return cell;
    }

    @Test void onlyGrippingTheGroundMovesABody() {
        double gripping = crawl(true, 30), sliding = crawl(false, 30);
        System.out.printf(Locale.ROOT, "inchworm 30 s: anchors %.1f px, without anchors %.1f px%n", gripping, sliding);
        assertEquals(0, sliding, 1, "forces between cells cancel, flat ground has no friction");
        // Slow: anchors are soft springs within the short actor fields (about 9 px), each beat gains a few px.
        assertTrue(gripping > 8, "anchors turn the gait into progress: " + gripping);
    }

    /** BODENREIBUNG.md: with terrain friction the pressing foot holds without any anchor. */
    @Test void frictionLetsTheBodyWalkWithoutAnchors() {
        for (double mu : new double[]{.25, .5, 1})
            System.out.printf(Locale.ROOT, "friction %.2f, 30 s: friction gait %.1f px, anchor gait %.1f px%n",
                    mu, crawl(false, mu, true, 2, 30), crawl(false, mu, false, 4, 30));
        // Default coefficients 0.5/0.5: the pressing foot holds (about 4 g), the released cell slides at about 1 g push.
        double walking = crawl(false, .5, true, 2, 30);
        assertTrue(walking > 15, "pressing the gripping foot on holds it: " + walking);
        assertEquals(0, crawl(false, 0, true, 2, 30), 1, "the same gait without friction stays");
    }
}
