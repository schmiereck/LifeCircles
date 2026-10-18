package de.lifecircles.service;

import de.lifecircles.model.Blocker;
import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

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
     * Stage 3 (ZELLVERBINDUNGEN.md), sequential and in list order, so results do not depend on threads.
     * Existing bonds fade with remains, tear beyond field range and release after a brain pushes on them
     * for LOCK_SECONDS. With forming enabled, mutually sensed attractive actors of living, matching cells
     * that stay within LOCK_RANGE_FRACTION of the range for LOCK_SECONDS lock in.
     */
    public static void updateBonds(final List<Cell> cells, final Environment environment, final double dt) {
        final SimulationConfig config = SimulationConfig.getInstance();
        final boolean form = config.isAdhesionBonds();
        final boolean anchors = config.isGroundAdhesion() && config.isGroundAnchors();
        final SensorActorForceCellCalcService.Ground ground = SensorActorForceCellCalcService.Ground.fromConfig(config.isAdhesionRecognition());
        final SensorActorForceCellCalcService.Affinity affinity = config.isAdhesionRecognition()
                ? SensorActorForceCellCalcService.Affinity.fromConfig() : null;
        final double fade = remainsFade(config, dt);
        final Set<Cell> present = Collections.newSetFromMap(new IdentityHashMap<>());
        present.addAll(cells);
        for (Cell cell : cells) {
            for (SensorActor actor : cell.getSensorActors()) {
                if (actor.getAnchor() != null) {
                    // Stage B (BODENHALT.md): same release rules as a bond; the anchor must still lie on terrain.
                    if (cell.isRemains()) actor.fadeBond(fade);
                    actor.setBondTimer(pushes(cell, actor) ? actor.getBondTimer() + dt : 0);
                    if (!onTerrain(actor.getAnchor(), environment)
                            || actor.calcPosition().getShortestDirectionWithWrapAround(actor.getAnchor(), environment.getWidth(), environment.getHeight()).length() > reach(cell, cell)
                            || actor.getBondTimer() >= LOCK_SECONDS - 1e-9
                            || -actor.getBondStrength() < MIN_BOND_STRENGTH) actor.releaseBond();
                    continue;
                }
                SensorActor partner = actor.getBondPartner();
                if (partner != null) {
                    Cell other = partner.getParentCell();
                    // Removed cells, unsaved partners and broken back links end the bond.
                    if (other == null || !present.contains(other) || partner.getBondPartner() != actor) {
                        actor.releaseBond();
                        continue;
                    }
                    if (cell.getSensingId() > other.getSensingId()) continue; // once per pair
                    if (cell.isRemains() || other.isRemains()) { actor.fadeBond(fade); partner.fadeBond(fade); }
                    boolean pushing = pushes(cell, actor) || pushes(other, partner);
                    actor.setBondTimer(pushing ? actor.getBondTimer() + dt : 0);
                    if (distance(actor, partner, environment) > reach(cell, other)
                            || actor.getBondTimer() >= LOCK_SECONDS - 1e-9
                            || -actor.getBondStrength() < MIN_BOND_STRENGTH) actor.releaseBond();
                    continue;
                }
                if (cell.isRemains()) continue;
                if (anchors && actor.getForceStrength() < 0) {
                    // Stage B: like a partner actor, the nearest terrain must stay within half the field range.
                    // Resting cells rarely have an actor inside a blocker, so touching is not required.
                    Vector2D at = actor.calcPosition(), offset = null;
                    Blocker nearest = null;
                    for (Blocker blocker : environment.getBlockerList()) {
                        if (config.isGroundAnchorsGroundOnly() && blocker.getBlockerType() != Blocker.BlockerType.GROUND) continue;
                        Vector2D o = blocker.offsetToSurface(at, environment.getWidth(), environment.getHeight());
                        if (offset == null || o.lengthSquared() < offset.lengthSquared()) { offset = o; nearest = blocker; }
                    }
                    if (nearest != null && offset.length() <= LOCK_RANGE_FRACTION * reach(cell, cell)
                            && ground.affinity(actor.getType(), nearest) >= LOCK_MIN_AFFINITY) {
                        actor.setBondTimer(actor.getBondTimer() + dt);
                        if (actor.getBondTimer() >= LOCK_SECONDS - 1e-9) actor.anchor(at.add(offset), actor.getForceStrength());
                        continue;
                    }
                }
                if (!form) { actor.setBondTimer(0); continue; }
                if (!(actor.getSensedActor() instanceof SensorActor candidate) || candidate.getBondPartner() != null
                        || candidate.getSensedActor() != actor) { actor.setBondTimer(0); continue; }
                Cell other = candidate.getParentCell();
                if (other == null || other == cell || other.isRemains() || !present.contains(other)) { actor.setBondTimer(0); continue; }
                if (cell.getSensingId() > other.getSensingId()) continue; // the lower id carries the timer
                boolean close = actor.getForceStrength() < 0 && candidate.getForceStrength() < 0
                        && (affinity == null || affinity.between(actor, candidate) >= LOCK_MIN_AFFINITY)
                        && distance(actor, candidate, environment) <= LOCK_RANGE_FRACTION * reach(cell, other);
                actor.setBondTimer(close ? actor.getBondTimer() + dt : 0);
                if (actor.getBondTimer() >= LOCK_SECONDS - 1e-9)
                    SensorActor.bond(actor, candidate, Math.max(actor.getForceStrength(), candidate.getForceStrength()));
            }
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
    static boolean onTerrain(Vector2D point, Environment environment) {
        for (Blocker blocker : environment.getBlockerList())
            if (blocker.offsetToSurface(point, environment.getWidth(), environment.getHeight()).lengthSquared() <= 1e-12) return true;
        return false;
    }

    private static boolean pushes(Cell cell, SensorActor actor) {
        return !cell.isRemains() && !cell.isBrainPaused() && actor.getForceStrength() > 0;
    }

    private static double reach(Cell a, Cell b) {
        return Math.max(SensorActorForceCellCalcService.calcSensorRadius(a.getRadiusSize(), a.getSensorActors().size()),
                SensorActorForceCellCalcService.calcSensorRadius(b.getRadiusSize(), b.getSensorActors().size()));
    }

    private static double distance(SensorActor a, SensorActor b, Environment environment) {
        return a.calcPosition().getShortestDirectionWithWrapAround(b.calcPosition(), environment.getWidth(), environment.getHeight()).length();
    }
}
