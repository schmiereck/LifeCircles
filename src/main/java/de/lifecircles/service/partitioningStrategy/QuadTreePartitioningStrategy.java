package de.lifecircles.service.partitioningStrategy;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.SimulationConfig;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Partitioning using QuadTree to find neighbors.
 */
public class QuadTreePartitioningStrategy implements PartitioningStrategy {
    private final double width;
    private final double height;
    private final double interactionRadius; // Neues Feld für den Interaktionsradius
    private QuadTree quadTree;

    public QuadTreePartitioningStrategy(double width, double height) {
        this(width, height, 2 * SimulationConfig.getInstance().getCellMaxRadiusSize()
                + SimulationConfig.getInstance().getCellActorMaxFieldRadius());
    }
    
    // Neuer Konstruktor mit interactionRadius Parameter
    public QuadTreePartitioningStrategy(double width, double height, double interactionRadius) {
        if (!Double.isFinite(width) || !Double.isFinite(height) || !Double.isFinite(interactionRadius)
                || width <= 0 || height <= 0 || interactionRadius <= 0) {
            throw new IllegalArgumentException("World and interaction dimensions must be finite and positive");
        }
        this.width = width;
        this.height = height;
        this.interactionRadius = interactionRadius;
    }

    @Override
    public void build(List<Cell> cells) {
        QuadTree.Boundary boundary = new QuadTree.Boundary(width / 2, height / 2, width / 2, height / 2);
        quadTree = new QuadTree(boundary);
        for (Cell cell : cells) {
            quadTree.insert(cell, cell.getPosition().wrapped(width, height));
        }
    }

    @Override
    public List<Cell> getNeighbors(Cell cell) {
        Vector2D center = cell.getPosition().wrapped(width, height);
        LinkedHashSet<Cell> neighbors = new LinkedHashSet<>();
        // Nine images suffice even for radii larger than a world dimension:
        // every canonical point has a nearest image in this range.
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                if (x < 0 && center.getX() >= interactionRadius) continue;
                if (x > 0 && center.getX() + interactionRadius < width) continue;
                if (y < 0 && center.getY() >= interactionRadius) continue;
                if (y > 0 && center.getY() + interactionRadius < height) continue;
                // Query centers are translated opposite the desired image of the data.
                neighbors.addAll(quadTree.queryRange(center.add(new Vector2D(-x * width, -y * height)), interactionRadius));
            }
        }
        return new ArrayList<>(neighbors);
    }
}
