package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;
import java.util.List;
import java.util.ArrayList;

/** Sensor fields use negative strength for attraction and positive strength for repulsion. */
public class SensorActorForceCellCalcService {
    private record Candidate(Cell cell, double range, double scale, double typeAffinity) {}

    /**
     * Key-lock adhesion: attraction acts in full only between similar actor colours on cells of
     * similar body type; toward a foreign partner the rest of it acts as bounded repulsion.
     */
    public record Affinity(double sharpness, double rejection) {
        public static Affinity fromConfig() {
            SimulationConfig config = SimulationConfig.getInstance();
            return new Affinity(config.getAdhesionSharpness(), config.getAdhesionRejection());
        }
        double of(CellType a, CellType b) { return Math.pow(Math.max(0, a.similarity(b)), sharpness); }
        /** Symmetric in both partners, so action and reaction stay equal. */
        double effective(double strength, double affinity) {
            return strength < 0 ? strength * affinity - strength * (1 - affinity) * rejection : strength;
        }
    }
    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner) {
        processInteractions(cells, partitioner, Environment.getInstance());
    }

    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner,
                                           final Environment environment) {
        processInteractions(cells, partitioner, environment, null);
    }

    /** affinity null keeps the original colour-blind fields. */
    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner,
                                           final Environment environment, final Affinity affinity) {
        final double width = environment.getWidth();
        final double height = environment.getHeight();
        final double softening = SimulationConfig.getInstance().getActorFieldSofteningFraction();
        final double maxRadiusSquared = Math.pow(SimulationConfig.getInstance().getCellMaxRadiusSize(), 2);
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
            final double ownRange = calcSensorRadius(cell.getRadiusSize(), cell.getSensorActors().size());
            final List<Candidate> candidates = new ArrayList<>();
            // Broad phase once per directed cell pair, preserving neighbor and force summation order.
            for (Cell other : partitioner.getNeighbors(cell)) {
                if (other == cell) continue;
                double otherRange = calcSensorRadius(other.getRadiusSize(), other.getSensorActors().size());
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
                        if (affinity != null && (received < 0 || emitted < 0)) {
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

    public static double calcSensorRadius(final double radiusSize, final int totalSensors) {
        return 2.0D * radiusSize * Math.sin(Math.PI / totalSensors) * SimulationConfig.cellActorMaxFieldRadiusFactor;
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
