package de.lifecircles.view;

import de.lifecircles.service.CalculationService;
import de.lifecircles.service.dto.SimulationState;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.service.TrainMode;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * JavaFX component for visualizing the simulation.
 * Supports zoom and pan interactions.
 */
public class SimulationView extends Pane {
    private final Canvas canvas;
    private final Camera camera;
    private final Renderer renderer;
    private final CalculationService calculationService;
    private final ViewConfig config;

    private double lastMouseX;
    private double lastMouseY;
    private boolean isDragging;

    private int frameCount = 0;
    private long lastFpsTime = System.nanoTime();
    private volatile double fps = 0.0;

    public SimulationView(CalculationService calculationService) {
        this.calculationService = calculationService;
        this.config = ViewConfig.getInstance();
        this.camera = new Camera();
        
        // Create canvas that fills the pane
        this.canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        
        // Initialize renderer
        this.renderer = new Renderer(canvas.getGraphicsContext2D(), camera);
        
        getChildren().add(canvas);
        setupInputHandlers();
        startRenderLoop();
    }

    private void setupInputHandlers() {
        // Pan with right mouse button
        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                isDragging = true;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (isDragging) {
                double dx = e.getX() - lastMouseX;
                double dy = e.getY() - lastMouseY;
                camera.pan(-dx, -dy);
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                isDragging = false;
            }
        });

        // Zoom with mouse wheel
        canvas.setOnScroll(e -> {
            double zoomFactor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            camera.zoom(zoomFactor, e.getX(), e.getY());
        });

        // Toggle debug info with D key
        canvas.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case D -> config.setShowDebugInfo(!config.isShowDebugInfo());
                case G -> config.setShowGrid(!config.isShowGrid());
                case F -> config.setShowForceFields(!config.isShowForceFields());
                case A -> config.setShowActors(!config.isShowActors());
            }
        });

        // Make canvas focusable
        canvas.setFocusTraversable(true);
    }

    private void startRenderLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
                // FPS tracking
                frameCount++;
                long nowFps = System.nanoTime();
                if (nowFps - lastFpsTime >= 1_000_000_000L) {
                    fps = frameCount / ((nowFps - lastFpsTime) / 1_000_000_000.0);
                    frameCount = 0;
                    lastFpsTime = nowFps;
                }
            }
        }.start();
    }

    private void render() {
        SimulationState state = calculationService.getLatestState();
        if (state != null) {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            renderer.render(state);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRoundRect(1, 1, canvas.getWidth() - 2, canvas.getHeight() - 2, 20, 20);
            // Anzeigen von FPS und Trainingsmodus
            gc.setFill(Color.WHITE);
            gc.setFont(new Font(14));
            gc.fillText("FPS: " + String.format("%.1f", getFps()) + "  Mode: " + SimulationConfig.getInstance().getTrainMode(), 10, canvas.getHeight() - 10);
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public ViewConfig getConfig() {
        return config;
    }

    /**
     * Returns the current rendering frames per second.
     */
    public double getFps() {
        return fps;
    }
}
