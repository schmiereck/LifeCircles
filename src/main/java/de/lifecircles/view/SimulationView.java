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

import java.util.Objects;

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
    
    // Für FPS-Begrenzung
    private static final long NANOS_PER_SECOND = 1_000_000_000;
    private final int TARGET_FPS = 30;
    private final long FRAME_TIME_NANOS = NANOS_PER_SECOND / TARGET_FPS; // Zeit pro Frame in Nanosekunden
    private long lastFrameTime = 0;
    
    // Für FPS-Glättung
    private final int FPS_SAMPLE_SIZE = 10;
    private final double[] fpsHistory = new double[FPS_SAMPLE_SIZE];
    private int fpsHistoryIndex = 0;
    
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

        // Drag handler for panning
        canvas.setOnMouseDragged(e -> {
            if (isDragging) {
                double deltaX = e.getX() - lastMouseX;
                double deltaY = e.getY() - lastMouseY;
                
                // Bewege die Kamera entgegengesetzt zur Mausbewegung
                camera.moveBy(-deltaX / camera.getZoom(), -deltaY / camera.getZoom());
                
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });
        
        // Release handler to end dragging
        canvas.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                isDragging = false;
            }
        });
        
        // Zoom with mouse wheel, centered on mouse position
        canvas.setOnScroll(e -> {
            // Position der Maus in Weltkoordinaten vor dem Zoom
            double mouseX = e.getX();
            double mouseY = e.getY();
            double worldX = camera.screenToWorldX(mouseX);
            double worldY = camera.screenToWorldY(mouseY);
            
            // Zoom-Faktor basierend auf der Scroll-Richtung
            double zoomFactor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            double newZoom = camera.getZoom() * zoomFactor;
            
            // Begrenze den Zoom auf sinnvolle Werte
            newZoom = Math.max(0.1, Math.min(newZoom, 10.0));
            
            // Setze neuen Zoom
            camera.setZoom(newZoom);
            
            // Berechne neue Position der Maus in Weltkoordinaten nach dem Zoom
            double newWorldX = camera.screenToWorldX(mouseX);
            double newWorldY = camera.screenToWorldY(mouseY);
            
            // Verschiebe die Kamera, damit der Punkt unter der Maus gleich bleibt
            camera.moveBy(worldX - newWorldX, worldY - newWorldY);
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
     * mit einer Begrenzung auf 30 FPS
     */
    private void startRenderLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Prüfen, ob genügend Zeit seit dem letzten Frame vergangen ist
                if (now - lastFrameTime >= FRAME_TIME_NANOS) {
                    // Aktualisiere die Ansicht mit den neuesten Daten
                    SimulationStateDto simulationStateDto = calculationService.getLatestState();
                    if (Objects.nonNull(simulationStateDto)) {
                        GraphicsContext gc = canvas.getGraphicsContext2D();
                        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                        
                        // Zeichne die Simulation mit dem Renderer
                        renderer.render(simulationStateDto);
                        
                        // Nur bei tatsächlich gerenderten Frames den Counter erhöhen
                        frameCount++;

                        // Zeit des letzten Frames nur um die exakte Frame-Zeit erhöhen
                        // Dies verhindert, dass wir mehr oder weniger als die Ziel-FPS bekommen
                        lastFrameTime += FRAME_TIME_NANOS;
                    }
                }
                
                // FPS-Zähler jede Sekunde aktualisieren (unabhängig vom Rendern)
                if (now - lastFpsTime >= NANOS_PER_SECOND) {
                    // Exakte Berechnung der FPS basierend auf der tatsächlich verstrichenen Zeit
                    double elapsedSeconds = (now - lastFpsTime) / (double)NANOS_PER_SECOND;
                    double currentFps = frameCount / elapsedSeconds;
                    
                    // In FPS-Historie speichern
                    fpsHistory[fpsHistoryIndex] = currentFps;
                    fpsHistoryIndex = (fpsHistoryIndex + 1) % FPS_SAMPLE_SIZE;
                    
                    // Durchschnitt über alle gespeicherten FPS-Werte berechnen
                    double totalFps = 0;
                    int validSamples = 0;
                    for (double fpsSample : fpsHistory) {
                        if (fpsSample > 0) {
                            totalFps += fpsSample;
                            validSamples++;
                        }
                    }
                    
                    // Durchschnittliche FPS setzen
                    if (validSamples > 0) {
                        fps = totalFps / validSamples;
                    } else {
                        fps = currentFps;
                    }
                    
                    // Zurücksetzen für die nächste Sekunde
                    frameCount = 0;
                    lastFpsTime = now;
                }
            }
        };
        
        // Initialisierung direkt vor dem Start des Timers
        lastFrameTime = System.nanoTime();
        lastFpsTime = lastFrameTime;
        
        timer.start();
    }
    
    /**
     * Gibt die aktuelle Bildrate (FPS) des Renderloops zurück
     */
    public double getFps() {
        return fps;
    }
}
