package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;
import java.util.List;

/** Sensor fields use negative strength for attraction and positive strength for repulsion. */
public class SensorActorForceCellCalcService {
    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner) {
        processInteractions(cells, partitioner, Environment.getInstance());
    }

    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner,
                                           final Environment environment) {
        final double width = environment.getWidth();
        final double height = environment.getHeight();
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
                for (Cell other : partitioner.getNeighbors(cell)) {
                    if (other == cell) continue;
                    final double otherRange = calcSensorRadius(other.getRadiusSize(), other.getSensorActors().size());
                    final double cutoff = cell.getRadiusSize() + other.getRadiusSize() + Math.max(ownRange, otherRange);
                    if (cell.getPosition().getShortestDirectionWithWrapAround(other.getPosition(), width, height)
                            .lengthSquared() > cutoff * cutoff) continue;
                    final double scale = cell.getRadiusSize() * other.getRadiusSize()
                            / Math.pow(SimulationConfig.getInstance().getCellMaxRadiusSize(), 2);
                    for (SensorActor otherActor : other.getSensorActors()) {
                        final Vector2D direction = actor.getCachedPosition().getShortestDirectionWithWrapAround(
                                otherActor.getCachedPosition(), width, height);
                        final double distance = direction.length();
                        if (touchingBlocker == null && distance <= otherRange
                                && isBetterTarget(distance, nearest, other, otherActor, actor)) {
                            nearest = distance;
                            actor.setSensedCell(other);
                            actor.setSensedActor(otherActor);
                        }
                        if (distance <= 1e-12) continue;
                        // Received field plus the reaction to our emitted field.
                        double strength = 0;
                        if (distance < otherRange) strength += otherActor.getForceStrength() * (1 - distance / otherRange);
                        if (distance < ownRange) strength += actor.getForceStrength() * (1 - distance / ownRange);
                        if (strength != 0) {
                            cell.applyForce(direction.multiply(-strength * scale / distance), actor.getCachedPosition());
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
