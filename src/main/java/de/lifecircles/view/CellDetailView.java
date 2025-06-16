package de.lifecircles.view;

import de.lifecircles.model.Cell;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.*;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ein Fenster, das detaillierte Informationen zu einer ausgewählten Zelle anzeigt.
 */
public class CellDetailView extends Stage {

    private Cell currentCell;
    private final Canvas brainCanvas;
    private final Canvas cellCanvas; // Neue Canvas für die Zellansicht
    private final Label typeLabel;
    private final Label stateLabel;
    private final Label ageLabel;
    private final Label energyLabel;
    private final Label generationLabel;
    private final Label synapseCountLabel;
    private final Label cellSizeLabel;
    private AnimationTimer updateTimer;
    private boolean needsRedraw = true;
    private double zoomFactor = 1.0;
    private double panOffsetX = 0.0;
    private double panOffsetY = 0.0;
    private double lastMouseX;
    private double lastMouseY;

    // Timer für die Brain-Aktualisierung (in Nanosekunden)
    private long lastBrainUpdate = 0;
    private static final long BRAIN_UPDATE_INTERVAL = 1_000_000_000; // 1 Sekunde in Nanosekunden

    public CellDetailView() {
        setTitle("Zelldetails");

        // Haupt-Layout erstellen
        BorderPane mainLayout = new BorderPane();

        // Layout für die Mitte vorbereiten - zwei Spalten nebeneinander
        GridPane centerGridPane = new GridPane();
        centerGridPane.setPadding(new javafx.geometry.Insets(10));
        centerGridPane.setHgap(10); // Horizontaler Abstand zwischen den Komponenten

        // Canvas für die Visualisierung des neuronalen Netzes
        brainCanvas = new Canvas(400, 300);

        // Canvas für die Zellansicht - jetzt größer definiert
        cellCanvas = new Canvas(300, 300);

        // Labels für die Visualisierungen
        Label networkLabel = new Label("Neuronales Netzwerk (ZellBrain):");
        Label cellLabel = new Label("Zellansicht:");

        // Brain-Canvas VBox
        VBox brainBox = new VBox(5);
        brainBox.getChildren().addAll(networkLabel, brainCanvas);
        VBox.setVgrow(brainCanvas, Priority.ALWAYS);

        // Cell-Canvas VBox
        VBox cellBox = new VBox(5);
        cellBox.getChildren().addAll(cellLabel, cellCanvas);
        VBox.setVgrow(cellCanvas, Priority.ALWAYS);

        // Beide Canvas-Boxen ins Grid einfügen
        centerGridPane.add(brainBox, 0, 0);
        centerGridPane.add(cellBox, 1, 0);

        // Spaltengewichtung setzen (60% Gehirn, 40% Zelle)
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(60);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(40);
        centerGridPane.getColumnConstraints().addAll(col1, col2);

        // Beide Canvas sollen proportional zur verfügbaren Fläche wachsen
        GridPane.setHgrow(brainBox, Priority.ALWAYS);
        GridPane.setVgrow(brainBox, Priority.ALWAYS);
        GridPane.setHgrow(cellBox, Priority.ALWAYS);
        GridPane.setVgrow(cellBox, Priority.ALWAYS);

        // Info-Panel mit Zelleigenschaften
        GridPane infoPanel = new GridPane();
        infoPanel.setHgap(16);
        infoPanel.setVgap(4);
        infoPanel.setPadding(new javafx.geometry.Insets(10));

        // Labels für Zelleigenschaften
        typeLabel = new Label();
        stateLabel = new Label();
        ageLabel = new Label();
        energyLabel = new Label();
        generationLabel = new Label();
        synapseCountLabel = new Label();
        cellSizeLabel = new Label();

        // Zweispaltiges Layout: links und rechts jeweils 4 Details
        // Spalte 0/1: links, Spalte 2/3: rechts
        infoPanel.add(new Label("Typ:"), 0, 0);
        infoPanel.add(typeLabel, 1, 0);
        infoPanel.add(new Label("Status:"), 0, 1);
        infoPanel.add(stateLabel, 1, 1);
        infoPanel.add(new Label("Alter:"), 0, 2);
        infoPanel.add(ageLabel, 1, 2);
        infoPanel.add(new Label("Energie:"), 0, 3);
        infoPanel.add(energyLabel, 1, 3);

        infoPanel.add(new Label("Generation:"), 2, 0);
        infoPanel.add(generationLabel, 3, 0);
        infoPanel.add(new Label("Synapsen:"), 2, 1);
        infoPanel.add(synapseCountLabel, 3, 1);
        infoPanel.add(new Label("Zellgröße:"), 2, 2);
        infoPanel.add(cellSizeLabel, 3, 2);
        // Platz für weitere Details in Zeile 3, Spalte 2/3

        // Haupt-Layout zusammenbauen
        mainLayout.setCenter(centerGridPane);
        mainLayout.setBottom(infoPanel);

        // Szene erstellen und Stage konfigurieren
        Scene scene = new Scene(mainLayout, 800, 600);
        setScene(scene);
        setResizable(true);

        // Animation-Timer für Updates der Zelleigenschaften und Netzwerkvisualisierung
        updateTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (currentCell != null) {
                    // Zellinformationen aktualisieren (ca. 10 mal pro Sekunde)
                    if (now - lastUpdate > 100_000_000) {
                        updateCellInfo();
                        lastUpdate = now;
                    }

                    // Neuronales Netzwerk und Zellansicht nur einmal pro Sekunde aktualisieren
                    // oder wenn eine Neuzeichnung angefordert wurde
                    if (now - lastBrainUpdate > BRAIN_UPDATE_INTERVAL || needsRedraw) {
                        renderBrain();
                        renderCell(); // Neue Methode zum Rendern der Zellansicht
                        lastBrainUpdate = now;
                        needsRedraw = false;
                    }
                }
            }
        };

        // Direkt an Fensteränderungen binden statt an Container-Änderungen
        // Dies funktioniert zuverlässiger, besonders beim Verkleinern
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (brainCanvas != null && cellCanvas != null) {
                // Padding berücksichtigen und Canvas-Breite anpassen
                double totalWidth = newVal.doubleValue() - 40; // Außenrand berücksichtigen

                // Aufteilung der Breite zwischen Brain-Canvas (60%) und Cell-Canvas (40%)
                if (totalWidth > 0) {
                    double brainCanvasWidth = totalWidth * 0.55; // 60% - etwas weniger wegen Innenabstand
                    brainCanvas.setWidth(brainCanvasWidth);

                    double cellCanvasWidth = totalWidth * 0.35; // 40% - etwas weniger wegen Innenabstand
                    cellCanvas.setWidth(cellCanvasWidth);

                    needsRedraw = true;

                    // Sofortige Neuzeichnung erzwingen, wenn eine Zelle aktiv ist
                    if (currentCell != null) {
                        renderBrain();
                        renderCell();
                    }
                }
            }
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (brainCanvas != null && cellCanvas != null) {
                // Feste Höhe für Labels und Padding verwenden
                double reservedSpace = 150; // Platz für Labels, Infobereich und Padding
                double availableHeight = newVal.doubleValue() - reservedSpace;

                if (availableHeight > 0) {
                    // Beide Canvas erhalten die gleiche Höhe
                    brainCanvas.setHeight(availableHeight);
                    cellCanvas.setHeight(availableHeight);

                    needsRedraw = true;

                    // Sofortige Neuzeichnung erzwingen, wenn eine Zelle aktiv ist
                    if (currentCell != null) {
                        renderBrain();
                        renderCell();
                    }
                }
            }
        });

        // Initialisiere die Canvas-Größe mit aktuellen Fenstermaßen
        double initialWidth = scene.getWidth() - 40;
        double brainCanvasWidth = initialWidth * 0.55; // 60% - etwas weniger wegen Innenabstand
        double cellCanvasWidth = initialWidth * 0.35;  // 40% - etwas weniger wegen Innenabstand

        brainCanvas.setWidth(brainCanvasWidth);
        brainCanvas.setHeight(scene.getHeight() - 150);

        cellCanvas.setWidth(cellCanvasWidth);
        cellCanvas.setHeight(scene.getHeight() - 150);

        // Wenn das Fenster geschlossen wird, Timer anhalten
        setOnCloseRequest(e -> {
            if (updateTimer != null) {
                updateTimer.stop();
            }
            this.currentCell = null;
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
        lastBrainUpdate = System.nanoTime(); // Zeit der letzten Brain-Aktualisierung setzen

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
        // NEU: Synapsen-Anzahl anzeigen
        int synapseCount = 0;
        long proccessedSynapses = 0;
        if (currentCell.getBrain() != null) {
            synapseCount = currentCell.getBrain().getSynapseCount();
            proccessedSynapses = currentCell.getBrain().getProccessedSynapses();
        }
        synapseCountLabel.setText("%d / %d".formatted(proccessedSynapses, synapseCount));
        // NEU: Zellgröße anzeigen
        cellSizeLabel.setText(String.format("%.2f", currentCell.getRadiusSize()));
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

        CellBrainInterface brainInterface = currentCell.getBrain();
        if (brainInterface == null) {
            return;
        }

        if (!(brainInterface instanceof CellBrain)) {
            return;
        }

        CellBrain brain = (CellBrain) brainInterface;

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
        //gc.setFill(Color.rgb(30, 30, 30));
        //gc.fillRect(0, 0, width, height);

        // Synapse-Liste vom Netzwerk holen
        List<Synapse> synapseList = network.getSynapseList();

        // Positions-Map für Neuronen erstellen: Speichert die X,Y-Position für jedes Neuron
        Map<Neuron, double[]> neuronPositions = new HashMap<>();

        // Input-Neuronen-Positionen
        int layerIdx = 0;
        int layerSize = layers[layerIdx];
        double layerX = horizontalSpacing * (layerIdx + 1);
        double vSpacing = Math.max(minVerticalSpacing,
                Math.min(maxVerticalSpacing, height / (layerSize + 1)));
        double layerYOffset = (height - layerSize * vSpacing) / 2;

        for (int layerPos = 0; layerPos < layerSize; layerPos++) {
            double nodeY = layerYOffset + vSpacing * (layerPos + 0.5);
            if (layerPos < network.getInputNeuronArr().length) {
                neuronPositions.put(network.getInputNeuronArr()[layerPos], new double[] {layerX, nodeY});
            }
        }

        // Hidden-Layer-Neuronen-Positionen
        for (int li = 0; li < network.getHiddenLayerArr().length; li++) {
            layerIdx = li + 1;
            layerSize = layers[layerIdx];
            layerX = horizontalSpacing * (layerIdx + 1);
            vSpacing = Math.max(minVerticalSpacing,
                    Math.min(maxVerticalSpacing, height / (layerSize + 1)));
            layerYOffset = (height - layerSize * vSpacing) / 2;

            Layer currentHiddenLayer = network.getHiddenLayerArr()[li];
            boolean layerIsActive = currentHiddenLayer.isActiveLayer();
            List<Neuron> neurons = currentHiddenLayer.getNeuronList();

            final int outputTypePos = 0; // Default-Output-Type für Input-Neuronen.
            for (int i = 0; i < layerSize; i++) {
                double nodeY = layerYOffset + vSpacing * (i + 0.5);
                if (i < neurons.size()) {
                    neuronPositions.put(neurons.get(i), new double[] {layerX, nodeY});
                }
                // Aktivierungswert direkt vom Hidden-Neuron holen
                double activation = 0;
                if (i < neurons.size()) {
                    activation = network.readNeuronValue(neurons.get(i), outputTypePos);
                    activation = Math.max(0, Math.min(1, activation));
                }

                // Farbe basierend auf Aktivierung (blau bis rot)
                Color nodeColor = Color.rgb(
                        (int) (255 * activation),
                        0,
                        (int) (255 * (1 - activation))
                );

                final Color strokeColor;
                if (!layerIsActive) {
                    nodeColor = nodeColor.desaturate();
                    strokeColor = Color.GRAY;
                } else {
                    strokeColor = Color.GREEN;
                }

                gc.setFill(nodeColor);
                gc.fillOval(layerX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);
                gc.setStroke(strokeColor);
                gc.setLineWidth(0.3);
                gc.strokeOval(layerX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);
            }
        }

        // Output-Neuronen-Positionen
        layerIdx = layers.length - 1;
        layerSize = layers[layerIdx];
        layerX = horizontalSpacing * (layerIdx + 1);
        vSpacing = Math.max(minVerticalSpacing,
                Math.min(maxVerticalSpacing, height / (layerSize + 1)));
        layerYOffset = (height - layerSize * vSpacing) / 2;

        for (int i = 0; i < layerSize; i++) {
            double nodeY = layerYOffset + vSpacing * (i + 0.5);
            if (i < network.getOutputNeuronArr().length) {
                neuronPositions.put(network.getOutputNeuronArr()[i], new double[] {layerX, nodeY});
            }
        }

        // Verbindungen zeichnen und entsprechend ihres Gewichts einfärben
        gc.setLineWidth(0.02D * screenSizeFactor);

        for (Synapse synapse : synapseList) {
            Neuron source = synapse.getSourceNeuron();
            Neuron target = synapse.getTargetNeuron();

            // Positionen der Neuronen abrufen
            double[] sourcePos = neuronPositions.get(source);
            double[] targetPos = neuronPositions.get(target);

            if (sourcePos != null && targetPos != null) {
                // Gewicht der Synapse holen und auf Farbe abbilden
                double weight = synapse.getWeight();
                Color synapseColor = this.weightToColor(weight);

                // Synapse mit entsprechender Farbe zeichnen
                gc.setStroke(synapseColor);
                gc.strokeLine(sourcePos[0], sourcePos[1], targetPos[0], targetPos[1]);
            }
        }

        // Input-Neuronen zeichnen
        layerIdx = 0;
        layerSize = layers[layerIdx];
        layerX = horizontalSpacing * (layerIdx + 1);
        vSpacing = Math.max(minVerticalSpacing,
                Math.min(maxVerticalSpacing, height / (layerSize + 1)));
        layerYOffset = (height - layerSize * vSpacing) / 2;

        final int outputTypePos = 0; // Default-Output-Type für Input-Neuronen.
        for (int layerPos = 0; layerPos < layerSize; layerPos++) {
            double nodeY = layerYOffset + vSpacing * (layerPos + 0.5);

            // Aktivierungswert direkt vom Input-Neuron holen
            double activation = 0;
            if (layerPos < network.getInputNeuronArr().length) {
                activation = network.readNeuronValue(network.getInputNeuronArr()[layerPos], outputTypePos);
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
        for (int li = 0; li < network.getHiddenLayerArr().length; li++) {
            layerIdx = li + 1; // +1 weil Input-Layer bereits gezeichnet wurde
            layerSize = layers[layerIdx];
            layerX = horizontalSpacing * (layerIdx + 1);
            vSpacing = Math.max(minVerticalSpacing,
                    Math.min(maxVerticalSpacing, height / (layerSize + 1)));
            layerYOffset = (height - layerSize * vSpacing) / 2;

            List<Neuron> neurons = network.getHiddenLayerArr()[li].getNeuronList();

            for (int i = 0; i < layerSize; i++) {
                double nodeY = layerYOffset + vSpacing * (i + 0.5);

                // Aktivierungswert direkt vom Hidden-Neuron holen
                double activation = 0;
                if (i < neurons.size()) {
                    activation = network.readNeuronValue(neurons.get(i), outputTypePos);
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
            if (i < network.getOutputNeuronArr().length) {
                activation = network.readNeuronValue(network.getOutputNeuronArr()[i], outputTypePos);
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

    /**
     * Visualisiert die Zelle mit allen SensorActors und ihren Feldern
     */
    private void renderCell() {
        if (currentCell == null) return;

        GraphicsContext gc = cellCanvas.getGraphicsContext2D();
        double width = cellCanvas.getWidth();
        double height = cellCanvas.getHeight();

        if (width <= 0 || height <= 0) return; // Verhindere Zeichnen bei ungültiger Größe

        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.rgb(20, 20, 30)); // Dunkler Hintergrund
        gc.fillRect(0, 0, width, height);

        // Zelle in die Mitte der Canvas platzieren mit geeignetem Zoom
        double cellSize = currentCell.getRadiusSize() * 2;
        double scale = Math.min(width, height) * 0.6 / cellSize; // 60% der Canvas nutzen

        // Transformiere das Koordinatensystem
        gc.save();
        gc.translate(width / 2, height / 2); // Mittelpunkt der Canvas
        gc.scale(scale, scale); // Vergrößere für bessere Sichtbarkeit

        // Erkannte Nachbar-Aktoren zuerst zeichnen (im Hintergrund)
        drawSensedActors(gc);

        // Zeichne die Zelle
        double[] rgb = {
            currentCell.getType().getRed(),
            currentCell.getType().getGreen(),
            currentCell.getType().getBlue()
        };

        // Zeichne die Zelle mit leichter Transparenz
        gc.setFill(Color.color(rgb[0], rgb[1], rgb[2], 0.5));
        gc.fillOval(
            -currentCell.getRadiusSize(),
            -currentCell.getRadiusSize(),
            currentCell.getRadiusSize() * 2,
            currentCell.getRadiusSize() * 2
        );

        // Umrandung der Zelle
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.2);
        gc.strokeOval(
            -currentCell.getRadiusSize(),
            -currentCell.getRadiusSize(),
            currentCell.getRadiusSize() * 2,
            currentCell.getRadiusSize() * 2
        );

        // Richtungsindikator zeichnen (Rotation der Zelle)
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(0.3);
        gc.strokeLine(0, 0, currentCell.getRadiusSize() * Math.cos(currentCell.getRotation()),
                currentCell.getRadiusSize() * Math.sin(currentCell.getRotation()));

        // SensorActors und ihre Kraftfelder zeichnen
        List<SensorActor> sensorActors = currentCell.getSensorActors();
        int actorCount = sensorActors.size();

        // Zeichne zuerst alle Kraftfelder
        for (SensorActor actor : sensorActors) {
            // SensorActor Position berechnen
            actor.updateCachedPosition(); // Stelle sicher, dass die Position aktuell ist
            Vector2D actorPos = actor.getCachedPosition();

            if (actorPos != null) {
                // Umrechnen in das Canvas-Koordinatensystem (relativ zum Mittelpunkt)
                double actorX = actorPos.getX() - currentCell.getPosition().getX();
                double actorY = actorPos.getY() - currentCell.getPosition().getY();

                // Radius für das Kraftfeld
                double radius = de.lifecircles.service.SensorActorForceCellCalcService.calcSensorRadius(
                        currentCell.getRadiusSize(), actorCount);

                // Färbung des Kraftfelds basierend auf der Kraftstärke
                final Color fieldColor;
                if (actor.getForceStrength() > 0) {
                    // Attraktionskraft - rot
                    fieldColor = Color.color(
                            1.0, 0, 0,
                            0.3 * Math.abs(actor.getForceStrength() /
                                  de.lifecircles.service.SimulationConfig.getInstance().getCellActorMaxAttractiveForceStrength())
                    );
                } else {
                    // Abstoßungskraft - grün
                    fieldColor = Color.color(
                            0, 1.0, 0,
                            0.3 * Math.abs(actor.getForceStrength() /
                                  de.lifecircles.service.SimulationConfig.getInstance().getCellActorMaxRepulsiveForceStrength())
                    );
                }

                // Kraftfeld als Kreis zeichnen
                gc.setFill(fieldColor);
                gc.fillOval(
                        actorX - radius,
                        actorY - radius,
                        radius * 2,
                        radius * 2
                );
            }
        }

        // Zeichne dann die SensorActors
        for (SensorActor actor : sensorActors) {
            Vector2D actorPos = actor.getCachedPosition();

            if (actorPos != null) {
                // Umrechnen in das Canvas-Koordinatensystem (relativ zum Mittelpunkt)
                double actorX = actorPos.getX() - currentCell.getPosition().getX();
                double actorY = actorPos.getY() - currentCell.getPosition().getY();

                double actorSize = 0.8; // Größe des SensorActors

                // Färbung des Sensors
                double[] actorRgb = {
                    actor.getType().getRed(),
                    actor.getType().getGreen(),
                    actor.getType().getBlue()
                };

                // SensorActor als kleineren Kreis zeichnen
                gc.setFill(Color.color(actorRgb[0], actorRgb[1], actorRgb[2]));
                gc.fillOval(
                        actorX - actorSize/2,
                        actorY - actorSize/2,
                        actorSize,
                        actorSize
                );

                // Umrandung
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(0.1);
                gc.strokeOval(
                        actorX - actorSize/2,
                        actorY - actorSize/2,
                        actorSize,
                        actorSize
                );

                // Bei aktivem Reproduktionswunsch einen Indikator zeichnen
                if (actor.getReproductionDesire() > 0.5) {
                    // Zeichne einen kleinen Stern oder Pfeil
                    gc.setFill(Color.PINK);
                    double starSize = actorSize * 0.5;
                    double[] xPoints = {
                            actorX, actorX - starSize/2, actorX + starSize/2
                    };
                    double[] yPoints = {
                            actorY - starSize, actorY - starSize/2, actorY - starSize/2
                    };
                    gc.fillPolygon(xPoints, yPoints, 3);
                }

                // Energieabsorption/Energieabgabe visualisieren
                if (actor.getEnergyAbsorption() > 0.1) {
                    gc.setStroke(Color.GREEN);
                    gc.setLineWidth(0.2);
                    double energySize = actorSize * actor.getEnergyAbsorption();
                    gc.strokeLine(actorX, actorY + actorSize/2, actorX, actorY + actorSize/2 + energySize);
                }

                if (actor.getEnergyDelivery() > 0.1) {
                    gc.setStroke(Color.RED);
                    gc.setLineWidth(0.2);
                    double energySize = actorSize * actor.getEnergyDelivery();
                    gc.strokeLine(actorX, actorY - actorSize/2, actorX, actorY - actorSize/2 - energySize);
                }

                // Verbindung zu erkanntem Aktor zeichnen
                if (actor.getSensedActor() != null && actor.getSensedActor() instanceof SensorActor) {
                    SensorActor sensedActor = (SensorActor) actor.getSensedActor();
                    sensedActor.updateCachedPosition();
                    Vector2D sensedActorPos = sensedActor.getCachedPosition();

                    if (sensedActorPos != null) {
                        // Verbindungslinie zeichnen
                        gc.setStroke(Color.YELLOW);
                        gc.setLineWidth(0.15);
                        gc.setLineDashes(0.4, 0.6); // Gestrichelte Linie

                        // Umrechnen der sensedActor-Position in das Canvas-System
                        double sensedActorX = sensedActorPos.getX() - currentCell.getPosition().getX();
                        double sensedActorY = sensedActorPos.getY() - currentCell.getPosition().getY();

                        gc.strokeLine(actorX, actorY, sensedActorX, sensedActorY);
                        gc.setLineDashes(null); // Linienart zurücksetzen
                    }
                }
            }
        }

        // Energielevel anzeigen
        double energyLevel = currentCell.getEnergy() / currentCell.getMaxEnergy();
        double barWidth = currentCell.getRadiusSize() * 1.0;
        double barHeight = currentCell.getRadiusSize() * 0.2;

        gc.setFill(Color.RED);
        gc.fillRect(-barWidth/2, currentCell.getRadiusSize() + 1, barWidth, barHeight);

        gc.setFill(Color.GREEN);
        gc.fillRect(-barWidth/2, currentCell.getRadiusSize() + 1, barWidth * energyLevel, barHeight);

        // Zelltitel mit Zelltypfarbe
        gc.setFill(Color.color(rgb[0], rgb[1], rgb[2], 0.9));
        gc.setFont(new javafx.scene.text.Font("Arial", 2));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("Zelle #" + currentCell.getCellState(), 0, -currentCell.getRadiusSize() - 2);

        // Status-Text hinzufügen
        gc.setFont(new javafx.scene.text.Font("Arial", 1.5));
        gc.fillText(String.format("E: %.1f  R: %.1f",
                    currentCell.getEnergy(),
                    currentCell.getRadiusSize()),
                    0, currentCell.getRadiusSize() + 4);

        gc.restore(); // Transformation zurücksetzen
    }

    /**
     * Zeichnet erkannte Nachbar-Aktoren und ihre Zellen
     */
    private void drawSensedActors(GraphicsContext gc) {
        if (currentCell == null) return;

        List<SensorActor> sensorActors = currentCell.getSensorActors();
        Map<Cell, Boolean> drawnCells = new HashMap<>(); // Speichert bereits gezeichnete Zellen

        for (SensorActor actor : sensorActors) {
            if (actor.getSensedActor() != null && actor.getSensedCell() != null) {
                if (actor.getSensedActor() instanceof SensorActor) {
                    SensorActor sensedActor = (SensorActor) actor.getSensedActor();
                    Cell sensedCell = null;

                    // Versuche, die Cell aus dem SensedCell zu bekommen
                    if (actor.getSensedCell() instanceof Cell) {
                        sensedCell = (Cell) actor.getSensedCell();

                        // Zeichne die Nachbarzelle nur einmal
                        if (!drawnCells.containsKey(sensedCell)) {
                            drawNeighborCell(gc, sensedCell);
                            drawnCells.put(sensedCell, true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Zeichnet eine erkannte Nachbarzelle auf die Canvas
     */
    private void drawNeighborCell(GraphicsContext gc, Cell neighborCell) {
        if (neighborCell == null || currentCell == null) return;

        // Position der Nachbarzelle relativ zur aktuellen Zelle berechnen
        Vector2D cellPos = currentCell.getPosition();
        Vector2D neighborPos = neighborCell.getPosition();
        double neighborX = neighborPos.getX() - cellPos.getX();
        double neighborY = neighborPos.getY() - cellPos.getY();
        double neighborRadius = neighborCell.getRadiusSize();

        // Zeichne die Nachbarzelle mit leichter Transparenz
        double[] rgb = {
            neighborCell.getType().getRed(),
            neighborCell.getType().getGreen(),
            neighborCell.getType().getBlue()
        };

        // Zeichne die Nachbarzelle halbtransparent im Hintergrund
        gc.setFill(Color.color(rgb[0], rgb[1], rgb[2], 0.1));
        gc.fillOval(
            neighborX - neighborRadius,
            neighborY - neighborRadius,
            neighborRadius * 2,
            neighborRadius * 2
        );

        // Dünne Umrandung der Nachbarzelle
        gc.setStroke(Color.color(rgb[0], rgb[1], rgb[2], 0.8).brighter());
        gc.setLineWidth(0.3);
        gc.strokeOval(
            neighborX - neighborRadius,
            neighborY - neighborRadius,
            neighborRadius * 2,
            neighborRadius * 2
        );

        // SensorActors der Nachbarzelle zeichnen
        List<SensorActor> neighborActors = neighborCell.getSensorActors();
        for (SensorActor actor : neighborActors) {
            actor.updateCachedPosition();
            Vector2D actorPos = actor.getCachedPosition();

            if (actorPos != null) {
                // Position relativ zur aktuellen Zelle
                double actorX = actorPos.getX() - cellPos.getX();
                double actorY = actorPos.getY() - cellPos.getY();

                double actorSize = 0.4; // Kleinere Größe für Nachbar-Aktoren

                // Aktualisierte Färbung des Nachbar-Sensors
                double[] actorRgb = {
                    actor.getType().getRed(),
                    actor.getType().getGreen(),
                    actor.getType().getBlue()
                };

                // Nachbar-SensorActor zeichnen
                gc.setFill(Color.color(actorRgb[0], actorRgb[1], actorRgb[2], 0.6));
                gc.fillOval(
                    actorX - actorSize/2,
                    actorY - actorSize/2,
                    actorSize,
                    actorSize
                );
            }
        }
    }

    /**
     * Konvertiert ein Synapsen-Gewicht in eine Farbe mit einer nichtlinearen Funktion.
     *
     * @param weight Gewicht der Synapse
     * @return Farbe basierend auf dem Gewicht (rot für negativ, blau für positiv)
     */
    private Color weightToColor(double weight) {
        // Nichtlineare Funktion anwenden: Hyperbeltangens (tanh)
        // Skaliert den Betrag der Gewichte nicht-linear auf [0,1]
        double scaledMagnitude = Math.tanh(Math.abs(weight) * 1.5);

        // Farbe basierend auf Vorzeichen wählen
        if (weight < 0) {
            // Negatives Gewicht: Rot mit Intensität basierend auf Betrag
            return Color.rgb((int) (255 * scaledMagnitude), 0, 0, 0.7);
        } else if (weight > 0) {
            // Positives Gewicht: Blau mit Intensität basierend auf Betrag
            return Color.rgb(0, (int) (200 * scaledMagnitude), (int) (255 * scaledMagnitude), 0.7);
        } else {
            // Gewicht = 0: Hellgrau
            return Color.rgb(150, 150, 150, 0.3);
        }
    }
}

