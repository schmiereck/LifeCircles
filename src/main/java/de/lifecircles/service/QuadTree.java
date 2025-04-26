package de.lifecircles.service;

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
        Vector2D pos = cell.getPosition();
        if (!boundary.contains(pos)) {
            return false;
        }
        if (points.size() < capacity) {
            points.add(cell);
            return true;
        } else {
            if (!divided) {
                subdivide();
            }
            if (northeast.insert(cell)) return true;
            if (northwest.insert(cell)) return true;
            if (southeast.insert(cell)) return true;
            if (southwest.insert(cell)) return true;
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
        for (Cell p : points) {
            if (p.getPosition().distance(center) <= radius) {
                found.add(p);
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
