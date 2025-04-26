package de.lifecircles.service;

public class PartitioningStrategyFactory {
    final private static Strategy strategy = Strategy.SPATIAL_GRID;
    //final private static Strategy strategy = Strategy.QUADTREE;

    enum Strategy {
        SPATIAL_GRID,
        QUADTREE
    }
    public static PartitioningStrategy createStrategy(final double width, final double height, final double cellSize) {
        switch (strategy) {
            case SPATIAL_GRID:
                return new SpatialGridPartitioningStrategy(width, height, cellSize);
            case QUADTREE:
                return new QuadTreePartitioningStrategy(width, height);
            default:
                throw new IllegalArgumentException("Unknown partitioning strategy type: " + strategy);
        }
    }
}
