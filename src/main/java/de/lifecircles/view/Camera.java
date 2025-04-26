package de.lifecircles.view;

import de.lifecircles.model.Vector2D;
import de.lifecircles.service.SimulationConfig;
import javafx.geometry.Point2D;

/**
 * Handles view transformation (pan and zoom).
 */
public class Camera {
    private double scale;
    private Vector2D position;
    private final double minScale = 0.1;
    private final double maxScale = 10.0;

    public Camera() {
        this.scale = 0.92D / SimulationConfig.getInstance().getScaleSimulation();
        this.position = new Vector2D(0, 0);
    }

    public void pan(double dx, double dy) {
        position = position.add(new Vector2D(dx / scale, dy / scale));
    }

    public void zoom(double factor, double centerX, double centerY) {
        double newScale = scale * factor;
        if (newScale < minScale || newScale > maxScale) {
            return;
        }

        // Adjust position to keep the zoom centered on the mouse
        Vector2D beforeZoom = screenToWorld(new Point2D(centerX, centerY));
        scale = newScale;
        Vector2D afterZoom = screenToWorld(new Point2D(centerX, centerY));
        position = position.add(beforeZoom.subtract(afterZoom));
    }

    public Point2D worldToScreen(Vector2D worldPos) {
        return new Point2D(
            (worldPos.getX() - position.getX()) * scale,
            (worldPos.getY() - position.getY()) * scale
        );
    }

    public Vector2D screenToWorld(Point2D screenPos) {
        return new Vector2D(
            screenPos.getX() / scale + position.getX(),
            screenPos.getY() / scale + position.getY()
        );
    }

    /**
     * Scales a world distance to screen distance.
     * @param worldDistance The distance in world coordinates
     * @return The distance in screen coordinates
     */
    public double scaleToScreen(double worldDistance) {
        return worldDistance * scale;
    }

    public double getScale() {
        return scale;
    }

    public Vector2D getPosition() {
        return position;
    }
}
