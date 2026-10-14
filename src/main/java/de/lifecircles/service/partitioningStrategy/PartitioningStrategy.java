package de.lifecircles.service.partitioningStrategy;

import de.lifecircles.model.Cell;
import java.util.List;

/**
 * Strategy interface for spatial partitioning to find cell neighbors.
 */
public interface PartitioningStrategy {
    /**
     * Builds internal data structures for the given cells.
     */
    void build(List<Cell> cells);

    /**
     * Returns neighbors of the given cell based on the strategy.
     * The returned list is borrowed and read-only to callers, valid until the next build.
     */
    List<Cell> getNeighbors(Cell cell);
}
