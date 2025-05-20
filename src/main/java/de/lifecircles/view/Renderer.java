package de.lifecircles.view;

import de.lifecircles.model.Vector2D;
import de.lifecircles.service.ActorSensorCellCalcService;
import de.lifecircles.service.dto.SimulationStateDto;
import de.lifecircles.service.SimulationConfig;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;
import javafx.geometry.VPos;

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

    public void render(SimulationStateDto state) {
        this.clear();
        if (config.isShowGrid()) {
            this.drawGrid();
        }

        // Render blockers first (background)
        for (SimulationStateDto.BlockerStateDto blocker : state.getBlockers()) {
            this.renderBlocker(blocker);
        }
        
        for (SimulationStateDto.CellStateDto cell : state.getCells()) {
            if (config.isShowForceFields()) {
                this.drawForceFields(cell);
            }
        }

        for (SimulationStateDto.CellStateDto cell : state.getCells()) {
            this.renderCell(cell);
        }

        // Render sun rays
        if (config.isShowSunRays()) {
            gc.setStroke(config.SUN_COLOR);
            gc.setLineWidth(1.5);
            for (SimulationStateDto.SunRayStateDto ray : state.getSunRays()) {
                Point2D start = camera.worldToScreen(new Vector2D(ray.getStartX(), ray.getStartY()));
                Point2D end = camera.worldToScreen(new Vector2D(ray.getEndX(), ray.getEndY()));
                gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
            }
        }

        if (config.isShowDebugInfo()) {
            this.drawDebugInfo(state);
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

    private void renderCell(SimulationStateDto.CellStateDto cell) {
        Point2D screenCellPos = this.camera.worldToScreen(cell.getPosition());
        double screenCellRadius = cell.getRadiusSize() * this.camera.getScale();

        // Draw cell body
        if (this.config.isShowCellBodies()) {
            double[] rgb = cell.getTypeRGB();
            this.gc.setFill(Color.color(rgb[0], rgb[1], rgb[2], 0.5));
            this.gc.fillOval(
                screenCellPos.getX() - screenCellRadius,
                screenCellPos.getY() - screenCellRadius,
                screenCellRadius * 2.0D,
                screenCellRadius * 2.0D
            );

            // Draw outline
            this.gc.setStroke(config.getCellOutlineColor());
            this.gc.setLineWidth(config.getCellOutlineWidth());
            this.gc.strokeOval(
                screenCellPos.getX() - screenCellRadius,
                screenCellPos.getY() - screenCellRadius,
                screenCellRadius * 2.0D,
                screenCellRadius * 2.0D
            );

            if (config.isShowActors()) {
                this.drawActors(cell);
            }

            // Draw energy bar
            if (this.config.isShowEnergy()) {
                double energyBarWidth = screenCellRadius * 0.8;
                double energyBarHeight = screenCellRadius * 0.1;
                double energyLevel = cell.getEnergy();

                this.gc.setFill(Color.RED);
                this.gc.fillRect(
                    screenCellPos.getX() - energyBarWidth / 2,
                    screenCellPos.getY() + screenCellRadius / 2 + 2,
                    energyBarWidth,
                    energyBarHeight
                );

                this.gc.setFill(this.config.getEnergyBarColor());
                this.gc.fillRect(
                    screenCellPos.getX() - energyBarWidth / 2,
                    screenCellPos.getY() + screenCellRadius / 2 + 2,
                    energyBarWidth * energyLevel,
                    energyBarHeight
                );
            }

            // Draw age indicator
            if (this.config.isShowAge()) {
                double maxAge = 60.0; // 1 minute
                double normalizedAge = Math.min(cell.getAge() / maxAge, 1.0);
                double ageBarWidth = screenCellRadius * 0.8;
                double ageBarHeight = screenCellRadius * 0.1;

                this.gc.setFill(this.config.getAgeBarColor());
                this.gc.fillRect(
                    screenCellPos.getX() - ageBarWidth / 2,
                    screenCellPos.getY() + screenCellRadius / 2 + 6,
                    ageBarWidth * normalizedAge,
                    ageBarHeight
                );
            }

            // Draw cell state as a number in the center of the cell
            if (this.config.isShowSpecialization()) {
                this.drawCellState(cell, screenCellPos, rgb);
            }
        }
    }

    private void drawCellState(SimulationStateDto.CellStateDto cell, Point2D screenCellPos, double[] rgb) {
        int cellState = cell.getCellState(); // Retrieve the cell state
        this.gc.setFill(Color.color(rgb[0], rgb[1], rgb[2]).brighter()); // Use cell type color
        this.gc.setFont(new Font("Arial", 14 * this.camera.getScale())); // Scale font size with zoom
        this.gc.setTextAlign(TextAlignment.CENTER);
        this.gc.setTextBaseline(VPos.CENTER);
        this.gc.fillText(
            String.valueOf(cellState), // Display the cell state as a number
            screenCellPos.getX(),
            screenCellPos.getY()
        );
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

    private void drawActors(SimulationStateDto.CellStateDto cell) {
        for (SimulationStateDto.ActorStateDto actor : cell.getActors()) {
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

    private void drawForceFields(SimulationStateDto.CellStateDto cell) {
        for (SimulationStateDto.ActorStateDto actor : cell.getActors()) {
            Point2D screenPos = camera.worldToScreen(actor.getPosition());

            // Dynamic force field radius based on actor spacing
            int actorCount = cell.getActors().size();
            double chord = ActorSensorCellCalcService.calcSensorRadius(cell.getRadiusSize(), actorCount);
            double radius = chord * camera.getScale();

            final Color baseColor;
            if (actor.getForceStrength() > 0.0D) {
                // Attractive force - inward gradient
                baseColor = Color.color(
                        1.0D, 0, 0,
                        config.getForceFieldOpacity() * Math.abs(actor.getForceStrength() /
                                (SimulationConfig.getInstance().getCellActorMaxAttractiveForceStrength() * 4.0D))
                );
            } else {
                // Repulsive force - outward gradient
                baseColor = Color.color(
                        0, 1.0D, 0,
                        config.getForceFieldOpacity() * Math.abs(actor.getForceStrength() /
                                (SimulationConfig.getInstance().getCellActorMaxAttractiveForceStrength() * 4.0D))
                );
            }
            gc.setFill(baseColor);
            gc.fillOval(
                    screenPos.getX() - radius,
                    screenPos.getY() - radius,
                    radius * 2,
                    radius * 2
            );
            gc.setGlobalAlpha(1.0);
        }
    }

    private void renderBlocker(SimulationStateDto.BlockerStateDto blocker) {
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

    private void drawDebugInfo(SimulationStateDto state) {
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
