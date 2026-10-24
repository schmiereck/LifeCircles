package de.lifecircles.service;

import de.lifecircles.model.*;
import java.util.*;

/** Former bond and anchor update and blocker collision (before 24.10.2026), kept as bit-exact references. */
final class IntegrationReference {
    static void updateBonds(final List<Cell> cells, final Environment environment, final double dt) {
        final SimulationConfig config = SimulationConfig.getInstance();
        final boolean form = config.isAdhesionBonds();
        final boolean anchors = config.isGroundAdhesion() && config.isGroundAnchors();
        final SensorActorForceCellCalcService.Ground ground = SensorActorForceCellCalcService.Ground.fromConfig(config.isAdhesionRecognition());
        final SensorActorForceCellCalcService.Affinity affinity = config.isAdhesionRecognition()
                ? SensorActorForceCellCalcService.Affinity.fromConfig() : null;
        final double fade = CellAdhesionService.remainsFade(config, dt);
        final Set<Cell> present = Collections.newSetFromMap(new IdentityHashMap<>());
        present.addAll(cells);
        final TerrainIndex terrain = anchors ? new TerrainIndex(environment.getBlockerList(), environment.getWidth(), maxReach(cells)) : null;
        for (Cell cell : cells) {
            for (SensorActor actor : cell.getSensorActors()) {
                if (actor.getAnchor() != null) {
                    // Stage B (BODENHALT.md): same release rules as a bond; the anchor must still lie on terrain.
                    if (cell.isRemains()) actor.fadeBond(fade);
                    actor.setBondTimer(pushes(cell, actor) ? actor.getBondTimer() + dt : 0);
                    if (!onTerrain(actor.getAnchor(), environment)
                            || actor.calcPosition().getShortestDirectionWithWrapAround(actor.getAnchor(), environment.getWidth(), environment.getHeight()).length() > reach(cell, cell)
                            || actor.getBondTimer() >= CellAdhesionService.LOCK_SECONDS - 1e-9
                            || -actor.getBondStrength() < CellAdhesionService.MIN_BOND_STRENGTH) actor.releaseBond();
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
                            || actor.getBondTimer() >= CellAdhesionService.LOCK_SECONDS - 1e-9
                            || -actor.getBondStrength() < CellAdhesionService.MIN_BOND_STRENGTH) actor.releaseBond();
                    continue;
                }
                if (cell.isRemains()) continue;
                if (anchors && actor.getForceStrength() < 0) {
                    // Stage B: like a partner actor, the nearest terrain must stay within half the field range.
                    // Resting cells rarely have an actor inside a blocker, so touching is not required.
                    Vector2D at = actor.calcPosition(), offset = null;
                    Blocker nearest = null;
                    final double lockRange = CellAdhesionService.LOCK_RANGE_FRACTION * reach(cell, cell);
                    for (Blocker blocker : terrain.near(at.getX())) {
                        if (config.isGroundAnchorsGroundOnly() && blocker.getBlockerType() != Blocker.BlockerType.GROUND) continue;
                        // Farther blockers cannot be the anchor: the nearest one must lie within lock range.
                        if (blocker.horizontalGap(at.getX(), environment.getWidth()) > lockRange + 1e-6) continue;
                        Vector2D o = blocker.offsetToSurface(at, environment.getWidth(), environment.getHeight());
                        if (offset == null || o.lengthSquared() < offset.lengthSquared()) { offset = o; nearest = blocker; }
                    }
                    if (nearest != null && offset.length() <= lockRange
                            && ground.affinity(actor.getType(), nearest) >= CellAdhesionService.LOCK_MIN_AFFINITY) {
                        actor.setBondTimer(actor.getBondTimer() + dt);
                        if (actor.getBondTimer() >= CellAdhesionService.LOCK_SECONDS - 1e-9) actor.anchor(at.add(offset), actor.getForceStrength());
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
                        && (affinity == null || affinity.between(actor, candidate) >= CellAdhesionService.LOCK_MIN_AFFINITY)
                        && distance(actor, candidate, environment) <= CellAdhesionService.LOCK_RANGE_FRACTION * reach(cell, other);
                actor.setBondTimer(close ? actor.getBondTimer() + dt : 0);
                if (actor.getBondTimer() >= CellAdhesionService.LOCK_SECONDS - 1e-9)
                    SensorActor.bond(actor, candidate, Math.max(actor.getForceStrength(), candidate.getForceStrength()));
            }
        }
    }

    /** Anchors loaded into another world, or whose blocker is gone, no longer lie on terrain. */
    private static boolean onTerrain(Vector2D point, Environment environment) {
        for (Blocker blocker : environment.getBlockerList())
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

    static void handleBlockerCollisions(final Cell cell, final List<Blocker> blockers, final Environment world) {
        for (Blocker blocker : blockers) {
            if (blocker == null) continue;
            Vector2D position = cell.getPosition();
            Vector2D corrected = blocker.getCellCenterOutside(position, cell.getRadiusSize(), world.getWidth(), world.getHeight());
            Vector2D correction = corrected.subtract(position);
            if (correction.lengthSquared() <= 1e-24) continue;
            Vector2D normal = correction.normalize();
            cell.setPosition(corrected);
            Vector2D velocity = cell.getVelocity();
            double inwardSpeed = velocity.getX() * normal.getX() + velocity.getY() * normal.getY();
            if (inwardSpeed < 0) cell.setVelocity(velocity.subtract(normal.multiply(inwardSpeed)));
        }
    }
}
