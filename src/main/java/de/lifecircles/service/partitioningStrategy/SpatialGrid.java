package de.lifecircles.service.partitioningStrategy;

import de.lifecircles.model.Cell;
import java.util.*;

/**
 * Spatial grid for partitioning cells into buckets of size based on cell diameter.
 * Allows neighbor lookups in constant time per cell, reducing interaction computation.
 */
public class SpatialGrid {
    private final double cellSize;
    private final int cols;
    private final int rows;
    private final List<Cell>[][] grid; // 2D array for faster access

    @SuppressWarnings("unchecked")
    public SpatialGrid(double width, double height, double cellSize) {
        this.cellSize = cellSize;
        this.cols = (int) Math.ceil(width / cellSize);
        this.rows = (int) Math.ceil(height / cellSize);
        this.grid = new ArrayList[rows][cols];
        clear();
    }

    public void clear() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                grid[row][col] = new ArrayList<>();
            }
        }
    }

    public void addCells(List<Cell> cells) {
        clear();
        for (Cell cell : cells) {
            int col = (int) (cell.getPosition().getX() / cellSize);
            int row = (int) (cell.getPosition().getY() / cellSize);
            // wrap around boundaries
            col = (col % cols + cols) % cols;
            row = (row % rows + rows) % rows;
            grid[row][col].add(cell);
        }
    }

    public List<Cell> getNeighbors(Cell cell) {
        List<Cell> neighbors = new ArrayList<>();
        int col = (int) (cell.getPosition().getX() / cellSize);
        int row = (int) (cell.getPosition().getY() / cellSize);
        // check own and eight surrounding cells
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int ncol = (col + dx + cols) % cols;
                int nrow = (row + dy + rows) % rows;
                neighbors.addAll(grid[nrow][ncol]);
            }
        }
        return neighbors;
    }
}
