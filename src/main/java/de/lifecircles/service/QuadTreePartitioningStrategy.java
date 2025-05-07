package de.lifecircles.service;

import de.lifecircles.model.Cell;
import java.util.List;

/**
 * Partitioning using QuadTree to find neighbors.
 */
public class QuadTreePartitioningStrategy implements PartitioningStrategy {
    private final double width;
    private final double height;
    private QuadTree quadTree;

    public QuadTreePartitioningStrategy(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void build(List<Cell> cells) {
        QuadTree.Boundary boundary = new QuadTree.Boundary(width / 2, height / 2, width / 2, height / 2);
        quadTree = new QuadTree(boundary);
        for (Cell cell : cells) {
            quadTree.insert(cell);
        }
    }

    @Override
    public List<Cell> getNeighbors(Cell cell) {
        // Query within a radius of max diameter to retrieve nearby cells
        return quadTree.queryRange(cell.getPosition(), SimulationConfig.getInstance().getCellMaxRadiusSize());
    }
}
