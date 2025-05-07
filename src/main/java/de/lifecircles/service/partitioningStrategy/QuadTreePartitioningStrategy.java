package de.lifecircles.service.partitioningStrategy;

import de.lifecircles.model.Cell;
import de.lifecircles.service.SimulationConfig;

import java.util.List;

/**
 * Partitioning using QuadTree to find neighbors.
 */
public class QuadTreePartitioningStrategy implements PartitioningStrategy {
    private final double width;
    private final double height;
    private final double interactionRadius; // Neues Feld für den Interaktionsradius
    private QuadTree quadTree;

    public QuadTreePartitioningStrategy(double width, double height) {
        this.width = width;
        this.height = height;
        this.interactionRadius = SimulationConfig.getInstance().getCellMaxRadiusSize();
    }
    
    // Neuer Konstruktor mit interactionRadius Parameter
    public QuadTreePartitioningStrategy(double width, double height, double interactionRadius) {
        this.width = width;
        this.height = height;
        this.interactionRadius = interactionRadius;
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
        // Verwende den kombinierten Interaktionsradius statt nur des maximalen Zellradius
        return quadTree.queryRange(cell.getPosition(), interactionRadius);
    }
}
