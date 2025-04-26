package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.SpatialGrid;
import java.util.List;

/**
 * Service responsible for calculating repulsive forces between cells.
 */
public class RepulsionCellCalcService {
    private static final double REPULSION_STRENGTH = 50.0;

    /**
     * Applies repulsive forces between overlapping cells.
     * @param cells list of cells
     * @param deltaTime time step
     */
    public static void processRepulsiveForces(List<Cell> cells, double deltaTime) {
        for (int i = 0; i < cells.size(); i++) {
            Cell cell1 = cells.get(i);
            for (int j = i + 1; j < cells.size(); j++) {
                Cell cell2 = cells.get(j);
                Vector2D delta = cell2.getPosition().subtract(cell1.getPosition());
                double distance = delta.length();
                double combinedRadius = cell1.getSize() / 2 + cell2.getSize() / 2;
                if (distance < combinedRadius) {
                    double overlap = combinedRadius - distance;
                    double forceMagnitude = REPULSION_STRENGTH * overlap;
                    Vector2D direction = delta.divide(distance);
                    Vector2D force = direction.multiply(forceMagnitude);
                    cell1.applyForce(force.multiply(-1), cell1.getPosition(), deltaTime);
                    cell2.applyForce(force, cell2.getPosition(), deltaTime);
                }
            }
        }
    }

    /**
     * Optimized repulsion using partitioning strategy.
     */
    public static void processRepulsiveForces(List<Cell> cells, double deltaTime, PartitioningStrategy partitioner) {
        for (Cell cell1 : cells) {
            List<Cell> neighbors = partitioner.getNeighbors(cell1);
            for (Cell cell2 : neighbors) {
                if (cell2 == cell1) continue;
                Vector2D delta = cell2.getPosition().subtract(cell1.getPosition());
                double distance = delta.length();
                double combinedRadius = cell1.getSize() / 2 + cell2.getSize() / 2;
                if (distance < combinedRadius) {
                    double overlap = combinedRadius - distance;
                    double forceMagnitude = REPULSION_STRENGTH * overlap;
                    Vector2D direction = delta.divide(distance);
                    Vector2D force = direction.multiply(forceMagnitude);
                    cell1.applyForce(force.multiply(-1), cell1.getPosition(), deltaTime);
                    cell2.applyForce(force, cell2.getPosition(), deltaTime);
                }
            }
        }
    }
}
