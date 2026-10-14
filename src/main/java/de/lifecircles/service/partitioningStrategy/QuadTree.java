package de.lifecircles.service.partitioningStrategy;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;
import java.util.List;
import java.util.ArrayList;

/**
 * Adaptive quadtree spatial partitioning for cells.
 */
public class QuadTree {
    private static final int DEFAULT_CAPACITY = 4;
    private final Boundary boundary;
    private final int capacity;
    private final List<Cell> points;
    private final List<Vector2D> positions = new ArrayList<>();
    private boolean divided;
    private QuadTree northeast;
    private QuadTree northwest;
    private QuadTree southeast;
    private QuadTree southwest;

    public QuadTree(Boundary boundary) {
        this(boundary, DEFAULT_CAPACITY);
    }

    public QuadTree(Boundary boundary, int capacity) {
        this.boundary = boundary;
        this.capacity = capacity;
        this.points = new ArrayList<>();
        this.divided = false;
    }

    public boolean insert(Cell cell) {
        return insert(cell, cell.getPosition());
    }

    /** Indexed coordinates may be a periodic image; the Cell itself is never moved. */
    public boolean insert(Cell cell, Vector2D pos) {
        if (!boundary.contains(pos)) {
            return false;
        }
        if (points.size() < capacity) {
            points.add(cell);
            positions.add(pos);
            return true;
        } else {
            if (!divided) {
                subdivide();
            }
            // Stop subdividing when floating-point coordinates cannot distinguish children.
            if (boundary.halfWidth < 1e-10 || boundary.halfHeight < 1e-10) {
                points.add(cell);
                positions.add(pos);
                return true;
            }
            if (northeast.insert(cell, pos)) return true;
            if (northwest.insert(cell, pos)) return true;
            if (southeast.insert(cell, pos)) return true;
            if (southwest.insert(cell, pos)) return true;
        }
        return false;
    }

    private void subdivide() {
        double x = boundary.x;
        double y = boundary.y;
        double hw = boundary.halfWidth / 2;
        double hh = boundary.halfHeight / 2;
        northeast = new QuadTree(new Boundary(x + hw, y - hh, hw, hh), capacity);
        northwest = new QuadTree(new Boundary(x - hw, y - hh, hw, hh), capacity);
        southeast = new QuadTree(new Boundary(x + hw, y + hh, hw, hh), capacity);
        southwest = new QuadTree(new Boundary(x - hw, y + hh, hw, hh), capacity);
        divided = true;
    }

    public List<Cell> queryRange(Vector2D center, double radius) {
        List<Cell> found = new ArrayList<>();
        if (!boundary.intersectsCircle(center.getX(), center.getY(), radius)) {
            return found;
        }
        for (int i = 0; i < points.size(); i++) {
            if (positions.get(i).distance(center) <= radius) {
                found.add(points.get(i));
            }
        }
        if (divided) {
            found.addAll(northeast.queryRange(center, radius));
            found.addAll(northwest.queryRange(center, radius));
            found.addAll(southeast.queryRange(center, radius));
            found.addAll(southwest.queryRange(center, radius));
        }
        return found;
    }

    /**
     * Axis-aligned rectangular boundary centered at (x,y).
     */
    public static class Boundary {
        private final double x;
        private final double y;
        private final double halfWidth;
        private final double halfHeight;

        public Boundary(double x, double y, double halfWidth, double halfHeight) {
            this.x = x;
            this.y = y;
            this.halfWidth = halfWidth;
            this.halfHeight = halfHeight;
        }

        public boolean contains(Vector2D point) {
            double px = point.getX();
            double py = point.getY();
            return px >= x - halfWidth && px < x + halfWidth &&
                   py >= y - halfHeight && py < y + halfHeight;
        }

        public boolean intersectsCircle(double cx, double cy, double radius) {
            double xDist = Math.abs(cx - x);
            double yDist = Math.abs(cy - y);
            if (xDist > (halfWidth + radius) || yDist > (halfHeight + radius)) {
                return false;
            }
            if (xDist <= halfWidth || yDist <= halfHeight) {
                return true;
            }
            double edges = Math.pow(xDist - halfWidth, 2) + Math.pow(yDist - halfHeight, 2);
            return edges <= radius * radius;
        }
    }
}
