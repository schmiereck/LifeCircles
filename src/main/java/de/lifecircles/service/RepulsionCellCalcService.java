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
    public static void processRepulsiveForces(final List<Cell> cells, final PartitioningStrategy partitioner) {
        for (final Cell cell1 : cells) {
            final List<Cell> neighbors = partitioner.getNeighbors(cell1);
            for (final Cell cell2 : neighbors) {
                if (cell2 == cell1) continue;
                final Vector2D delta = cell2.getPosition().subtract(cell1.getPosition());
                final double distance = delta.length();
                final double combinedRadius = cell1.getRadiusSize() + cell2.getRadiusSize();
                if (distance < combinedRadius) {
                    final double overlap = combinedRadius - distance;
                    final double forceMagnitude = SimulationConfig.getInstance().getCellRepulsionStrength() * overlap;
                    final Vector2D direction = delta.divide(distance);
                    final Vector2D force = direction.multiply(forceMagnitude);
                    cell1.applyForce(force.multiply(-2.0D), cell1.getPosition());
                    cell2.applyForce(force.multiply(2.0D), cell2.getPosition());
                }
            }
        }
    }

    public static void processRepulsiveForces_x(final List<Cell> cells, final double deltaTime, final PartitioningStrategy partitioner) {
        final double epsilon = 1e-6 * 10.0D; // Minimaler Abstand, um Division durch null zu vermeiden

        for (final Cell cell1 : cells) {
            final List<Cell> neighbors = partitioner.getNeighbors(cell1);
            for (final Cell cell2 : neighbors) {
                if (cell2 == cell1) continue;
                final Vector2D delta = cell2.getPosition().subtract(cell1.getPosition());
                double distance = delta.length();
                distance = Math.max(distance, epsilon); // Sicherstellen, dass distance >= epsilon
                final double combinedRadius = cell1.getRadiusSize() + cell2.getRadiusSize();
                if (distance < combinedRadius) {
                    final double overlap = combinedRadius - distance;
                    final double forceMagnitude = SimulationConfig.getInstance().getCellRepulsionStrength() * overlap;
                    final Vector2D direction = delta.divide(distance);
                    final Vector2D force = direction.multiply(forceMagnitude);
                    cell1.applyForce(force.multiply(-1), cell1.getPosition());
                    cell2.applyForce(force, cell2.getPosition());
                }
            }
        }
    }
    public static void processRepulsiveForces_nonLin(final List<Cell> cells, final double deltaTime, final PartitioningStrategy partitioner) {
        for (final Cell cell1 : cells) {
            final List<Cell> neighbors = partitioner.getNeighbors(cell1);
            for (final Cell cell2 : neighbors) {
                if (cell2 == cell1) continue;
                final Vector2D delta = cell2.getPosition().subtract(cell1.getPosition());
                final double distance = delta.length();
                final double combinedRadius = cell1.getRadiusSize() + cell2.getRadiusSize();
                if (distance < combinedRadius) {
                    final double overlap = combinedRadius - distance;
                    final double baseForceMagnitude = SimulationConfig.getInstance().getCellRepulsionStrength() * overlap;

                    // Nicht-lineare Skalierung der Kraft
                    final double scalingFactor = 0.5 + 3.5 * (1 - (distance / combinedRadius));
                    final double forceMagnitude = baseForceMagnitude * scalingFactor;

                    final Vector2D direction = delta.divide(distance);
                    final Vector2D force = direction.multiply(forceMagnitude);
                    cell1.applyForce(force.multiply(-1), cell1.getPosition());
                    cell2.applyForce(force, cell2.getPosition());
                }
            }
        }
    }
}
