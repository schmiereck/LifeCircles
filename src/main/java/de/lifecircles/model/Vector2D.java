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

    public Vector2D normalize() {
        double len = length();
        if (len > 0) {
            return new Vector2D(x / len, y / len);
        }
        return new Vector2D(0, 0);
    }

    public double distance(Vector2D other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double distance(Vector2D position, Vector2D position1) {
        return position.distance(position1);
    }

    public Vector2D rotate(double angleRadians) {
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        return new Vector2D(
            x * cos - y * sin,
            x * sin + y * cos
        );
    }

    /**
     * Returns a vector perpendicular to this vector.
     * The result is a 90-degree counterclockwise rotation of this vector.
     */
    public Vector2D perpendicular() {
        return new Vector2D(-y, x);
    }

    /**
     * Dot product of this vector with another.
     */
    public double dot(Vector2D other) {
        return x * other.x + y * other.y;
    }

    /**
     * Squared length of the vector.
     */
    public double lengthSquared() {
        return x * x + y * y;
    }

    /**
     * Projects this vector onto another vector.
     * @param other The vector to project onto.
     * @return The projection of this vector onto the other vector.
     */
    public Vector2D projectOnto(Vector2D other) {
        double dotProduct = this.dot(other);
        double otherLengthSquared = other.lengthSquared();
        if (otherLengthSquared == 0) {
            return new Vector2D(0, 0);
        }
        double scalar = dotProduct / otherLengthSquared;
        return other.multiply(scalar);
    }

    @Override
    public String toString() {
        return "Vector2D{" + "x=" + x + ", y=" + y + '}';
    }
}
