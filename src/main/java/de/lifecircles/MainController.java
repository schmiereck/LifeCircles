package de.lifecircles;

import de.lifecircles.model.Environment;
import de.lifecircles.service.CalculationService;
import de.lifecircles.service.FileService;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.service.SimulationSettingsFile;
import de.lifecircles.service.trainStrategy.TrainMode;
import javafx.scene.control.*;
import de.lifecircles.view.ConfigPanel;
import de.lifecircles.view.EnvironmentPanel;
import de.lifecircles.view.CellPanel;
import de.lifecircles.view.SimulationView;
import de.lifecircles.view.StatisticsPanel;
import de.lifecircles.view.ViewConfig;
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
    private static final String MERGE_SETTINGS_KEY = "mergeLoadsSettings";
    private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);
    private final CalculationService calculationService;
    private final SimulationView simulationView;
    private final SimulationConfig simulationConfig;
    private final ViewConfig viewConfig;
    private final FileService fileService;
    private final EnvironmentPanel environmentPanel = new EnvironmentPanel();
    private final ConfigPanel configPanel = new ConfigPanel();
    /** The cells file last loaded with "Load" or written with "Save as": shown in the title, offered by "Save as". */
    private String currentFile;

    public MainController() {
        this.simulationConfig = SimulationConfig.getInstance();
        this.viewConfig = ViewConfig.getInstance();
        this.calculationService = new CalculationService();
        this.simulationView = new SimulationView(calculationService);
        this.fileService = FileService.getInstance();

        setupUI();
    }

    private void setupUI() {
        // Create toolbar with controls
        ToolBar toolbar = createToolbar();
        setTop(toolbar);

        // Add simulation view in center
        setCenter(simulationView);
        simulationView.setStyle("-fx-background-color: black;");

        // Keep the whole world width available; scroll controls on narrow windows.
        HBox panels = new HBox(10, environmentPanel, new CellPanel(), configPanel, new StatisticsPanel());
        ScrollPane panelScroll = new ScrollPane(panels);
        panelScroll.setFitToHeight(true);
        panelScroll.setPrefViewportHeight(260);
        panelScroll.setMaxHeight(280);
        setBottom(new VBox(panelScroll, createStatusBar()));
    }

    private ToolBar createToolbar() {
        Button newButton = new Button("New…");
        newButton.setId("new-simulation-button");
        Button startButton = new Button("Start");
        Button pauseButton = new Button("Pause");
        Button resetButton = new Button("Reset");
        Button saveButton = new Button("Save as");
        Button saveBestButton = new Button("Save best as");
        Button loadButton = new Button("Load");
        Button loadMergeButton = new Button("Load & Merge");
        // Merging keeps the current environment by default; on request it takes the saved settings too.
        CheckBox mergeSettings = new CheckBox("with settings");
        mergeSettings.setId("merge-loads-settings");
        mergeSettings.setTooltip(new Tooltip("Load & Merge also applies the environment settings saved beside the file (step count stays)"));
        mergeSettings.setSelected(preferences.getBoolean(MERGE_SETTINGS_KEY, false));
        mergeSettings.selectedProperty().addListener((obs, old, next) -> preferences.putBoolean(MERGE_SETTINGS_KEY, next));

        newButton.setOnAction(e -> createScenarioDialog().showAndWait().ifPresent(mode -> {
            calculationService.newSimulation(mode);
            environmentPanel.syncFromConfig();
            configPanel.syncFromConfig();
            simulationView.resetWorldView();
            startButton.setDisable(false); pauseButton.setDisable(true);
            currentFile = null;
            updateTitle();
        }));

        startButton.setOnAction(e -> {
            calculationService.start();
            startButton.setDisable(true);
            pauseButton.setDisable(false);
        });

        pauseButton.setOnAction(e -> {
            this.calculationService.pause();
            startButton.setDisable(false);
            pauseButton.setDisable(true);
        });
        pauseButton.setDisable(true);

        resetButton.setOnAction(e -> {
            this.calculationService.pause();
            startButton.setDisable(false);
            pauseButton.setDisable(true);

            this.calculationService.resetSimulation();
            simulationView.resetWorldView();
        });

        saveButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Cells");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cell Files", "*.cells"));
            String lastFilePath = currentFile != null ? currentFile : preferences.get(LAST_FILE_PATH_KEY, null);
            if (lastFilePath != null) {
                fileChooser.setInitialDirectory(new File(lastFilePath).getParentFile());
            }
            // Offer the current file's name; the dialog asks before overwriting it.
            if (currentFile != null) fileChooser.setInitialFileName(new File(currentFile).getName());
            File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                try {
                    // Verwende die neue FileService-Methode mit Fortschrittsanzeige
                    fileService.saveCellsToFileWithProgress(file.getAbsolutePath(), Environment.getInstance().getCopyOfCellList());
                    SimulationSettingsFile.save(file.getAbsolutePath(), simulationConfig, calculationService.getStepCount());
                    preferences.put(LAST_FILE_PATH_KEY, file.getAbsolutePath());
                    currentFile = file.getAbsolutePath();
                    updateTitle();
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
                    // Verwende die FileService-Methode, die bereits Fortschrittsausgaben hat
                    fileService.saveBestCellsToFile(
                        file.getAbsolutePath(),
                        Environment.getInstance().getCopyOfCellList(),
                        Environment.getInstance().getWidth()
                    );
                    SimulationSettingsFile.save(file.getAbsolutePath(), simulationConfig, calculationService.getStepCount());
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
                    loadCells(file.getAbsolutePath());
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
                    mergeCells(file.getAbsolutePath(), mergeSettings.isSelected());
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
            newButton, startButton, pauseButton, resetButton,
            saveButton, saveBestButton, loadButton, loadMergeButton, mergeSettings,
            new Label(" | "),
            speedLabel, speedLayout //speedSlider
        );
    }

    /** Building the dialog has no side effects: Cancel keeps the current run intact. */
    Dialog<TrainMode> createScenarioDialog() {
        Dialog<TrainMode> dialog=new Dialog<>();
        dialog.setTitle("Neue Simulation");
        dialog.setHeaderText("Szenario wählen. Die aktuelle Welt wird beim Erstellen ersetzt.");
        if(getScene()!=null && getScene().getWindow()!=null) dialog.initOwner(getScene().getWindow());
        ComboBox<TrainMode> scenarios=new ComboBox<>(); scenarios.setId("new-scenario-choice");
        // EVOLUTIONARY is only a legacy alias of NONE, not a separate implemented strategy.
        for(TrainMode mode:TrainMode.values()) if(mode!=TrainMode.EVOLUTIONARY) scenarios.getItems().add(mode);
        scenarios.setValue(simulationConfig.getTrainMode()==TrainMode.EVOLUTIONARY?TrainMode.NONE:simulationConfig.getTrainMode());
        Label description=new Label(scenarios.getValue().description());
        description.setWrapText(true); description.setPrefWidth(450);
        scenarios.valueProperty().addListener((o,old,value)->description.setText(value.description()));
        dialog.getDialogPane().setContent(new VBox(12,scenarios,description,new Label("Die neue Welt startet pausiert. Weiter mit Start.")));
        ButtonType create=new ButtonType("Erstellen",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(create,ButtonType.CANCEL);
        dialog.setResultConverter(button->button==create?scenarios.getValue():null);
        return dialog;
    }

    private HBox createStatusBar() {
        Label calcFpsLabel = new Label("Calc FPS: 0.0");
        Label stepCountLabel = new Label("Steps: 0");
        Label renderFpsLabel = new Label("Render FPS: 0.0");
        Label trainingLabel = new Label(calculationService.getTrainingStatus());

        // Update status every 500ms
        Thread statusUpdater = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    double calcFps = calculationService.getFps();
                    long steps = calculationService.getStepCount();
                    double renderFps = simulationView.getFps();
                    String trainingStatus = calculationService.getTrainingStatus();
                    double targetFps = 1.0 / this.simulationConfig.getRunTimeStep(); // Zielwert in FPS
                    javafx.application.Platform.runLater(() -> {
                        calcFpsLabel.setText(String.format("Calc FPS: %.1f / %.1f", calcFps, targetFps));
                        stepCountLabel.setText(String.format(java.util.Locale.GERMANY, "Steps: %,d", steps));
                        renderFpsLabel.setText(String.format("Render FPS: %.1f", renderFps));
                        trainingLabel.setText(trainingStatus);
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statusUpdater.setDaemon(true);
        statusUpdater.start();

        HBox statusBar = new HBox(10, calcFpsLabel, stepCountLabel, renderFpsLabel, trainingLabel);
        statusBar.setStyle("-fx-padding: 5; -fx-background-color: #333333; -fx-text-fill: white;");
        calcFpsLabel.setStyle("-fx-text-fill: white;");
        stepCountLabel.setStyle("-fx-text-fill: white;");
        renderFpsLabel.setStyle("-fx-text-fill: white;");
        trainingLabel.setStyle("-fx-text-fill: white;");
        
        return statusBar;
    }

    /** Replaces the population and applies environment settings and step count saved beside it (if any). */
    void loadCells(String path) throws java.io.IOException, ClassNotFoundException {
        // Publish the loaded population between complete simulation steps.
        calculationService.loadCellsFromFile(path, false);
        currentFile = path;
        updateTitle();
        SimulationSettingsFile.Loaded settings = applySettings(path);
        if (settings.steps() >= 0) calculationService.setStepCount(settings.steps());
    }

    /** Adds the population; with settings also applies the saved environment, but keeps this run's step count. */
    void mergeCells(String path, boolean withSettings) throws java.io.IOException, ClassNotFoundException {
        // Merge through the simulation service so paused snapshots are refreshed too.
        calculationService.loadCellsFromFile(path, true);
        if (withSettings) applySettings(path);
    }

    private SimulationSettingsFile.Loaded applySettings(String path) throws java.io.IOException {
        SimulationSettingsFile.Loaded settings = SimulationSettingsFile.load(path, simulationConfig);
        if (!java.nio.file.Files.exists(SimulationSettingsFile.pathFor(path)))
            System.out.println("Settings: no " + SimulationSettingsFile.pathFor(path).getFileName() + ", current environment settings kept");
        else System.out.println("Settings: applied " + SimulationSettingsFile.pathFor(path).getFileName());
        settings.problems().forEach(problem -> System.out.println("Settings: " + problem));
        environmentPanel.syncFromConfig();
        configPanel.syncFromConfig();
        return settings;
    }

    String getCurrentFile() { return currentFile; }

    /** "LifeCircles Simulation - MODE - file.cells" (without a file after "New…"). */
    String title() {
        return "LifeCircles Simulation - " + simulationConfig.getTrainMode()
                + (currentFile == null ? "" : " - " + new File(currentFile).getName());
    }

    private void updateTitle() {
        if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage stage) stage.setTitle(title());
    }

    public void shutdown() {
        calculationService.stop();
    }
}

