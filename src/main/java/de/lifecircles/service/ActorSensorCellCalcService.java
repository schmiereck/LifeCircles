package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.PartitioningStrategy;

import java.util.List;

/**
 * Service responsible for processing sensor-actor interactions between cells.
 */
public class ActorSensorCellCalcService {

    private static final double INTERACTION_FORCE = 32.0D * 2.0D * 1.0D;

    /**
     * Processes interactions between sensor/actor points of all cells.
     *
     * @param cells     the list of cells to process
     * @param deltaTime the time elapsed since last update
     */
    public static void processInteractions(List<Cell> cells, double deltaTime) {
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
    public static void processInteractions(List<Cell> cells, double deltaTime, PartitioningStrategy partitioner) {
        partitioner.build(cells);
        for (int i = 0; i < cells.size(); i++) {
            Cell cell1 = cells.get(i);
            for (Cell cell2 : partitioner.getNeighbors(cell1)) {
                int j = cells.indexOf(cell2);
                if (j <= i) continue;
                processInteraction(cell1, cell2, deltaTime);
            }
        }
    }

    private static void processInteraction(Cell cell1, Cell cell2, double deltaTime) {
        for (SensorActor actor1 : cell1.getSensorActors()) {
            for (SensorActor actor2 : cell2.getSensorActors()) {
                // Calculate and apply forces between sensor actors
                Vector2D force1to2 = calculateForceOn(actor1, actor2);
                Vector2D force2to1 = calculateForceOn(actor2, actor1);

                cell2.applyForce(force1to2, actor2.getPosition(), deltaTime);
                cell1.applyForce(force2to1, actor1.getPosition(), deltaTime);
            }
        }
    }

    /**
     * Senses the types of nearby actors.
     * @param other The other sensor/actor point
     * @return The sensed type intensity between -1 and 1, or 0 if out of range
     */
    public static double sense(SensorActor sensorActor, SensorActor other) {
        double distance = sensorActor.getPosition().distance(other.getPosition());
        int totalSensors = sensorActor.getParentCell().getSensorActors().size();
        double chord = sensorActor.getParentCell().getSize() * Math.sin(Math.PI / totalSensors);
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
     * @param other The other sensor/actor point
     * @return Force vector (direction and magnitude)
     */
    public static Vector2D calculateForceOn(SensorActor sensorActor, SensorActor other) {
        Vector2D direction = other.getPosition().subtract(sensorActor.getPosition());
        double distance = direction.length();
        if (distance == 0) {
            return new Vector2D(0, 0);
        }
        double weight = -sense(sensorActor, other) * INTERACTION_FORCE;
        if (weight == 0) {
            return new Vector2D(0, 0);
        }
        return direction.normalize().multiply(weight);
    }
}
