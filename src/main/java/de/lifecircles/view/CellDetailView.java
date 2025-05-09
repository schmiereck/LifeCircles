package de.lifecircles.view;

import de.lifecircles.model.Cell;
import de.lifecircles.model.neural.CellBrain;
import de.lifecircles.model.neural.NeuralNetwork;
import de.lifecircles.model.neural.Neuron;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

/**
 * Ein Fenster, das detaillierte Informationen zu einer ausgewählten Zelle anzeigt.
 */
public class CellDetailView extends Stage {
    
    private Cell currentCell;
    private final Canvas brainCanvas;
    private final Label typeLabel;
    private final Label stateLabel;
    private final Label ageLabel;
    private final Label energyLabel;
    private final Label generationLabel;
    private AnimationTimer updateTimer;
    private boolean needsRedraw = true;
    private double zoomFactor = 1.0;
    private double panOffsetX = 0.0;
    private double panOffsetY = 0.0;
    private double lastMouseX;
    private double lastMouseY;

    public CellDetailView() {
        setTitle("Zelldetails");
        
        // Haupt-Layout erstellen
        BorderPane mainLayout = new BorderPane();
        
        // Layout für die Mitte vorbereiten
        VBox centerBox = new VBox(10);
        centerBox.setPadding(new javafx.geometry.Insets(10));
        
        // Canvas für die Visualisierung des neuronalen Netzes
        brainCanvas = new Canvas(400, 300);
        
        // Info-Panel mit Zelleigenschaften
        GridPane infoPanel = new GridPane();
        infoPanel.setHgap(10);
        infoPanel.setVgap(5);
        infoPanel.setPadding(new javafx.geometry.Insets(10));
        
        // Labels für Zelleigenschaften
        typeLabel = new Label();
        stateLabel = new Label();
        ageLabel = new Label();
        energyLabel = new Label();
        generationLabel = new Label();
        
        // Labels zum Panel hinzufügen
        infoPanel.add(new Label("Typ:"), 0, 0);
        infoPanel.add(typeLabel, 1, 0);
        infoPanel.add(new Label("Status:"), 0, 1);
        infoPanel.add(stateLabel, 1, 1);
        infoPanel.add(new Label("Alter:"), 0, 2);
        infoPanel.add(ageLabel, 1, 2);
        infoPanel.add(new Label("Energie:"), 0, 3);
        infoPanel.add(energyLabel, 1, 3);
        infoPanel.add(new Label("Generation:"), 0, 4);
        infoPanel.add(generationLabel, 1, 4);
        
        // Label für die Netzwerk-Visualisierung
        Label networkLabel = new Label("Neuronales Netzwerk (ZellBrain):");
        
        // Layout zusammenbauen
        centerBox.getChildren().addAll(networkLabel, brainCanvas);
        VBox.setVgrow(brainCanvas, Priority.ALWAYS); // Canvas soll vertikal wachsen
        
        mainLayout.setCenter(centerBox);
        mainLayout.setBottom(infoPanel);
        
        // Szene erstellen und Stage konfigurieren
        Scene scene = new Scene(mainLayout, 600, 500);
        setScene(scene);
        setResizable(true);
        
        // Animation-Timer für Updates der Zelleigenschaften, nicht der Netzwerkvisualisierung
        updateTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            
            @Override
            public void handle(long now) {
                if (currentCell != null && now - lastUpdate > 100_000_000) { // ca. 10 mal pro Sekunde aktualisieren
                    updateCellInfo(); // Nur die Zellinformationen aktualisieren
                    
                    // Neuronennetz nur neu zeichnen, wenn es notwendig ist
                    if (needsRedraw) {
                        renderBrain();
                        needsRedraw = false;
                    }
                    
                    lastUpdate = now;
                }
            }
        };
        
        // Direkt an Fensteränderungen binden statt an Container-Änderungen
        // Dies funktioniert zuverlässiger, besonders beim Verkleinern
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (brainCanvas != null) {
                // Padding berücksichtigen und Canvas-Breite anpassen
                double width = newVal.doubleValue() - 40; // Mehr Platz für Ränder lassen
                if (width > 0) {
                    brainCanvas.setWidth(width);
                    needsRedraw = true;
                    // Sofortige Neuzeichnung erzwingen, wenn eine Zelle aktiv ist
                    if (currentCell != null) {
                        renderBrain();
                    }
                }
            }
        });
        
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (brainCanvas != null) {
                // Feste Höhe für Labels und Padding verwenden (statt Label-Höhe abzufragen)
                double reservedSpace = 150; // Platz für Labels, Infobereich und Padding
                double height = newVal.doubleValue() - reservedSpace;
                if (height > 0) {
                    brainCanvas.setHeight(height);
                    needsRedraw = true;
                    // Sofortige Neuzeichnung erzwingen, wenn eine Zelle aktiv ist
                    if (currentCell != null) {
                        renderBrain();
                    }
                }
            }
        });
        
        // Initialisiere die Canvas-Größe mit aktuellen Fenstermaßen
        brainCanvas.setWidth(scene.getWidth() - 40);
        brainCanvas.setHeight(scene.getHeight() - 150);
        
        // Wenn das Fenster geschlossen wird, Timer anhalten
        setOnCloseRequest(e -> {
            if (updateTimer != null) {
                updateTimer.stop();
            }
        });

        // Maus-Scroll-Event für Zoom
        brainCanvas.setOnScroll((ScrollEvent event) -> {
            double mouseX = event.getX();
            double mouseY = event.getY();
            double zoomDelta = event.getDeltaY() > 0 ? 1.1 : 0.9;

            // Berechne den neuen Zoomfaktor
            double newZoomFactor = zoomFactor * zoomDelta;
            newZoomFactor = Math.max(0.1, Math.min(newZoomFactor, 10.0)); // Begrenze Zoomfaktor

            // Anpassung der Pan-Werte, um die Mausposition als Mittelpunkt zu verwenden
            panOffsetX = (panOffsetX - mouseX) * (newZoomFactor / zoomFactor) + mouseX;
            panOffsetY = (panOffsetY - mouseY) * (newZoomFactor / zoomFactor) + mouseY;

            zoomFactor = newZoomFactor;
            needsRedraw = true;
            renderBrain();
        });

        // Maus-Drag-Event für Pan
        brainCanvas.setOnMousePressed((MouseEvent event) -> {
            lastMouseX = event.getX();
            lastMouseY = event.getY();
        });

        brainCanvas.setOnMouseDragged((MouseEvent event) -> {
            double deltaX = event.getX() - lastMouseX;
            double deltaY = event.getY() - lastMouseY;
            panOffsetX += deltaX;
            panOffsetY += deltaY;
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            needsRedraw = true;
            renderBrain();
        });
    }
    
    /**
     * Zeigt die Details einer ausgewählten Zelle an und öffnet das Fenster
     */
    public void showCell(Cell cell) {
        this.currentCell = cell;
        needsRedraw = true;  // Erzwinge Neuzeichnung bei neuer Zelle
        updateCellInfo();    // Informationen aktualisieren
        renderBrain();       // Initialer Render des Gehirns
        
        show();
        toFront();
        
        // Timer starten
        updateTimer.start();
    }
    
    /**
     * Aktualisiert die Zellinformationen ohne das Netzwerk neu zu zeichnen
     */
    private void updateCellInfo() {
        if (currentCell == null) return;
        
        // Zelleigenschaften aktualisieren
        typeLabel.setText(String.format("R:%.2f G:%.2f B:%.2f", 
                currentCell.getType().getRed(), 
                currentCell.getType().getGreen(), 
                currentCell.getType().getBlue()));
        stateLabel.setText(String.valueOf(currentCell.getCellState()));
        ageLabel.setText(String.format("%.2f", currentCell.getAge()));
        energyLabel.setText(String.format("%.2f", currentCell.getEnergy()));
        generationLabel.setText(String.valueOf(currentCell.getGeneration()));
    }
    
    /**
     * Erzwingt eine Neuzeichnung des neuronalen Netzwerks
     */
    public void forceRedraw() {
        needsRedraw = true;
    }
    
    /**
     * Visualisiert das neuronale Netzwerk der Zelle
     */
    private void renderBrain() {
        if (currentCell == null) return;
        
        GraphicsContext gc = brainCanvas.getGraphicsContext2D();
        // Aktuelle Canvas-Größe verwenden
        double width = brainCanvas.getWidth();
        double height = brainCanvas.getHeight();
        
        if (width <= 0 || height <= 0) return; // Verhindere Zeichnen bei ungültiger Größe
        
        gc.clearRect(0, 0, width, height);
        
        gc.save(); // Transformation speichern
        gc.translate(panOffsetX, panOffsetY); // Pan anwenden
        gc.scale(zoomFactor, zoomFactor); // Zoom anwenden

        CellBrain brain = currentCell.getBrain();
        if (brain == null) return;
        
        NeuralNetwork network = brain.getNeuralNetwork();
        if (network == null) return;
        
        // Netzwerkarchitektur holen
        int[] layers = network.getLayerSizes();
        
        if (layers == null) return;
        
        // Zeichenparameter berechnen
        // Knotengröße dynamisch berechnen
        int maxLayerSize = 0;
        for (int size : layers) {
            maxLayerSize = Math.max(maxLayerSize, size);
        }
        
        // Verbesserte Neuronengröße - fester Mindestradius
        double screenSizeFactor = Math.min(width, height) / 400.0;

        // Konstanter Mindestradius, weniger Abhängigkeit von der Netzwerkgröße
        double nodeRadius = 1.0 * screenSizeFactor;
        
        // Horizontalen Abstand anpassen
        double horizontalSpacing = width / (layers.length + 1);
        
        // Vertikale Abstände dynamisch anpassen
        double minVerticalSpacing = nodeRadius * 3; // Mindestabstand zwischen Knoten
        double maxVerticalSpacing = height / (maxLayerSize + 1);
        
        // Hintergrund
        gc.setFill(Color.rgb(30, 30, 30));
        gc.fillRect(0, 0, width, height);
        
        // Verbindungen zeichnen
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(0.5);
        
        for (int layerIdx = 0; layerIdx < layers.length - 1; layerIdx++) {
            int currentLayerSize = layers[layerIdx];
            int nextLayerSize = layers[layerIdx + 1];
            
            double currentLayerX = horizontalSpacing * (layerIdx + 1);
            double nextLayerX = horizontalSpacing * (layerIdx + 2);
            
            // Vertikale Abstände für aktuelle und nächste Schicht
            double currentLayerVSpacing = Math.max(minVerticalSpacing, 
                    Math.min(maxVerticalSpacing, height / (currentLayerSize + 1)));
            double nextLayerVSpacing = Math.max(minVerticalSpacing, 
                    Math.min(maxVerticalSpacing, height / (nextLayerSize + 1)));
            
            // Offset berechnen, um die Neuronen zu zentrieren
            double currentLayerYOffset = (height - currentLayerSize * currentLayerVSpacing) / 2;
            double nextLayerYOffset = (height - nextLayerSize * nextLayerVSpacing) / 2;
            
            // Draw connections between layers
            for (int i = 0; i < currentLayerSize; i++) {
                double y1 = currentLayerYOffset + currentLayerVSpacing * (i + 0.5);
                
                for (int j = 0; j < nextLayerSize; j++) {
                    double y2 = nextLayerYOffset + nextLayerVSpacing * (j + 0.5);
                    gc.strokeLine(currentLayerX, y1, nextLayerX, y2);
                }
            }
        }
        
        // Neuronen mit Aktivierungen zeichnen
        // Direkt auf Neuronen-Struktur zugreifen (Input, Hidden, Output)
        
        // Input-Neuronen zeichnen
        int layerIdx = 0;
        int layerSize = layers[layerIdx];
        double layerX = horizontalSpacing * (layerIdx + 1);
        double vSpacing = Math.max(minVerticalSpacing, 
                Math.min(maxVerticalSpacing, height / (layerSize + 1)));
        double layerYOffset = (height - layerSize * vSpacing) / 2;
        
        for (int i = 0; i < layerSize; i++) {
            double nodeY = layerYOffset + vSpacing * (i + 0.5);
            
            // Aktivierungswert direkt vom Input-Neuron holen
            double activation = 0;
            if (i < network.getInputNeuronList().size()) {
                activation = network.getInputNeuronList().get(i).getValue();
                // Aktivierungswert auf den Bereich [0,1] begrenzen
                activation = Math.max(0, Math.min(1, activation));
            }
            
            // Farbe basierend auf Aktivierung (blau bis rot)
            Color nodeColor = Color.rgb(
                (int) (255 * activation),
                0,
                (int) (255 * (1 - activation))
            );
            
            // Neuron zeichnen
            gc.setFill(nodeColor);
            gc.fillOval(layerX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);
            gc.setStroke(Color.BLACK);
            gc.strokeOval(layerX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);
        }
        
        // Hidden-Layer-Neuronen zeichnen
        for (int li = 0; li < network.getHiddenLayerList().size(); li++) {
            layerIdx = li + 1; // +1 weil Input-Layer bereits gezeichnet wurde
            layerSize = layers[layerIdx];
            layerX = horizontalSpacing * (layerIdx + 1);
            vSpacing = Math.max(minVerticalSpacing, 
                    Math.min(maxVerticalSpacing, height / (layerSize + 1)));
            layerYOffset = (height - layerSize * vSpacing) / 2;
            
            List<Neuron> neurons = network.getHiddenLayerList().get(li).getNeurons();
            
            for (int i = 0; i < layerSize; i++) {
                double nodeY = layerYOffset + vSpacing * (i + 0.5);
                
                // Aktivierungswert direkt vom Hidden-Neuron holen
                double activation = 0;
                if (i < neurons.size()) {
                    activation = neurons.get(i).getValue();
                    // Aktivierungswert auf den Bereich [0,1] begrenzen
                    activation = Math.max(0, Math.min(1, activation));
                }
                
                // Farbe basierend auf Aktivierung (blau bis rot)
                Color nodeColor = Color.rgb(
                    (int) (255 * activation),
                    0,
                    (int) (255 * (1 - activation))
                );
                
                // Neuron zeichnen
                gc.setFill(nodeColor);
                gc.fillOval(layerX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);
                gc.setStroke(Color.BLACK);
                gc.strokeOval(layerX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);
            }
        }
        
        // Output-Neuronen zeichnen
        layerIdx = layers.length - 1;
        layerSize = layers[layerIdx];
        layerX = horizontalSpacing * (layerIdx + 1);
        vSpacing = Math.max(minVerticalSpacing, 
                Math.min(maxVerticalSpacing, height / (layerSize + 1)));
        layerYOffset = (height - layerSize * vSpacing) / 2;
        
        for (int i = 0; i < layerSize; i++) {
            double nodeY = layerYOffset + vSpacing * (i + 0.5);
            
            // Aktivierungswert direkt vom Output-Neuron holen
            double activation = 0;
            if (i < network.getOutputNeuronList().size()) {
                activation = network.getOutputNeuronList().get(i).getValue();
                // Aktivierungswert auf den Bereich [0,1] begrenzen
                activation = Math.max(0, Math.min(1, activation));
            }
            
            // Farbe basierend auf Aktivierung (blau bis rot)
            Color nodeColor = Color.rgb(
                (int) (255 * activation),
                0,
                (int) (255 * (1 - activation))
            );
            
            // Neuron zeichnen
            gc.setFill(nodeColor);
            gc.fillOval(layerX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);
            gc.setStroke(Color.BLACK);
            gc.strokeOval(layerX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);
        }
        
        // Beschriftungen der Schichten mit dynamischer Schriftgröße
        double fontSize = Math.min(12, Math.max(9, width / 50));
        gc.setFont(new javafx.scene.text.Font(fontSize));
        gc.setFill(Color.WHITE);
        gc.fillText("Input", horizontalSpacing - 20, height - 10);
        
        if (layers.length > 2) {
            gc.fillText("Hidden", width / 2 - 20, height - 10);
        }
        
        gc.fillText("Output", width - horizontalSpacing - 20, height - 10);

        gc.restore(); // Transformation zurücksetzen
    }
}
