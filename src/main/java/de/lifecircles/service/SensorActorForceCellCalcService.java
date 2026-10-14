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
                for (Blocker blocker : environment.getBlockerList()) {
                    if (blocker.containsPoint(actor.getCachedPosition(), width, height)) {
                        actor.setSensedCell(blocker);
                        actor.setSensedActor(blocker);
                        nearest = -1; // A touching blocker has sensing priority.
                        break;
                    }
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
                        if (distance <= otherRange && distance < nearest) {
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
}
