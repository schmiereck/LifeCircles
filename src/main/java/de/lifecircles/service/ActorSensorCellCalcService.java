package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;
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
        for (Cell cell : cells) {
            for (SensorActor actor : cell.getSensorActors()) {
                actor.updateCachedPosition();
                actor.setSensedActor(null);
                actor.setSensedCell(null);
            }
        }
        
        cells.parallelStream().forEach(calcCell -> {
            for (Cell otherCell : partitioner.getNeighbors(calcCell)) {
                if (calcCell != otherCell) {
                    processInteraction(calcCell, otherCell, deltaTime);
                }
            }
        });
    }

    private static void processInteraction(final Cell calcCell, final Cell otherCell, final double deltaTime) {
        double foundSenseForceValue = 0.0D;
        SensorActor foundOtherCellActor = null;
        SensorActor foundCalcCellActor = null;
        Vector2D foundDirection = null;

        // Process interactions in one direction only
        for (SensorActor calcCellActor : calcCell.getSensorActors()) {
            for (SensorActor otherCellActor : otherCell.getSensorActors()) {
                // Berechne die Kraft, die der otherCellActor auf calcCellActor ausübt
                Vector2D direction = calcCellActor.getCachedPosition().subtract(otherCellActor.getCachedPosition());
                double distance = direction.length();
                
                if (distance > 0.0D) {
                    double senseForceValue = sense(otherCellActor, calcCellActor); // Kraft von otherCellActor auf calcCellActor
                    if ((senseForceValue != 0.0D) && (senseForceValue > foundSenseForceValue)) {
                        foundSenseForceValue = senseForceValue;
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

            Vector2D forceOnCalcCell = foundDirection.normalize().multiply(foundSenseForceValue);
            calcCell.applyForce(forceOnCalcCell, foundCalcCellActor.getCachedPosition(), deltaTime);
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
        return 1.0D - (distance / maxSensorRadius);
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
        double chord = sensorActor.getParentCell().getRadiusSize() * Math.sin(Math.PI / totalSensors);
        if (distance > chord) {
            return 0;
        }
        double intensity = 1 - (distance / chord);
        double similarity = sensorActor.getType().similarity(other.getType());
        double weight = 2.0 * similarity - 1.0; // map [0,1] to [-1,1]
        return intensity * weight;
    }

    /**
     * Calculates the force vector this actor applies to another actor.
     * @return Force vector (direction and magnitude)
     */
    public static Vector2D calculateForceOn(SensorActor sensorActor) {
        SensorActor sensedActor = sensorActor.getSensedActor();
        if (sensedActor == null) {
            return new Vector2D(0, 0);
        }
        
        Vector2D direction = sensedActor.getCachedPosition().subtract(sensorActor.getCachedPosition());
        double distance = direction.length();
        if (distance == 0) {
            return new Vector2D(0, 0);
        }
        
        double weight = -sense(sensorActor, sensedActor);
        if (weight == 0) {
            return new Vector2D(0, 0);
        }
        
        return direction.normalize().multiply(weight);
    }
}
