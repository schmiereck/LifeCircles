package de.lifecircles.view;

import de.lifecircles.model.Vector2D;
import de.lifecircles.service.dto.SimulationState;
import de.lifecircles.service.SimulationConfig;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * Handles rendering of the simulation state.
 */
public class Renderer {
    private final ViewConfig config;
    private final Camera camera;
    private final GraphicsContext gc;

    public Renderer(GraphicsContext gc, Camera camera) {
        this.gc = gc;
        this.camera = camera;
        this.config = ViewConfig.getInstance();
    }

    public void render(SimulationState state) {
        clear();
        if (config.isShowGrid()) {
            drawGrid();
        }
        // Render sun rays
        if (config.isShowSunRays()) {
            gc.setStroke(Color.YELLOW.deriveColor(0, 1, 1, 0.5));
            gc.setLineWidth(1.5);
            for (SimulationState.SunRayState ray : state.getSunRays()) {
                Point2D start = camera.worldToScreen(new Vector2D(ray.getStartX(), ray.getStartY()));
                Point2D end = camera.worldToScreen(new Vector2D(ray.getEndX(), ray.getEndY()));
                gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
            }
        }
        
        // Render blockers first (background)
        for (SimulationState.BlockerState blocker : state.getBlockers()) {
            renderBlocker(blocker);
        }
        
        for (SimulationState.CellState cell : state.getCells()) {
            if (config.isShowForceFields()) {
                drawForceFields(cell);
            }
        }

        for (SimulationState.CellState cell : state.getCells()) {
            renderCell(cell);
            if (config.isShowActors()) {
                drawActors(cell);
            }
        }

        if (config.isShowDebugInfo()) {
            drawDebugInfo(state);
        }
        // Draw light-gray rounded border for simulation bounds (follows zoom/pan)
        SimulationConfig simConfig = SimulationConfig.getInstance();
        double worldW = simConfig.getWidth();
        double worldH = simConfig.getHeight();
        Point2D topLeft = camera.worldToScreen(new Vector2D(0, 0));
        double x = topLeft.getX();
        double y = topLeft.getY();
        double w = worldW * camera.getScale();
        double h = worldH * camera.getScale();
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(2);
        double arc = 20;
        gc.strokeRoundRect(x, y, w, h, arc, arc);
    }

    private void clear() {
        gc.setFill(config.getBackgroundColor());
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
    }

    private void drawGrid() {
        gc.setStroke(config.getGridColor());
        gc.setLineWidth(1.0);

        double spacing = config.getGridSpacing() * camera.getScale();
        Vector2D camPos = camera.getPosition();
        double startX = -camPos.getX() * camera.getScale() % spacing;
        double startY = -camPos.getY() * camera.getScale() % spacing;

        for (double x = startX; x < gc.getCanvas().getWidth(); x += spacing) {
            gc.strokeLine(x, 0, x, gc.getCanvas().getHeight());
        }

        for (double y = startY; y < gc.getCanvas().getHeight(); y += spacing) {
            gc.strokeLine(0, y, gc.getCanvas().getWidth(), y);
        }
    }

    private void renderCell(SimulationState.CellState cell) {
        Point2D screenPos = camera.worldToScreen(cell.getPosition());
        double screenSize = cell.getSize() * camera.getScale();

        // Draw cell body
        if (config.isShowCellBodies()) {
            double[] rgb = cell.getTypeRGB();
            gc.setFill(Color.color(rgb[0], rgb[1], rgb[2], 0.5));
            gc.fillOval(
                screenPos.getX() - screenSize / 2,
                screenPos.getY() - screenSize / 2,
                screenSize,
                screenSize
            );

            // Draw outline
            gc.setStroke(config.getCellOutlineColor());
            gc.setLineWidth(config.getCellOutlineWidth());
            gc.strokeOval(
                screenPos.getX() - screenSize / 2,
                screenPos.getY() - screenSize / 2,
                screenSize,
                screenSize
            );

            // Draw energy bar
            if (config.isShowEnergy()) {
                double energyBarWidth = screenSize * 0.8;
                double energyBarHeight = screenSize * 0.1;
                double energyLevel = cell.getEnergy();
                
                gc.setFill(Color.RED);
                gc.fillRect(
                    screenPos.getX() - energyBarWidth / 2,
                    screenPos.getY() + screenSize / 2 + 2,
                    energyBarWidth,
                    energyBarHeight
                );
                
                gc.setFill(config.getEnergyBarColor());
                gc.fillRect(
                    screenPos.getX() - energyBarWidth / 2,
                    screenPos.getY() + screenSize / 2 + 2,
                    energyBarWidth * energyLevel,
                    energyBarHeight
                );
            }

            // Draw age indicator
            if (config.isShowAge()) {
                double maxAge = 60.0; // 1 minute
                double normalizedAge = Math.min(cell.getAge() / maxAge, 1.0);
                double ageBarWidth = screenSize * 0.8;
                double ageBarHeight = screenSize * 0.1;
                
                gc.setFill(config.getAgeBarColor());
                gc.fillRect(
                    screenPos.getX() - ageBarWidth / 2,
                    screenPos.getY() + screenSize / 2 + 6,
                    ageBarWidth * normalizedAge,
                    ageBarHeight
                );
            }

            // Draw specialization indicator
            if (config.isShowSpecialization()) {
                double indicatorSize = screenSize * 0.2;
                
                // Determine specialization based on RGB values
                if (rgb[0] > rgb[1] && rgb[0] > rgb[2]) {
                    // Predator - Red triangle
                    drawTriangle(gc, screenPos, indicatorSize, Color.RED);
                } else if (rgb[1] > rgb[0] && rgb[1] > rgb[2]) {
                    // Producer - Green circle
                    gc.setFill(Color.GREEN);
                    gc.fillOval(
                        screenPos.getX() - indicatorSize / 2,
                        screenPos.getY() - screenSize / 2 - indicatorSize - 2,
                        indicatorSize,
                        indicatorSize
                    );
                } else if (rgb[2] > rgb[0] && rgb[2] > rgb[1]) {
                    // Consumer - Blue square
                    gc.setFill(Color.BLUE);
                    gc.fillRect(
                        screenPos.getX() - indicatorSize / 2,
                        screenPos.getY() - screenSize / 2 - indicatorSize - 2,
                        indicatorSize,
                        indicatorSize
                    );
                }
            }
        }
    }

    private void drawTriangle(GraphicsContext gc, Point2D center, double size, Color color) {
        double[] xPoints = {
            center.getX(),
            center.getX() - size / 2,
            center.getX() + size / 2
        };
        double[] yPoints = {
            center.getY() - size,
            center.getY() - size / 2,
            center.getY() - size / 2
        };
        
        gc.setFill(color);
        gc.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawActors(SimulationState.CellState cell) {
        for (SimulationState.ActorState actor : cell.getActors()) {
            Point2D screenPos = camera.worldToScreen(actor.getPosition());
            double actorSize = config.getActorSize() * camera.getScale();

            // Draw actor
            double[] rgb = actor.getTypeRGB();
            gc.setFill(Color.color(rgb[0], rgb[1], rgb[2]));
            gc.fillOval(
                screenPos.getX() - actorSize / 2,
                screenPos.getY() - actorSize / 2,
                actorSize,
                actorSize
            );
        }
    }

    private void drawForceFields(SimulationState.CellState cell) {
        for (SimulationState.ActorState actor : cell.getActors()) {
            if (Math.abs(actor.getForceStrength()) > 0.1) {
                Point2D screenPos = camera.worldToScreen(actor.getPosition());
                double[] rgb = actor.getTypeRGB();
                
                // Dynamic force field radius based on actor spacing
                int actorCount = cell.getActors().size();
                double chord = cell.getSize() * Math.sin(Math.PI / actorCount);
                double radius = chord * camera.getScale();
                
                // Create radial gradient for force field
                Color baseColor = Color.color(
                    rgb[0], rgb[1], rgb[2],
                    config.getForceFieldOpacity() * Math.abs(actor.getForceStrength())
                );
                
                gc.setFill(baseColor);
                if (actor.getForceStrength() > 0) {
                    // Attractive force - inward gradient
                    gc.setGlobalAlpha(config.getForceFieldOpacity());
                    gc.fillOval(
                        screenPos.getX() - radius,
                        screenPos.getY() - radius,
                        radius * 2,
                        radius * 2
                    );
                } else {
                    // Repulsive force - outward gradient
                    gc.setGlobalAlpha(config.getForceFieldOpacity() * 0.5);
                    gc.fillOval(
                        screenPos.getX() - radius,
                        screenPos.getY() - radius,
                        radius * 2,
                        radius * 2
                    );
                }
                gc.setGlobalAlpha(1.0);
            }
        }
    }

    private void renderBlocker(SimulationState.BlockerState blocker) {
        Point2D screenPos = camera.worldToScreen(new Vector2D(blocker.getX(), blocker.getY()));
        double screenWidth = camera.scaleToScreen(blocker.getWidth());
        double screenHeight = camera.scaleToScreen(blocker.getHeight());
        
        gc.setFill(blocker.getColor());
        gc.fillRect(
            screenPos.getX() - screenWidth/2,
            screenPos.getY() - screenHeight/2,
            screenWidth,
            screenHeight
        );
    }

    private void drawDebugInfo(SimulationState state) {
        gc.setFill(config.getTextColor());
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format("Cells: %d", state.getCells().size()), 10, 20);
        gc.fillText(String.format("Scale: %.2f", camera.getScale()), 10, 40);
        gc.fillText(String.format("Camera: (%.1f, %.1f)",
            camera.getPosition().getX(),
            camera.getPosition().getY()
        ), 10, 60);
    }
}
