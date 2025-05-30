package de.lifecircles;

import de.lifecircles.model.Environment;
import de.lifecircles.service.CalculationService;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.view.ConfigPanel;
import de.lifecircles.view.SimulationView;
import de.lifecircles.view.StatisticsPanel;
import de.lifecircles.view.ViewConfig;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.prefs.Preferences;

/**
 * Main controller for the application.
 * Manages the simulation lifecycle and UI components.
 */
public class MainController extends BorderPane {
    private static final String LAST_FILE_PATH_KEY = "lastFilePath";
    private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);
    private final CalculationService calculationService;
    private final SimulationView simulationView;
    private final SimulationConfig simulationConfig;
    private final ViewConfig viewConfig;

    public MainController() {
        this.simulationConfig = SimulationConfig.getInstance();
        this.viewConfig = ViewConfig.getInstance();
        this.calculationService = new CalculationService();
        this.simulationView = new SimulationView(calculationService);

        // Create right side panels
        VBox rightPanels = new VBox(10);
        rightPanels.setPrefWidth(250);

        // Add config panel
        ConfigPanel configPanel = new ConfigPanel();
        
        // Add statistics panel
        StatisticsPanel statisticsPanel = new StatisticsPanel();

        rightPanels.getChildren().addAll(configPanel, statisticsPanel);
        setRight(rightPanels);

        setupUI();
    }

    private void setupUI() {
        // Create toolbar with controls
        ToolBar toolbar = createToolbar();
        setTop(toolbar);

        // Add simulation view in center
        setCenter(simulationView);
        simulationView.setStyle("-fx-background-color: black;");

        // Add status bar at bottom
        HBox statusBar = createStatusBar();
        setBottom(statusBar);
    }

    private ToolBar createToolbar() {
        Button startButton = new Button("Start");
        Button pauseButton = new Button("Pause");
        Button resetButton = new Button("Reset");
        Button saveButton = new Button("Save as");
        Button saveBestButton = new Button("Save best as");
        Button loadButton = new Button("Load");
        Button loadMergeButton = new Button("Load & Merge");

        startButton.setOnAction(e -> {
            calculationService.start();
            startButton.setDisable(true);
            pauseButton.setDisable(false);
        });

        pauseButton.setOnAction(e -> {
            calculationService.pause();
            startButton.setDisable(false);
            pauseButton.setDisable(true);
        });
        pauseButton.setDisable(true);

        resetButton.setOnAction(e -> {
            this.calculationService.stop();
            this.calculationService.resetSimulation();
            this.calculationService.start();
        });

        saveButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Cells");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cell Files", "*.cells"));
            String lastFilePath = preferences.get(LAST_FILE_PATH_KEY, null);
            if (lastFilePath != null) {
                fileChooser.setInitialDirectory(new File(lastFilePath).getParentFile());
            }
            File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                try {
                    Environment.getInstance().saveCellsToFile(file.getAbsolutePath());
                    preferences.put(LAST_FILE_PATH_KEY, file.getAbsolutePath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        saveBestButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Best Cells");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cell Files", "*.cells"));
            String lastFilePath = preferences.get(LAST_FILE_PATH_KEY, null);
            if (lastFilePath != null) {
                fileChooser.setInitialDirectory(new File(lastFilePath).getParentFile());
            }
            File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                try {
                    Environment.getInstance().saveBestCellsToFile(file.getAbsolutePath());
                    preferences.put(LAST_FILE_PATH_KEY, file.getAbsolutePath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        loadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Cells");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cell Files", "*.cells"));
            String lastFilePath = preferences.get(LAST_FILE_PATH_KEY, null);
            if (lastFilePath != null) {
                fileChooser.setInitialDirectory(new File(lastFilePath).getParentFile());
            }
            File file = fileChooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                try {
                    Environment.getInstance().loadCellsFromFile(file.getAbsolutePath());
                    preferences.put(LAST_FILE_PATH_KEY, file.getAbsolutePath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        loadMergeButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load & Merge Cells");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cell Files", "*.cells"));
            String lastFilePath = preferences.get(LAST_FILE_PATH_KEY, null);
            if (lastFilePath != null) {
                fileChooser.setInitialDirectory(new File(lastFilePath).getParentFile());
            }
            File file = fileChooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                try {
                    Environment.getInstance().loadAndMergeCellsFromFile(file.getAbsolutePath());
                    preferences.put(LAST_FILE_PATH_KEY, file.getAbsolutePath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Simulation speed control
        Label speedLabel = new Label("Speed:");
        Slider speedSlider = new Slider(0.1, 4.0, 1.0D);
        Label sliderValueLabel = new Label(String.format("%.1f", speedSlider.getValue() * SimulationConfig.FPS));
        speedSlider.setBlockIncrement(0.1);
        speedSlider.valueProperty().addListener((obs, old, newValue) -> {
            final double value = newValue.doubleValue();
            simulationConfig.setRunTimeStep(0.016666 / value);
            sliderValueLabel.setText(String.format("%.1f", value * SimulationConfig.FPS));
        });
        HBox speedLayout = new HBox(10, speedSlider, sliderValueLabel);

        return new ToolBar(
            startButton, pauseButton, resetButton,
            saveButton, saveBestButton, loadButton, loadMergeButton,
            new Label(" | "),
            speedLabel, speedLayout //speedSlider
        );
    }

    private HBox createStatusBar() {
        Label calcFpsLabel = new Label("Calc FPS: 0.0");
        Label stepCountLabel = new Label("Steps: 0");
        Label renderFpsLabel = new Label("Render FPS: 0.0");

        // Update status every 500ms
        Thread statusUpdater = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    double calcFps = calculationService.getFps();
                    long steps = calculationService.getStepCount();
                    double renderFps = simulationView.getFps();
                    double targetFps = 1.0 / this.simulationConfig.getRunTimeStep(); // Zielwert in FPS
                    javafx.application.Platform.runLater(() -> {
                        calcFpsLabel.setText(String.format("Calc FPS: %.1f / %.1f", calcFps, targetFps));
                        stepCountLabel.setText(String.format("Steps: %d", steps));
                        renderFpsLabel.setText(String.format("Render FPS: %.1f", renderFps));
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statusUpdater.setDaemon(true);
        statusUpdater.start();

        HBox statusBar = new HBox(10, calcFpsLabel, stepCountLabel, renderFpsLabel);
        statusBar.setStyle("-fx-padding: 5; -fx-background-color: #333333; -fx-text-fill: white;");
        calcFpsLabel.setStyle("-fx-text-fill: white;");
        stepCountLabel.setStyle("-fx-text-fill: white;");
        renderFpsLabel.setStyle("-fx-text-fill: white;");
        
        return statusBar;
    }

    public void shutdown() {
        calculationService.stop();
    }
}

