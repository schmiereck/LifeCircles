package de.lifecircles.service.partitioningStrategy;

import de.lifecircles.model.Cell;
import java.util.List;

/**
 * Partitioning using a spatial grid to find neighbors.
 */
public class SpatialGridPartitioningStrategy implements PartitioningStrategy {
    private final SpatialGrid grid;

    public SpatialGridPartitioningStrategy(final double width, final double height, final double interactionRadius) {
        // Parameter wird jetzt als interactionRadius interpretiert
        this.grid = new SpatialGrid(width, height, interactionRadius);
    }

    @Override
    public void build(List<Cell> cells) {
        grid.clear();
        grid.addCells(cells);
    }

    @Override
    public List<Cell> getNeighbors(Cell cell) {
        return grid.getNeighbors(cell);
    }
}

