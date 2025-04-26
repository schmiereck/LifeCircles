package de.lifecircles.service;

import de.lifecircles.model.Cell;
import java.util.List;

/**
 * Partitioning using a spatial grid to find neighbors.
 */
public class SpatialGridPartitioningStrategy implements PartitioningStrategy {
    private final SpatialGrid grid;

    public SpatialGridPartitioningStrategy(double width, double height, double cellSize) {
        this.grid = new SpatialGrid(width, height, cellSize);
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
