package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;

import java.util.List;
import java.util.Objects;

/**
 * Service responsible for processing sensor-actor interactions between cells.
 */
public class ActorSensorCellCalcService {

    /**
     * Processes interactions between sensor/actor points of all cells.
     *
     * @param cells     the list of cells to process
     * @param deltaTime the time elapsed since last update
     */
    public static void processInteractions(final List<Cell> cells, final double deltaTime) {
        // cache positions for all sensorActors in this simulation step
        for (Cell cell : cells) {
            for (SensorActor actor : cell.getSensorActors()) {
                actor.updateCachedPosition();
            }
        }
        for (int i = 0; i < cells.size(); i++) {
            Cell cell1 = cells.get(i);
            for (int j = i + 1; j < cells.size(); j++) {
                Cell cell2 = cells.get(j);
                processInteraction(cell1, cell2, deltaTime);
            }
        }
    }

    /**
     * Optimized processing of sensor/actor interactions using a partitioning strategy.
     */
    public static void processInteractions(final List<Cell> cells, final double deltaTime, final PartitioningStrategy partitioner) {
        // cache positions for all sensorActors in this simulation step
        for (final Cell cell : cells) {
            for (final SensorActor actor : cell.getSensorActors()) {
                actor.updateCachedPosition();
                actor.setSensedActor(null);
                actor.setSensedCell(null);
            }
        }
        
        // Prüfe Blocker-Kollisionen
        checkBlockerCollisions(cells);
        
        cells.parallelStream().forEach(calcCell -> {
            for (final Cell otherCell : partitioner.getNeighbors(calcCell)) {
                if (calcCell != otherCell) {
                    processInteraction(calcCell, otherCell, deltaTime);
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
        for (Cell cell : cells) {
            for (SensorActor sensor : cell.getSensorActors()) {
                Vector2D sensorPos = sensor.getCachedPosition();
                if (sensorPos != null) {
                    for (Blocker blocker : blockers) {
                        if (blocker.containsPoint(sensorPos)) {
                            // Setze den Blocker als wahrgenommenes Objekt
                            sensor.setSensedCell(blocker);
                            // Der SensorActor nimmt Blocker als Zelle wahr
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void processInteraction(final Cell calcCell, final Cell otherCell, final double deltaTime) {
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
            for (SensorActor calcCellActor : calcCell.getSensorActors()) {
                // Wenn der Sensor bereits etwas wahrnimmt (z.B. einen Blocker), überspringen
                if (calcCellActor.getSensedCell() != null) {
                    continue;
                }

                for (SensorActor otherCellActor : otherCell.getSensorActors()) {
                    // Berechne die Kraft, die der otherCellActor auf calcCellActor ausübt
                    Vector2D direction = calcCellActor.getCachedPosition().subtract(otherCellActor.getCachedPosition());
                    double distance = direction.length();

                    if (distance > 0.0D) {
                        // Kraft von otherCellActor auf calcCellActor
                        double forceStrength = otherCellActor.getForceStrength(); // Kraftstärke (positiv/negativ)
                        double senseForceValue = sense(otherCellActor, calcCellActor);
                        double totalForceStrength = senseForceValue * forceStrength; // Berücksichtige Richtung und Stärke

                        if ((senseForceValue != 0.0D) && (Math.abs(totalForceStrength) > Math.abs(foundForceStrength))) {
                            foundForceStrength = totalForceStrength;
                            foundCalcCellActor = calcCellActor;
                            foundOtherCellActor = otherCellActor;
                            foundDirection = direction;
                        }
                    }
                }
            }
            if (Objects.nonNull(foundOtherCellActor)) {
                foundCalcCellActor.setSensedActor(foundOtherCellActor);
                foundCalcCellActor.setSensedCell(otherCell);

                Vector2D forceOnCalcCell = foundDirection.normalize().multiply(foundForceStrength);
                calcCell.applyForce(forceOnCalcCell, foundCalcCellActor.getCachedPosition(), deltaTime);
            }
        }
    }

    /**
     * Senses the force of given other Actor to the given Actor.
     * @param sensorActor is the Actor the force applies to.
     * @param otherSensorActor is the Actor the force applies from.
     * @return The sensed force intensity between -1 and 1, or 0 if out of range
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
