package de.lifecircles.view;

import de.lifecircles.model.Cell;
import de.lifecircles.service.CalculationService;
import de.lifecircles.service.dto.SimulationStateDto;
import de.lifecircles.service.SimulationConfig;
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
    private CellDetailView cellDetailView;

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
            } else if (e.getButton() == MouseButton.PRIMARY) {
                // Linksklick - versuche, eine Zelle zu selektieren
                checkForCellClick(e.getX(), e.getY());
            }
        });

        // ... existing code ...

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

    private void checkForCellClick(double mouseX, double mouseY) {
        // Umrechnung von Bildschirmkoordinaten in Weltkoordinaten
        double worldX = camera.screenToWorldX(mouseX);
        double worldY = camera.screenToWorldY(mouseY);
        
        // Holen des aktuellen Simulationszustands
        SimulationStateDto state = calculationService.getLatestState();
        if (state == null) return;
        
        // Zugriff auf die tatsächlichen Zellen aus dem CalculationService
        Cell selectedCell = calculationService.findCellAt(worldX, worldY);
        
        if (selectedCell != null) {
            // Zelle gefunden, öffne Detailansicht
            if (cellDetailView == null || !cellDetailView.isShowing()) {
                cellDetailView = new CellDetailView();
            }
            cellDetailView.showCell(selectedCell);
        }
    }

    /**
     * Startet den Rendering-Loop für die kontinuierliche Darstellung der Simulation
     */
    private void startRenderLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Aktualisiere die Ansicht mit den neuesten Daten
                SimulationStateDto state = calculationService.getLatestState();
                if (state != null) {
                    GraphicsContext gc = canvas.getGraphicsContext2D();
                    gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    
                    // Zeichne die Simulation mit dem Renderer
                    renderer.render(state);
                    
                    // FPS-Zähler aktualisieren
                    frameCount++;
                    long currentTime = System.nanoTime();
                    if (currentTime - lastFpsTime >= 1_000_000_000) {
                        fps = frameCount / ((currentTime - lastFpsTime) / 1_000_000_000.0);
                        frameCount = 0;
                        lastFpsTime = currentTime;
                    }
                }
            }
        };
        timer.start();
    }
    
    /**
     * Gibt die aktuelle Bildrate (FPS) des Renderloops zurück
     */
    public double getFps() {
        return fps;
    }
}
