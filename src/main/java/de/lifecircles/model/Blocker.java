package de.lifecircles.model;

import javafx.scene.paint.Color;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a static blocking element in the environment.
 * Blockers can be used to create terrain, walls, and other static elements
 * that cells can interact with.
 */
public class Blocker implements SensableActor, SensableCell {
    private static final AtomicLong NEXT_SENSING_ID = new AtomicLong();
    private final long sensingId = NEXT_SENSING_ID.getAndIncrement();

    /** Stable runtime identity used only to resolve overlapping blocker contacts. */
    public long getSensingId() { return sensingId; }
    private final Vector2D position;
    private final double width;
    private final double height;
    private final Color color;
    private final BlockerType type;
    private final CellType blockCellType = new CellType(0.5, 0.5, 0.5); // Grauer Zelltyp für Blocker

    public enum BlockerType {
        GROUND,
        WALL,
        PLATFORM
    }

    public Blocker(Vector2D position, double width, double height, Color color, BlockerType type) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.color = color;
        this.type = type;
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Color getColor() {
        return color;
    }

    public BlockerType getBlockerType() {
        return type;
    }

    /**
     * Implementiert die Sensable-Schnittstelle und gibt den Blocker-Typ zurück
     * @return Immer ein graues CellType-Objekt (0.5, 0.5, 0.5)
     */
    @Override
    public CellType getType() {
        return blockCellType;
    }

    @Override
    public void setEnergy(double energy) {
    }

    @Override
    public double getEnergy() {
        return 0.0D;
    }

    @Override
    public double getMaxEnergy() {
        return 0.0D;
    }

    /**
     * Checks if a point intersects with this blocker
     */
    public boolean intersects(Vector2D point) {
        return point.getX() >= position.getX() - width/2 &&
               point.getX() <= position.getX() + width/2 &&
               point.getY() >= position.getY() - height/2 &&
               point.getY() <= position.getY() + height/2;
    }

    /**
     * Returns the nearest point on the blocker's surface to the given point.
     * Ensures that the point is always pushed outside the blocker.
     */
    public Vector2D getNearestPoint(Vector2D point) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;

        // Wenn der Punkt außerhalb liegt, finde den nächsten Punkt auf der Oberfläche
        double x = Math.max(position.getX() - halfWidth, Math.min(point.getX(), position.getX() + halfWidth));
        double y = Math.max(position.getY() - halfHeight, Math.min(point.getY(), position.getY() + halfHeight));
        Vector2D nearestPoint = new Vector2D(x, y);

        // Wenn der Punkt innerhalb liegt, bewege ihn zur nächsten Kante
        if (containsPoint(point)) {
            double distToLeft = Math.abs(point.getX() - (position.getX() - halfWidth));
            double distToRight = Math.abs((position.getX() + halfWidth) - point.getX());
            double distToTop = Math.abs(point.getY() - (position.getY() - halfHeight));
            double distToBottom = Math.abs((position.getY() + halfHeight) - point.getY());

            if (distToLeft <= distToRight && distToLeft <= distToTop && distToLeft <= distToBottom) {
                nearestPoint = new Vector2D(position.getX() - halfWidth, point.getY());
            } else if (distToRight <= distToTop && distToRight <= distToBottom) {
                nearestPoint = new Vector2D(position.getX() + halfWidth, point.getY());
            } else if (distToTop <= distToBottom) {
                nearestPoint = new Vector2D(point.getX(), position.getY() - halfHeight);
            } else {
                nearestPoint = new Vector2D(point.getX(), position.getY() + halfHeight);
            }
        }

        return nearestPoint;
    }

    /**
     * Returns the new position of the cell's center when it is moved outside the blocker along the shortest path.
     * This ensures the cell is positioned just outside the blocker's surface.
     *
     * @param cellPos The current position of the cell's center.
     * @param radius The radius of the cell.
     * @return The new position of the cell's center outside the blocker.
     */
    public Vector2D getCellCenterOutside(final Vector2D cellPos, final double radius) {
        return getCellCenterOutside_x(cellPos, radius);
    }

    /**
     * Returns the new position of the cell's center when it is moved outside the blocker along the shortest path.
     * This ensures the cell is positioned just outside the blocker's surface.
     *
     * @param cellPos The current position of the cell's center.
     * @param radius The radius of the cell.
     * @return The new position of the cell's center outside the blocker.
     */
    public Vector2D getCellCenterOutside_x(Vector2D cellPos, double radius) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;

        // Wenn die Zelle innerhalb des Blockers ist, bestimme die kürzeste Richtung zum Rand
        if (containsPoint(cellPos)) {
            double distToLeft = Math.abs(cellPos.getX() - (position.getX() - halfWidth));
            double distToRight = Math.abs(cellPos.getX() - (position.getX() + halfWidth));
            double distToTop = Math.abs(cellPos.getY() - (position.getY() - halfHeight));
            double distToBottom = Math.abs(cellPos.getY() - (position.getY() + halfHeight));

            // Bestimme die kürzeste Distanz zum Rand
            if (distToLeft <= distToRight && distToLeft <= distToTop && distToLeft <= distToBottom) {
                // Links ist am nächsten
                return new Vector2D(position.getX() - halfWidth - radius, cellPos.getY());
            } else if (distToRight <= distToTop && distToRight <= distToBottom) {
                // Rechts ist am nächsten
                return new Vector2D(position.getX() + halfWidth + radius, cellPos.getY());
            } else if (distToTop <= distToBottom) {
                // Oben ist am nächsten
                return new Vector2D(cellPos.getX(), position.getY() - halfHeight - radius);
            } else {
                // Unten ist am nächsten
                return new Vector2D(cellPos.getX(), position.getY() + halfHeight + radius);
            }
        }

        // Finde den nächsten Punkt auf dem Blocker zur Zellposition
        double x = Math.max(position.getX() - halfWidth, Math.min(cellPos.getX(), position.getX() + halfWidth));
        double y = Math.max(position.getY() - halfHeight, Math.min(cellPos.getY(), position.getY() + halfHeight));
        Vector2D nearestSurfacePoint = new Vector2D(x, y);

        // Vektor vom nächsten Punkt auf der Oberfläche zum Zellmittelpunkt
        Vector2D toCellCenter = cellPos.subtract(nearestSurfacePoint);
        double dist = toCellCenter.length();
        
        // Wenn die Zelle zu nahe am Blocker ist oder sich darin befindet
        if (dist < radius) {
            // Falls genau auf dem Blocker-Rand liegt oder Nullvektor vorliegt, spezielle Behandlung
            if (dist == 0 || (toCellCenter.getX() == 0 && toCellCenter.getY() == 0)) {
                // Ermittle welcher Rand am nächsten ist
                if (Math.abs(x - (position.getX() - halfWidth)) < 0.001) {
                    // Links
                    return new Vector2D(x - radius, y);
                } else if (Math.abs(x - (position.getX() + halfWidth)) < 0.001) {
                    // Rechts
                    return new Vector2D(x + radius, y);
                } else if (Math.abs(y - (position.getY() - halfHeight)) < 0.001) {
                    // Oben
                    return new Vector2D(x, y - radius);
                } else {
                    // Unten
                    return new Vector2D(x, y + radius);
                }
            }
            
            // Normalisiere den Vektor und verschiebe die Zelle entsprechend ihres Radius
            Vector2D direction = toCellCenter.normalize();
            return nearestSurfacePoint.add(direction.multiply(radius));
        }
        
        // Wenn die Zelle bereits weit genug entfernt ist, behalte die Position bei
        return cellPos;
    }

    /**
     * Überprüft, ob ein gegebener Punkt innerhalb des Blockers liegt
     * @param point Der zu prüfende Punkt
     * @return true, wenn der Punkt im Blocker liegt, sonst false
     */
    public boolean containsPoint(Vector2D point) {
        if (point == null) return false;

        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;

        // Kleine Toleranz hinzufügen, um Randpunkte besser zu erkennen
        double tolerance = 0.0;

        // Prüfe, ob der Punkt innerhalb des Rechtecks liegt
        return point.getX() >= (position.getX() - halfWidth - tolerance) &&
                point.getX() <= (position.getX() + halfWidth + tolerance) &&
                point.getY() >= (position.getY() - halfHeight - tolerance) &&
                point.getY() <= (position.getY() + halfHeight + tolerance);
    }

    /** Tests the union of all periodic images, including blockers spanning a full axis. */
    public boolean containsPoint(Vector2D point, double worldWidth, double worldHeight) {
        if (point == null) return false;
        Vector2D local = position.getShortestDirectionWithWrapAround(point, worldWidth, worldHeight);
        return Math.abs(local.getX()) <= width / 2 && Math.abs(local.getY()) <= height / 2;
    }

    /**
     * Shortest offset from point to the periodic rectangle, zero inside or on it. A rectangle covering a full
     * axis has no edge on that axis (BODENHALT.md: surface fields and anchors).
     */
    public Vector2D offsetToSurface(Vector2D point, double worldWidth, double worldHeight) {
        Vector2D local = position.getShortestDirectionWithWrapAround(point, worldWidth, worldHeight);
        double halfX = width >= worldWidth ? Double.POSITIVE_INFINITY : width / 2;
        double halfY = height >= worldHeight ? Double.POSITIVE_INFINITY : height / 2;
        double x = local.getX(), y = local.getY();
        return new Vector2D(Math.max(-halfX, Math.min(x, halfX)) - x, Math.max(-halfY, Math.min(y, halfY)) - y);
    }

    /** Circle overlap with the periodic rectangle; exact tangency is allowed. */
    public boolean overlapsCircle(Vector2D point, double radius, double worldWidth, double worldHeight) {
        Vector2D local = position.getShortestDirectionWithWrapAround(point, worldWidth, worldHeight);
        double dx = Math.max(0, Math.abs(local.getX()) - width / 2);
        double dy = Math.max(0, Math.abs(local.getY()) - height / 2);
        return containsPoint(point, worldWidth, worldHeight) || Math.hypot(dx, dy) < radius;
    }

    /**
     * Resolves contact in a local periodic image, returning an unwrapped center.
     * A rectangle covering a full axis has no escapable edge on that axis.
     */
    public Vector2D getCellCenterOutside(Vector2D point, double radius, double worldWidth, double worldHeight) {
        Vector2D local = position.getShortestDirectionWithWrapAround(point, worldWidth, worldHeight);
        double halfX = width >= worldWidth ? Double.POSITIVE_INFINITY : width / 2;
        double halfY = height >= worldHeight ? Double.POSITIVE_INFINITY : height / 2;
        double x = local.getX(), y = local.getY();
        double nearestX = Math.max(-halfX, Math.min(x, halfX));
        double nearestY = Math.max(-halfY, Math.min(y, halfY));
        double dx = x - nearestX, dy = y - nearestY;
        double distance = Math.hypot(dx, dy);
        if (Math.abs(x) <= halfX && Math.abs(y) <= halfY) {
            // A completely blocked torus has no outside position to project to.
            if (Double.isInfinite(halfX) && Double.isInfinite(halfY)) return point;
            double left = x + halfX, right = halfX - x;
            double top = y + halfY, bottom = halfY - y;
            if (left <= right && left <= top && left <= bottom) dx = -left - radius;
            else if (right <= top && right <= bottom) dx = right + radius;
            else if (top <= bottom) dy = -top - radius;
            else dy = bottom + radius;
            return point.add(new Vector2D(dx, dy));
        }
        if (distance >= radius) return point;
        double correction = (radius - distance) / distance;
        return point.add(new Vector2D(dx * correction, dy * correction));
    }
}
