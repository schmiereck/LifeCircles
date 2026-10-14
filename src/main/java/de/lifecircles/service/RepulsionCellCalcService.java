package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Calculates cell repulsion without writing to neighboring cells. */
public class RepulsionCellCalcService {
    private static final double MIN_DIRECTION_DISTANCE = 1e-12;

    public static void processRepulsiveForces(final List<Cell> cells, final PartitioningStrategy partitioner) {
        processRepulsiveForces(cells, partitioner, false);
    }

    /** Legacy linear entry point; time integration is performed by Cell.updateForce. */
    public static void processRepulsiveForces_x(final List<Cell> cells, final double deltaTime,
                                               final PartitioningStrategy partitioner) {
        processRepulsiveForces(cells, partitioner, false);
    }

    public static void processRepulsiveForces_nonLin(final List<Cell> cells, final double deltaTime,
                                                    final PartitioningStrategy partitioner) {
        processRepulsiveForces(cells, partitioner, true);
    }

    private static void processRepulsiveForces(final List<Cell> cells, final PartitioningStrategy partitioner,
                                               final boolean nonLinear) {
        final Environment environment = Environment.getInstance();
        final Map<Cell, Integer> order = new IdentityHashMap<>();
        for (int i = 0; i < cells.size(); i++) order.put(cells.get(i), i);
        cells.parallelStream().forEach(cell -> {
            for (Cell other : partitioner.getNeighbors(cell)) {
                if (other == cell) continue;
                final Vector2D delta = cell.getPosition().getShortestDirectionWithWrapAround(
                        other.getPosition(), environment.getWidth(), environment.getHeight());
                final double distance = Math.hypot(delta.getX(), delta.getY());
                final double combinedRadius = cell.getRadiusSize() + other.getRadiusSize();
                if (distance >= combinedRadius) continue;
                double magnitude = SimulationConfig.getInstance().getCellRepulsionStrength()
                        * (combinedRadius - distance);
                if (nonLinear) magnitude *= 0.5 + 3.5 * (1 - distance / combinedRadius);

                // A zero (or numerically negligible) delta has no usable normal.
                // The stable list order gives the reverse pair the opposite normal,
                // preserving momentum and actively separating coincident cells.
                final Vector2D force = distance > MIN_DIRECTION_DISTANCE
                        ? new Vector2D(-delta.getX() / distance * magnitude, -delta.getY() / distance * magnitude)
                        : new Vector2D(order.get(cell) < order.get(other) ? -magnitude : magnitude, 0);
                cell.applyForce(force);
            }
        });
    }
}
