package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;

import java.util.List;
import java.util.Objects;

/**
 * Service responsible for processing sensor-actor interactions between cells.
 */
public class SensorActorForceCellCalcService {

    /**
     * Optimized processing of sensor/actor interactions using a partitioning strategy.
     */
    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner) {
        // cache positions for all sensorActors in this simulation step
        for (final Cell cell : cells) {
            for (final SensorActor actor : cell.getSensorActors()) {
                actor.updateCachedPosition();
                actor.setSensedCell(null);
                actor.setSensedActor(null);
            }
        }
        
        // Prüfe Blocker-Kollisionen
        checkBlockerCollisions(cells);
        
        cells.parallelStream().forEach(calcCell -> {
            for (final Cell otherCell : partitioner.getNeighbors(calcCell)) {
                if (calcCell != otherCell) {
                    processInteraction(calcCell, otherCell);
                }
            }
        });
    }

    /**
     * Überprüft, ob Sensoren mit Blockern kollidieren und setzt entsprechende Sensable-Objekte
     */
    private static void checkBlockerCollisions(List<Cell> cells) {
        // Hole die Blocker aus der Environment-Instanz
        Environment environment = Environment.getInstance();
        if (environment == null) return;
        
        List<Blocker> blockers = environment.getBlockers();
        if (blockers == null || blockers.isEmpty()) return;
        
        // Prüfe für jeden Sensor, ob er einen Blocker berührt
        cells.parallelStream().forEach(cell -> {
            for (final SensorActor sensor : cell.getSensorActors()) {
                final Vector2D sensorPos = sensor.getCachedPosition();
                if (sensorPos != null) {
                    for (Blocker blocker : blockers) {
                        if (blocker.containsPoint(sensorPos)) {
                            // Setze den Blocker als wahrgenommenes Objekt
                            // Der SensorActor nimmt Blocker als Zelle wahr
                            sensor.setSensedCell(blocker);
                            sensor.setSensedActor(blocker);
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Berechne die Kraft, die der otherCellActor auf calcCellActor ausübt.
     */
    private static void processInteraction(final Cell calcCell, final Cell otherCell) {
        final Vector2D delta = calcCell.getPosition().subtract(otherCell.getPosition());
        final double cellDistance = delta.length();
        final double combinedRadius = Math.max(otherCell.getRadiusSize(), calcCell.getRadiusSize());
        // Nur ausführen, wenn die Zellen sich nicht inneinander befinden.
        if (cellDistance > combinedRadius) {
            double foundForceStrength = 0.0D;
            SensorActor foundOtherCellActor = null;
            SensorActor foundCalcCellActor = null;
            Vector2D foundDirection = null;

            // Process interactions in one direction only
            for (final SensorActor calcCellActor : calcCell.getSensorActors()) {
                // Wenn der Sensor bereits etwas wahrnimmt (z.B. einen Blocker), überspringen
                if (calcCellActor.getSensedCell() != null) {
                    continue;
                }

                for (SensorActor otherCellActor : otherCell.getSensorActors()) {
                    // Berechne die Kraft, die der otherCellActor auf calcCellActor ausübt
                    final Vector2D direction = calcCellActor.getCachedPosition().subtract(otherCellActor.getCachedPosition());
                    final double distance = direction.length();

                    if (distance > 0.0D) {
                        // Kraft von otherCellActor auf calcCellActor
                        final double forceStrength = otherCellActor.getForceStrength(); // Kraftstärke (positiv/negativ)
                        final double senseForceValue; //= sense(otherCellActor, calcCellActor);
                        // sense(SensorActor sensorActor, SensorActor otherSensorActor)
                        //double distance = otherCellActor.getCachedPosition().distance(calcCellActor.getCachedPosition());
                        int totalSensors = otherCellActor.getParentCell().getSensorActors().size();
                        double maxSensorRadius = calcSensorRadius(otherCellActor.getParentCell().getRadiusSize(), totalSensors);
                        if (distance > maxSensorRadius) {
                            senseForceValue = 0.0D;
                        } else {
                            senseForceValue = (distance / maxSensorRadius);
                        }

                        final double totalForceStrength = senseForceValue * forceStrength; // Berücksichtige Richtung und Stärke

                        if ((senseForceValue != 0.0D) && (Math.abs(totalForceStrength) > Math.abs(foundForceStrength))) {
                            foundForceStrength = totalForceStrength;
                            foundCalcCellActor = calcCellActor;
                            foundOtherCellActor = otherCellActor;
                            foundDirection = direction;

                            if (Objects.nonNull(foundOtherCellActor)) {
                                foundCalcCellActor.setSensedCell(otherCell);
                                foundCalcCellActor.setSensedActor(foundOtherCellActor);

                                final Vector2D forceOnCalcCell = foundDirection.normalize().multiply(foundForceStrength);
                                calcCell.applyForce(forceOnCalcCell, foundCalcCellActor.getCachedPosition());
                            }

                        }
                    }
                }
            }
//            if (Objects.nonNull(foundOtherCellActor)) {
//                foundCalcCellActor.setSensedActor(foundOtherCellActor);
//                foundCalcCellActor.setSensedCell(otherCell);
//
//                final double usedForceStrength;
//                //// Abstoßung?
//                //if (foundForceStrength > 0.0D) {
//                //    usedForceStrength = foundForceStrength * 1.75D;
//                //} else {
//                    usedForceStrength = foundForceStrength;
//                //}
//                Vector2D forceOnCalcCell = foundDirection.normalize().multiply(usedForceStrength);
//                calcCell.applyForce(forceOnCalcCell, foundCalcCellActor.getCachedPosition(), deltaTime);
//            }
        }
    }

    /**
     * Senses the force of given other Actor to the given Actor.
     * @param sensorActor is the Actor the force applies to.
     * @param otherSensorActor is the Actor the force applies from.
     * @return The sensed force intensity between >0 and 1, or 0 if out of range
     */
    public static double sense(SensorActor sensorActor, SensorActor otherSensorActor) {
        double distance = sensorActor.getCachedPosition().distance(otherSensorActor.getCachedPosition());
        int totalSensors = sensorActor.getParentCell().getSensorActors().size();
        double maxSensorRadius = calcSensorRadius(sensorActor.getParentCell().getRadiusSize(), totalSensors);
        if (distance > maxSensorRadius) {
            return 0.0D;
        }
        return (distance / maxSensorRadius);
    }

    public static double calcSensorRadius(final double radiusSize, final int totalSensors) {
        // Berechnung der vollen Sehnenlänge zwischen zwei benachbarten Sensoren
        return 2.0D * radiusSize * Math.sin(Math.PI / totalSensors);
    }

    /**
     * Senses the types of nearby actors.
     * @param other The other sensor/actor point
     * @return The sensed type intensity between -1 and 1, or 0 if out of range
     */
    public static double senseWithType(SensorActor sensorActor, SensorActor other) {
        double distance = sensorActor.getCachedPosition().distance(other.getCachedPosition());
        int totalSensors = sensorActor.getParentCell().getSensorActors().size();
        double chord = calcSensorRadius(sensorActor.getParentCell().getRadiusSize(), totalSensors);
        if (distance > chord) {
            return 0;
        }
        double intensity = 1 - (distance / chord);
        double similarity = sensorActor.getType().similarity(other.getType());
        double weight = 2.0 * similarity - 1.0; // map [0,1] to [-1,1]
        return intensity * weight;
    }
}
