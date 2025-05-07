package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;

import java.util.List;

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
            }
        }
        for (int i = 0; i < cells.size(); i++) {
            Cell cell1 = cells.get(i);
            for (Cell cell2 : partitioner.getNeighbors(cell1)) {
                int j = cells.indexOf(cell2);
                if (j <= i) continue;
                processInteraction(cell1, cell2, deltaTime);
            }
        }
    }

    private static void processInteraction(final Cell cell1, final Cell cell2, final double deltaTime) {
        // reset any previous sensed references
        for (SensorActor actor : cell1.getSensorActors()) {
            actor.setSensedActor(null);
            actor.setSensedCell(null);
        }
        for (SensorActor actor : cell2.getSensorActors()) {
            actor.setSensedActor(null);
            actor.setSensedCell(null);
        }

        // Process interactions in one direction only
        for (SensorActor actor1 : cell1.getSensorActors()) {
            for (SensorActor actor2 : cell2.getSensorActors()) {
                // integrate sensing logic
                double senseValue1 = sense(actor1, actor2);
                if (senseValue1 != 0) {
                    actor1.setSensedActor(actor2);
                    actor1.setSensedCell(cell2);
                    actor2.setSensedActor(actor1);
                    actor2.setSensedCell(cell1);
                }

                // Calculate force in one direction and derive the opposite direction
                Vector2D force1to2 = calculateForceOn(actor1);
                Vector2D force2to1 = force1to2.multiply(-1);

                cell2.applyForce(force1to2, actor2.getCachedPosition(), deltaTime);
                cell1.applyForce(force2to1, actor1.getCachedPosition(), deltaTime);
            }
        }
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
     * Senses the types of nearby actors.
     * @param other The other sensor/actor point
     * @return The sensed type intensity between -1 and 1, or 0 if out of range
     */
    public static double sense(SensorActor sensorActor, SensorActor other) {
        double distance = sensorActor.getCachedPosition().distance(other.getCachedPosition());
        int totalSensors = sensorActor.getParentCell().getSensorActors().size();
        double chord = calcSensorRadius(sensorActor.getParentCell().getRadiusSize(), totalSensors);
        if (distance > chord) {
            return 0;
        }
        double intensity = 1.0D - (distance / chord);
        //double similarity = sensorActor.getType().similarity(other.getType());
        //double weight = 2.0 * similarity - 1.0; // map [0,1] to [-1,1]
        return intensity; // * weight;
    }

    public static double calcSensorRadius(final double radiusSize, final int totalSensors) {
        // Berechnung der vollen Sehnenlänge zwischen zwei benachbarten Sensoren
        return 2.0D * radiusSize * Math.sin(Math.PI / totalSensors);
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
