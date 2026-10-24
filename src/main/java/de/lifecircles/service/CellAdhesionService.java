package de.lifecircles.service;

import de.lifecircles.model.Blocker;
import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Force tonus after thinking: commands are approached gradually, paused brains hold, remains fade. */
public final class CellAdhesionService {
    /** Seconds two actors must stay close to lock in, or a brain must push to release. */
    public static final double LOCK_SECONDS = .5;
    /** Locking distance as a share of the larger field range. */
    public static final double LOCK_RANGE_FRACTION = .5;
    /** Minimum complete affinity (actor colour times body type) for locking in. */
    public static final double LOCK_MIN_AFFINITY = .8;
    /** Fading bonds of remains dissolve below this strength. */
    static final double MIN_BOND_STRENGTH = 1e-3;

    /** A fresh stamp per updateBonds call marks the cells of its list (instead of an identity set). */
    private static final AtomicLong LIST_MARKS = new AtomicLong();

    private CellAdhesionService() {}

    public static void settleForces(final List<Cell> cells, final double dt) {
        final SimulationConfig config = SimulationConfig.getInstance();
        final double tau = config.getActorForceTonusSeconds();
        final double alpha = tau == 0 ? 1 : 1 - Math.exp(-dt / tau);
        final double fade = remainsFade(config, dt);
        // Each worker writes only its own cell's actors.
        cells.parallelStream().forEach(cell -> {
            for (SensorActor actor : cell.getSensorActors()) {
                if (cell.isRemains()) actor.decayForce(fade);
                else actor.settleForce(alpha);
            }
        });
    }

    static double remainsFade(SimulationConfig config, double dt) {
        return Math.exp(-Math.log(2) * dt / config.getRemainsForceHalfLife());
    }

    /**
     * Stage 3 (ZELLVERBINDUNGEN.md). Existing bonds fade with remains, tear beyond field range and release after a
     * brain pushes on them for LOCK_SECONDS. With forming enabled, mutually sensed attractive actors of living,
     * matching cells that stay within LOCK_RANGE_FRACTION of the range for LOCK_SECONDS lock in; an anchored actor
     * takes no partner.
     * <p>
     * Two phases, so results do not depend on threads: in parallel per cell everything that changes only the own
     * actor (anchors, anchor forming, timer resets without a mutual partner), writing no bond links; then
     * sequentially in list order the pair cases (existing bonds, broken links, forming), which change two cells.
     */
    public static void updateBonds(final List<Cell> cells, final Environment environment, final double dt) {
        final SimulationConfig config = SimulationConfig.getInstance();
        final boolean anchors = config.isGroundAdhesion() && config.isGroundAnchors();
        final long present = LIST_MARKS.incrementAndGet();
        boolean unique = true;
        for (Cell cell : cells) {
            if (cell.getListMark() == present) unique = false; // listed twice: its actors must not run concurrently
            cell.setListMark(present);
        }
        final Pass pass = new Pass(config, environment, dt, present,
                anchors ? new TerrainIndex(environment.getBlockerList(), environment.getWidth(), maxReach(cells)) : null);
        final SensorActor[][] pairs = new SensorActor[cells.size()][];
        java.util.stream.IntStream range = java.util.stream.IntStream.range(0, cells.size());
        (unique ? range.parallel() : range).forEach(i -> pairs[i] = pass.ownActors(cells.get(i)));
        for (int i = 0; i < pairs.length; i++) if (pairs[i] != null) for (SensorActor actor : pairs[i]) pass.pair(cells.get(i), actor);
    }

    /** Settings and shared read-only data of one updateBonds call. */
    private static final class Pass {
        final boolean form, anchors, groundOnly;
        final SensorActorForceCellCalcService.Ground ground;
        final SensorActorForceCellCalcService.Affinity affinity;
        final double fade, dt;
        final long present;
        final TerrainIndex terrain;
        final Environment environment;

        Pass(SimulationConfig config, Environment environment, double dt, long present, TerrainIndex terrain) {
            form = config.isAdhesionBonds();
            anchors = terrain != null;
            groundOnly = config.isGroundAnchorsGroundOnly();
            ground = SensorActorForceCellCalcService.Ground.fromConfig(config.isAdhesionRecognition());
            affinity = config.isAdhesionRecognition() ? SensorActorForceCellCalcService.Affinity.fromConfig() : null;
            fade = remainsFade(config, dt);
            this.dt = dt; this.present = present; this.terrain = terrain; this.environment = environment;
        }

        /**
         * Phase 1: own actor only. Reads partners' bond links, sensing and forces, which no phase-1 worker writes.
         * Returns the actors left for the pair phase, or null.
         */
        SensorActor[] ownActors(Cell cell) {
            List<SensorActor> pairs = null;
            for (SensorActor actor : cell.getSensorActors()) {
                if (actor.getAnchor() != null) { anchored(cell, actor); continue; }
                SensorActor partner = actor.getBondPartner();
                if (partner != null) {
                    Cell other = partner.getParentCell();
                    boolean broken = other == null || other.getListMark() != present || partner.getBondPartner() != actor;
                    // Intact pairs are handled once, by the lower id.
                    if (broken || cell.getSensingId() <= other.getSensingId()) (pairs == null ? pairs = new ArrayList<>(2) : pairs).add(actor);
                    continue;
                }
                if (cell.isRemains()) continue;
                if (anchors && actor.getForceStrength() < 0 && anchoring(cell, actor)) continue;
                if (!form) { actor.setBondTimer(0); continue; }
                if (!(actor.getSensedActor() instanceof SensorActor candidate) || candidate.getBondPartner() != null
                        || candidate.getSensedActor() != actor) { actor.setBondTimer(0); continue; }
                Cell other = candidate.getParentCell();
                if (other == null || other == cell || other.isRemains() || other.getListMark() != present) { actor.setBondTimer(0); continue; }
                if (cell.getSensingId() > other.getSensingId()) continue; // the lower id carries the timer
                (pairs == null ? pairs = new ArrayList<>(2) : pairs).add(actor);
            }
            return pairs == null ? null : pairs.toArray(new SensorActor[0]);
        }

        /** Stage B (BODENHALT.md): same release rules as a bond; the anchor must still lie on terrain. */
        private void anchored(Cell cell, SensorActor actor) {
            if (cell.isRemains()) actor.fadeBond(fade);
            actor.setBondTimer(pushes(cell, actor) ? actor.getBondTimer() + dt : 0);
            if (!onTerrain(actor.getAnchor(), environment, terrain)
                    || actor.calcPosition().getShortestDirectionWithWrapAround(actor.getAnchor(), environment.getWidth(), environment.getHeight()).length() > reach(cell, cell)
                    || actor.getBondTimer() >= LOCK_SECONDS - 1e-9
                    || -actor.getBondStrength() < MIN_BOND_STRENGTH) actor.releaseBond();
        }

        /**
         * Stage B: like a partner actor, the nearest terrain must stay within half the field range. Resting cells
         * rarely have an actor inside a blocker, so touching is not required. True while the actor is anchoring.
         */
        private boolean anchoring(Cell cell, SensorActor actor) {
            Vector2D at = actor.calcPosition(), offset = null;
            Blocker nearest = null;
            final double lockRange = LOCK_RANGE_FRACTION * reach(cell, cell);
            for (Blocker blocker : terrain.near(at.getX())) {
                if (groundOnly && blocker.getBlockerType() != Blocker.BlockerType.GROUND) continue;
                // Farther blockers cannot be the anchor: the nearest one must lie within lock range.
                if (blocker.horizontalGap(at.getX(), environment.getWidth()) > lockRange + 1e-6) continue;
                Vector2D o = blocker.offsetToSurface(at, environment.getWidth(), environment.getHeight());
                if (offset == null || o.lengthSquared() < offset.lengthSquared()) { offset = o; nearest = blocker; }
            }
            if (nearest == null || offset.length() > lockRange || ground.affinity(actor.getType(), nearest) < LOCK_MIN_AFFINITY) return false;
            actor.setBondTimer(actor.getBondTimer() + dt);
            if (actor.getBondTimer() >= LOCK_SECONDS - 1e-9) actor.anchor(at.add(offset), actor.getForceStrength());
            return true;
        }

        /** Phase 2, in list order: an existing bond or a forming pair, re-checked on the current links. */
        void pair(Cell cell, SensorActor actor) {
            SensorActor partner = actor.getBondPartner();
            if (partner != null) {
                Cell other = partner.getParentCell();
                // Removed cells, unsaved partners and broken back links end the bond.
                if (other == null || other.getListMark() != present || partner.getBondPartner() != actor) {
                    actor.releaseBond();
                    return;
                }
                if (cell.getSensingId() > other.getSensingId()) return; // once per pair
                if (cell.isRemains() || other.isRemains()) { actor.fadeBond(fade); partner.fadeBond(fade); }
                boolean pushing = pushes(cell, actor) || pushes(other, partner);
                actor.setBondTimer(pushing ? actor.getBondTimer() + dt : 0);
                if (distance(actor, partner, environment) > reach(cell, other)
                        || actor.getBondTimer() >= LOCK_SECONDS - 1e-9
                        || -actor.getBondStrength() < MIN_BOND_STRENGTH) actor.releaseBond();
                return;
            }
            if (actor.getAnchor() != null) return;
            // An anchored candidate (possibly since phase 1) holds the terrain and takes no partner.
            if (!(actor.getSensedActor() instanceof SensorActor candidate) || candidate.getBondPartner() != null
                    || candidate.getAnchor() != null || candidate.getSensedActor() != actor) { actor.setBondTimer(0); return; }
            Cell other = candidate.getParentCell();
            boolean close = actor.getForceStrength() < 0 && candidate.getForceStrength() < 0
                    && (affinity == null || affinity.between(actor, candidate) >= LOCK_MIN_AFFINITY)
                    && distance(actor, candidate, environment) <= LOCK_RANGE_FRACTION * reach(cell, other);
            actor.setBondTimer(close ? actor.getBondTimer() + dt : 0);
            if (actor.getBondTimer() >= LOCK_SECONDS - 1e-9)
                SensorActor.bond(actor, candidate, Math.max(actor.getForceStrength(), candidate.getForceStrength()));
        }
    }

    /**
     * Stage 4: the daughter stays bonded to the mother's actor facing it, if that actor attracts.
     * The bond keeps the mother's current attraction; nothing else of the birth changes.
     */
    public static boolean bindAtBirth(final Cell parent, final Cell child, final Environment environment) {
        SensorActor mother = nearest(parent, child.getPosition(), environment);
        SensorActor daughter = nearest(child, parent.getPosition(), environment);
        if (mother == null || daughter == null || mother.getBondPartner() != null || !(mother.getForceStrength() < 0)) return false;
        SensorActor.bond(mother, daughter, mother.getForceStrength());
        return true;
    }

    private static SensorActor nearest(Cell cell, Vector2D target, Environment environment) {
        SensorActor best = null; double bestDistance = Double.POSITIVE_INFINITY;
        for (SensorActor actor : cell.getSensorActors()) {
            double d = actor.calcPosition().getShortestDirectionWithWrapAround(target, environment.getWidth(), environment.getHeight()).lengthSquared();
            if (d < bestDistance) { bestDistance = d; best = actor; }
        }
        return best;
    }

    /** Anchors loaded into another world, or whose blocker is gone, no longer lie on terrain. */
    static boolean onTerrain(Vector2D point, Environment environment) { return onTerrain(point, environment, null); }

    /** With the step's terrain index only blockers near x are candidates (all within 1e-6 of x are among them). */
    private static boolean onTerrain(Vector2D point, Environment environment, TerrainIndex terrain) {
        for (Blocker blocker : terrain == null ? environment.getBlockerList() : terrain.near(point.getX()))
            if (blocker.offsetToSurface(point, environment.getWidth(), environment.getHeight()).lengthSquared() <= 1e-12) return true;
        return false;
    }

    private static boolean pushes(Cell cell, SensorActor actor) {
        return !cell.isRemains() && !cell.isBrainPaused() && actor.getForceStrength() > 0;
    }

    /** Largest sensor radius of the step; bounds every lock range for the terrain index. */
    private static double maxReach(List<Cell> cells) {
        double max = 0;
        for (Cell cell : cells) max = Math.max(max, SensorActorForceCellCalcService.calcSensorRadius(cell.getRadiusSize(), cell.getSensorActors().size()));
        return max;
    }

    private static double reach(Cell a, Cell b) {
        return Math.max(SensorActorForceCellCalcService.calcSensorRadius(a.getRadiusSize(), a.getSensorActors().size()),
                SensorActorForceCellCalcService.calcSensorRadius(b.getRadiusSize(), b.getSensorActors().size()));
    }

    private static double distance(SensorActor a, SensorActor b, Environment environment) {
        return a.calcPosition().getShortestDirectionWithWrapAround(b.calcPosition(), environment.getWidth(), environment.getHeight()).length();
    }
}
