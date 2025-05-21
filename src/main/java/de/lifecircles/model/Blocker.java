package de.lifecircles.model;

import javafx.scene.paint.Color;

/**
 * Represents a static blocking element in the environment.
 * Blockers can be used to create terrain, walls, and other static elements
 * that cells can interact with.
 */
public class Blocker implements SensableActor, SensableCell {
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
}
