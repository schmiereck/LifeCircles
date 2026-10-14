package de.lifecircles.service.partitioningStrategy;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;
import java.util.*;

/**
 * Spatial grid for partitioning cells into buckets of size based on cell diameter.
 * Allows neighbor lookups in constant time per cell, reducing interaction computation.
 */
public class SpatialGrid {
    private final double width;
    private final double height;
    private final double cellWidth;
    private final double cellHeight;
    private final int cols;
    private final int rows;
    private final List<Cell>[][] grid; // 2D array for faster access
    private final List<Cell>[][] bucketNeighbors;
    private final long[][] neighborVersion;
    private long buildVersion;
    private final List<List<Cell>> usedNeighbors = new ArrayList<>();

    // Neue Map für vorberechnete Nachbarn
    private final Map<Cell, List<Cell>> cellNeighbors = new HashMap<>();

    @SuppressWarnings("unchecked")
    public SpatialGrid(double width, double height, double interactionRadius) {
        if (!Double.isFinite(width) || !Double.isFinite(height) || !Double.isFinite(interactionRadius)
                || !(width > 0) || !(height > 0) || !(interactionRadius > 0)) {
            throw new IllegalArgumentException("World and interaction dimensions must be positive");
        }
        // Equal buckets cover the whole torus; no narrow remainder bucket at the seam.
        this.width = width;
        this.height = height;
        this.cols = Math.max(1, (int) Math.floor(width / interactionRadius));
        this.rows = Math.max(1, (int) Math.floor(height / interactionRadius));
        this.cellWidth = width / cols;
        this.cellHeight = height / rows;
        this.grid = new ArrayList[rows][cols];
        this.bucketNeighbors = new ArrayList[rows][cols];
        this.neighborVersion = new long[rows][cols];
        clear();
    }

    public void clear() {
        for (List<Cell> neighbors : usedNeighbors) neighbors.clear();
        usedNeighbors.clear();
        buildVersion++;
        for (int row = 0; row < this.rows; row++) {
            for (int col = 0; col < this.cols; col++) {
                if (this.grid[row][col] == null) this.grid[row][col] = new ArrayList<>(4);
                else this.grid[row][col].clear();
            }
        }
        cellNeighbors.clear(); // Nachbarn ebenfalls leeren
    }

    public void addCells(List<Cell> cells) {
        this.clear();
        for (Cell cell : cells) {
            int col = column(cell);
            int row = row(cell);
            // wrap around boundaries
            col = (col % this.cols + this.cols) % this.cols;
            row = (row % this.rows + this.rows) % this.rows;
            this.grid[row][col].add(cell);
        }
        // Nachbarn vorberechnen
        for (Cell cell : cells) {
            cellNeighbors.put(cell, computeNeighbors(cell));
        }
    }

    // Hilfsmethode zum Berechnen der Nachbarn einer Zelle
    private List<Cell> computeNeighbors(Cell cell) {
        int col = column(cell);
        int row = row(cell);
        col = (col % this.cols + this.cols) % this.cols;
        row = (row % this.rows + this.rows) % this.rows;

        if (neighborVersion[row][col] == buildVersion) return bucketNeighbors[row][col];
        List<Cell> neighbors = bucketNeighbors[row][col];
        if (neighbors == null) bucketNeighbors[row][col] = neighbors = new ArrayList<>(16);
        neighborVersion[row][col] = buildVersion;
        usedNeighbors.add(neighbors);
        for (int dRow = -1; dRow < Math.min(3, rows) - 1; dRow++) {
            for (int dCol = -1; dCol < Math.min(3, cols) - 1; dCol++) {
                int nRow = (row + dRow + this.rows) % this.rows;
                int nCol = (col + dCol + this.cols) % this.cols;
                neighbors.addAll(this.grid[nRow][nCol]);
            }
        }
        return neighbors;
    }

    public List<Cell> getNeighbors(Cell cell) {
        // Gibt die vorberechnete Nachbarliste zurück, falls vorhanden
        return cellNeighbors.getOrDefault(cell, Collections.emptyList());
    }

    private int column(Cell cell) {
        return Math.min(cols - 1, (int) (Vector2D.wrapCoordinate(cell.getPosition().getX(), width) / cellWidth));
    }

    private int row(Cell cell) {
        return Math.min(rows - 1, (int) (Vector2D.wrapCoordinate(cell.getPosition().getY(), height) / cellHeight));
    }
}
