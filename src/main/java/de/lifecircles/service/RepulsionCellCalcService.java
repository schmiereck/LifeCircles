package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;

import java.util.List;

/**
 * Service responsible for calculating repulsive forces between cells.
 */
public class RepulsionCellCalcService {
    /**
     * Optimized repulsion using partitioning strategy.
     */
    public static void processRepulsiveForces(final List<Cell> cells, final double deltaTime, final PartitioningStrategy partitioner) {
        for (Cell cell1 : cells) {
            List<Cell> neighbors = partitioner.getNeighbors(cell1);
            for (Cell cell2 : neighbors) {
                if (cell2 == cell1) continue;
                Vector2D delta = cell2.getPosition().subtract(cell1.getPosition());
                double distance = delta.length();
                double combinedRadius = cell1.getRadiusSize() / 2 + cell2.getRadiusSize() / 2;
                if (distance < combinedRadius) {
                    double overlap = combinedRadius - distance;
                    double forceMagnitude = SimulationConfig.getInstance().getCellRepulsionStrength() * overlap;
                    Vector2D direction = delta.divide(distance);
                    Vector2D force = direction.multiply(forceMagnitude);
                    cell1.applyForce(force.multiply(-1), cell1.getPosition(), deltaTime);
                    cell2.applyForce(force, cell2.getPosition(), deltaTime);
                }
            }
        }
    }
}
