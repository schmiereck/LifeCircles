package de.lifecircles.view;

import java.util.Objects;

import de.lifecircles.service.StatisticsManager;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Panel displaying simulation statistics and population graphs.
 */
public class StatisticsPanel extends VBox {
    private final StatisticsManager statistics;
    private final Label totalLabel = new Label();
    private final Label performanceLabel = new Label();
    private final Label clustersLabel = new Label();
    private final Canvas populationGraph;

    public StatisticsPanel() {
        this.statistics = StatisticsManager.getInstance();
        this.populationGraph = new Canvas(230, 150);

        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-background-color: #333333;");

        setupUI();
        startUpdateTimer();
    }

    private void setupUI() {
        // Population counts
        VBox countsBox = new VBox(5);
        countsBox.setPadding(new Insets(5));
        
        totalLabel.setStyle("-fx-text-fill: black;");
        performanceLabel.setStyle("-fx-text-fill: black;");
        clustersLabel.setStyle("-fx-text-fill: black;");
        
        countsBox.getChildren().addAll(
            totalLabel,
            performanceLabel,
            clustersLabel
        );

        TitledPane countsPane = new TitledPane("Population", countsBox);
        countsPane.setCollapsible(true);
        countsPane.setStyle("-fx-text-fill: black;");

        // Population graph
        populationGraph.setStyle("-fx-background-color: #1a1a1a;");
        TitledPane graphPane = new TitledPane("Population History", populationGraph);
        graphPane.setCollapsible(true);
        graphPane.setStyle("-fx-text-fill: black;");

        getChildren().addAll(countsPane, graphPane);
    }

    private void startUpdateTimer() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateLabels();
                updateGraph();
            }
        }.start();
    }

    private void updateLabels() {
        totalLabel.setText(String.format("Total: %d", statistics.getTotalPopulation()));
        performanceLabel.setText(String.format("Update Time: %.1f ms", statistics.getAverageUpdateTime()));
        clustersLabel.setText(String.format("Clusters: %d", statistics.getClusterHistories().size()));
    }

    private void updateGraph() {
        GraphicsContext gc = populationGraph.getGraphicsContext2D();
        gc.setFill(Color.rgb(26, 26, 26));
        gc.fillRect(0, 0, populationGraph.getWidth(), populationGraph.getHeight());

        // Get history data
        var totalHistory = statistics.getTotalHistory();

        if (totalHistory.isEmpty()) return;

        // Find max value for scaling
        int maxValue = totalHistory.stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(100);
        maxValue = Math.max(maxValue, 100); // Minimum scale

        // Draw grid
        gc.setStroke(Color.rgb(51, 51, 51));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = populationGraph.getHeight() * i / 4;
            gc.strokeLine(0, y, populationGraph.getWidth(), y);
        }

        // Draw stacked cluster lines
        var clusterHistories = statistics.getClusterHistories();
        int clusterCount = clusterHistories.size();
        int historySize = totalHistory.size();
        int[] cumulative = new int[historySize];
        for (int i = 0; i < clusterCount; i++) {
            var history = clusterHistories.get(i);
            java.util.List<Integer> stackedHistory = new java.util.ArrayList<>(historySize);
            for (int j = 0; j < historySize; j++) {
                int val = (j < history.size() && history.get(j) != null) ? history.get(j) : 0;
                cumulative[j] += val;
                stackedHistory.add(cumulative[j]);
            }
            // Draw cluster curve
            Color c = Color.hsb(360.0 * i / clusterCount, 0.7, 1.0);
            drawHistoryLine(gc, stackedHistory, maxValue, c);
        }
        // Draw total population line on top
        drawHistoryLine(gc, totalHistory, maxValue, Color.WHITE);
    }

    private void drawHistoryLine(GraphicsContext gc, java.util.List<Integer> history, int maxValue, Color color) {
        if (history.isEmpty()) return;

        gc.setStroke(color);
        gc.setLineWidth(2);

        double xStep = populationGraph.getWidth() / (history.size() - 1);
        double height = populationGraph.getHeight();

        gc.beginPath();
        for (int i = 0; i < history.size(); i++) {
            Integer value = history.get(i);
            if (Objects.nonNull(value)) {
                double x = i * xStep;
                double y = height * (1 - (double) value / maxValue);
                if (i == 0) {
                    gc.moveTo(x, y);
                } else {
                    gc.lineTo(x, y);
                }
            }
        }
        gc.stroke();
    }
}
