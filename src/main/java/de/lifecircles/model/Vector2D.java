package de.lifecircles.model;

import java.io.Serializable;

/**
 * Represents a 2D vector with x and y components.
 * Provides basic vector operations needed for the simulation.
 */
public class Vector2D implements Serializable {
    private static final long serialVersionUID = 1L;

    private double x;
    private double y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Vector2D add(Vector2D other) {
        return new Vector2D(x + other.x, y + other.y);
    }

    public Vector2D subtract(Vector2D other) {
        return new Vector2D(x - other.x, y - other.y);
    }

    public Vector2D multiply(double scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }

    public Vector2D divide(double scalar) {
        return scalar != 0 ? new Vector2D(x / scalar, y / scalar) : new Vector2D(0, 0);
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double lengthSquared() {
        return x * x + y * y;
    }

    public Vector2D normalize() {
        double length = length();
        return length > 0 ? divide(length) : new Vector2D(0, 0);
    }

    public double distance(Vector2D other) {
        return subtract(other).length();
    }

    /**
     * Berechnet den Richtungsvektor und Abstand zum nächsten Punkt unter Berücksichtigung von Wrap-Around-Grenzen.
     * @param other Der andere Punkt
     * @param width Breite der Umgebung
     * @param height Höhe der Umgebung
     * @return Ein Vektor, der die kürzeste Richtung unter Berücksichtigung von Wrap-Around angibt
     */
    public Vector2D getShortestDirectionWithWrapAround(Vector2D other, double width, double height) {
        // Direkte Differenz
        double dx = other.x - this.x;
        double dy = other.y - this.y;

        // Prüfen, ob es über Grenzen hinweg kürzer ist (horizontale Richtung)
        if (Math.abs(dx) > width / 2) {
            if (dx > 0) {
                dx = dx - width;  // andere Zelle ist links über die Grenze hinweg
            } else {
                dx = dx + width;  // andere Zelle ist rechts über die Grenze hinweg
            }
        }

        // Prüfen, ob es über Grenzen hinweg kürzer ist (vertikale Richtung)
        if (Math.abs(dy) > height / 2) {
            if (dy > 0) {
                dy = dy - height;  // andere Zelle ist oberhalb über die Grenze hinweg
            } else {
                dy = dy + height;  // andere Zelle ist unterhalb über die Grenze hinweg
            }
        }

        return new Vector2D(dx, dy);
    }

    @Override
    public String toString() {
        return "Vector2D [x=" + x + ", y=" + y + "]";
    }
}
