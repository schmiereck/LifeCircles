package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;
import java.util.List;
import java.util.ArrayList;

/** Former sensor/actor interaction algorithm (before 24.10.2026), kept as bit-exact reference for the optimized one. */
final class SensorActorForceReference {
    private record Candidate(Cell cell, double range, double scale, double typeAffinity) {}

    static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner,
                                           final Environment environment, final SensorActorForceCellCalcService.Affinity affinity, final SensorActorForceCellCalcService.Ground ground) {
        final double width = environment.getWidth();
        final double height = environment.getHeight();
        final double softening = SimulationConfig.getInstance().getActorFieldSofteningFraction();
        final double bondFactor = SimulationConfig.getInstance().getBondFieldFactor();
        final double maxRadiusSquared = Math.pow(SimulationConfig.getInstance().getCellMaxRadiusSize(), 2);
        double maxRange = 0;
        if (ground != null) for (Cell cell : cells) maxRange = Math.max(maxRange, SensorActorForceCellCalcService.calcSensorRadius(cell.getRadiusSize(), cell.getSensorActors().size()));
        final TerrainIndex terrain = ground == null ? null : new TerrainIndex(environment.getBlockerList(), width, maxRange);
        cells.parallelStream().forEach(cell -> {
            for (SensorActor actor : cell.getSensorActors()) {
                actor.updateCachedPosition();
                actor.setSensedCell(null);
                actor.setSensedActor(null);
            }
        });

        // Workers write only their own cell. Reverse traversal computes the
        // equal and opposite reaction at the other cell's own actor.
        cells.parallelStream().forEach(cell -> {
            final double ownRange = SensorActorForceCellCalcService.calcSensorRadius(cell.getRadiusSize(), cell.getSensorActors().size());
            final List<Candidate> candidates = new ArrayList<>();
            // Broad phase once per directed cell pair, preserving neighbor and force summation order.
            for (Cell other : partitioner.getNeighbors(cell)) {
                if (other == cell) continue;
                double otherRange = SensorActorForceCellCalcService.calcSensorRadius(other.getRadiusSize(), other.getSensorActors().size());
                double cutoff = cell.getRadiusSize() + other.getRadiusSize() + Math.max(ownRange, otherRange);
                if (cell.getPosition().getShortestDirectionWithWrapAround(other.getPosition(), width, height)
                        .lengthSquared() <= cutoff * cutoff)
                    candidates.add(new Candidate(other, otherRange, cell.getRadiusSize() * other.getRadiusSize() / maxRadiusSquared,
                            affinity == null ? 1 : affinity.of(cell.getType(), other.getType())));
            }
            for (SensorActor actor : cell.getSensorActors()) {
                double nearest = Double.POSITIVE_INFINITY;
                Blocker touchingBlocker = null;
                for (Blocker blocker : environment.getBlockerList()) {
                    if (blocker.containsPoint(actor.getCachedPosition(), width, height)
                            && (touchingBlocker == null || blocker.getSensingId() < touchingBlocker.getSensingId())) {
                        touchingBlocker = blocker;
                    }
                }
                if (touchingBlocker != null) {
                    actor.setSensedCell(touchingBlocker);
                    actor.setSensedActor(touchingBlocker);
                }
                // Blockers do not move: the reaction goes into the terrain. Scale as for a maximum-size partner.
                final double groundScale = cell.getRadiusSize() / Math.sqrt(maxRadiusSquared);
                if (actor.getAnchor() != null) {
                    // An anchor replaces the surface fields with its frozen attraction, like a bond (sent and received).
                    Vector2D towardAnchor = actor.getCachedPosition().getShortestDirectionWithWrapAround(actor.getAnchor(), width, height);
                    double d = towardAnchor.length();
                    if (d > 1e-12 && d < ownRange) {
                        double strength = 2 * actor.getBondStrength() * bondFactor * (1 - d / ownRange) / Math.max(d, ownRange * softening);
                        cell.applyForce(towardAnchor.multiply(-strength * groundScale), actor.getCachedPosition());
                    }
                } else if (ground != null) {
                    double emitted = Math.min(0, actor.getForceStrength());
                    for (Blocker blocker : terrain.near(actor.getCachedPosition().getX())) {
                        double strength = emitted * ground.affinity(actor.getType(), blocker) + ground.stickiness();
                        if (strength == 0) continue;
                        Vector2D towardSurface = blocker.offsetToSurface(actor.getCachedPosition(), width, height);
                        double d = towardSurface.length();
                        if (d <= 1e-12 || d >= ownRange) continue;
                        strength *= (1 - d / ownRange) / Math.max(d, ownRange * softening);
                        cell.applyForce(towardSurface.multiply(-strength * groundScale), actor.getCachedPosition());
                    }
                }
                for (Candidate candidate : candidates) {
                    final Cell other = candidate.cell();
                    final double otherRange = candidate.range();
                    final double scale = candidate.scale();
                    for (SensorActor otherActor : other.getSensorActors()) {
                        final Vector2D towardOtherActor = actor.getCachedPosition().getShortestDirectionWithWrapAround(
                                otherActor.getCachedPosition(), width, height);
                        final double distance = towardOtherActor.length();
                        if (touchingBlocker == null && distance <= otherRange
                                && isBetterTarget(distance, nearest, other, otherActor, actor)) {
                            nearest = distance;
                            actor.setSensedCell(other);
                            actor.setSensedActor(otherActor);
                        }
                        if (distance <= 1e-12) continue;
                        // Received field plus the reaction to our emitted field.
                        double strength = 0;
                        double received = otherActor.getForceStrength(), emitted = actor.getForceStrength();
                        if (actor.getBondPartner() == otherActor) {
                            // A bond replaces this pair's fields with its frozen, already recognised attraction.
                            received = actor.getBondStrength() * bondFactor; emitted = received;
                        } else if (affinity != null && (received < 0 || emitted < 0)) {
                            double a = candidate.typeAffinity() * affinity.of(actor.getType(), otherActor.getType());
                            received = affinity.effective(received, a);
                            emitted = affinity.effective(emitted, a);
                        }
                        if (distance < otherRange) strength += received * (1 - distance / otherRange)
                                / Math.max(distance, otherRange * softening);
                        if (distance < ownRange) strength += emitted * (1 - distance / ownRange)
                                / Math.max(distance, ownRange * softening);
                        if (strength != 0) {
                            // towardOtherActor points TO the other actor. The minus sign
                            // makes negative strength attract and positive strength repel.
                            // A finite linear core removes the direction jump at contact.
                            // Each emitter uses its own range, preserving pair symmetry.
                            cell.applyForce(towardOtherActor.multiply(-strength * scale), actor.getCachedPosition());
                        }
                    }
                }
            }
        });
    }

    /** Total ordering: distance, then stable cell identity, then fixed actor angle. */
    private static boolean isBetterTarget(double distance, double nearest, Cell candidateCell,
                                          SensorActor candidateActor, SensorActor sensor) {
        int byDistance = Double.compare(distance, nearest);
        if (byDistance != 0) return byDistance < 0;
        if (!(sensor.getSensedCell() instanceof Cell currentCell)) return true;
        int byCell = Long.compare(candidateCell.getSensingId(), currentCell.getSensingId());
        if (byCell != 0) return byCell < 0;
        return Double.compare(candidateActor.getAngleOnCell(),
                ((SensorActor) sensor.getSensedActor()).getAngleOnCell()) < 0;
    }
}
