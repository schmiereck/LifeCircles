package de.lifecircles.view;

import de.lifecircles.service.SimulationConfig;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Configuration panel for simulation parameters.
 */
public class ConfigPanel extends VBox {
    private final ViewConfig viewConfig;

    public ConfigPanel() {
        this.viewConfig = ViewConfig.getInstance();
        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-background-color: #333333;");

        // Reproduction settings
        TitledPane reproductionPane = createReproductionSettings();
        
        // Visualization settings
        TitledPane visualizationPane = createVisualizationSettings();

        getChildren().addAll(reproductionPane, visualizationPane);
    }

    private TitledPane createReproductionSettings() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(5));

        final SimulationConfig simulationConfig = SimulationConfig.getInstance();

        // Energy threshold
        Label energyLabel = new Label("Min. Energy:");
        energyLabel.setStyle("-fx-text-fill: black;");
        Slider energySlider = new Slider(0.1, 1.0, simulationConfig.getReproductionEnergyThreshold());
        energySlider.setShowTickLabels(true);
        energySlider.valueProperty().addListener((obs, old, newValue) ->
                simulationConfig.setReproductionEnergyThreshold(newValue.doubleValue()));

        // Mutation rate
        Label mutationLabel = new Label("Mutation Rate:");
        mutationLabel.setStyle("-fx-text-fill: black;");
        Slider mutationSlider = new Slider(0.0, 0.5, simulationConfig.getMutationRate());
        mutationSlider.setShowTickLabels(true);
        mutationSlider.valueProperty().addListener((obs, old, newValue) ->
                simulationConfig.setMutationRate(newValue.doubleValue()));

        // Mutation Strength
        Label strengthLabel = new Label("Mutation Strength:");
        strengthLabel.setStyle("-fx-text-fill: black;");
        Slider strengthSlider = new Slider(0.0, 0.5, simulationConfig.getMutationStrength());
        strengthSlider.setShowTickLabels(true);
        strengthSlider.valueProperty().addListener((obs, old, newValue) ->
                simulationConfig.setMutationStrength(newValue.doubleValue()));

        // Reproduction desire threshold
        Label desireLabel = new Label("Min. Desire:");
        desireLabel.setStyle("-fx-text-fill: black;");
        Slider desireSlider = new Slider(0.0, 1.0, simulationConfig.getReproductionDesireThreshold());
        desireSlider.setShowTickLabels(true);
        desireSlider.valueProperty().addListener((obs, old, newValue) ->
                simulationConfig.setReproductionDesireThreshold(newValue.doubleValue()));

        // Reproduction desire threshold
        Label ageLabel = new Label("Min. Age (s):");
        ageLabel.setStyle("-fx-text-fill: black;");
        Slider ageSlider = new Slider(0.0, 20.0, simulationConfig.getReproductionAgeThreshold());
        ageSlider.setShowTickLabels(true);
        ageSlider.valueProperty().addListener((obs, old, newValue) ->
                simulationConfig.setReproductionAgeThreshold(newValue.doubleValue()));

        grid.addRow(0, energyLabel, energySlider);
        grid.addRow(1, mutationLabel, mutationSlider);
        grid.addRow(2, strengthLabel, strengthSlider);
        grid.addRow(3, desireLabel, desireSlider);
        grid.addRow(4, ageLabel, ageSlider);

        TitledPane pane = new TitledPane("Reproduction", grid);
        pane.setCollapsible(true);
        pane.setStyle("-fx-text-fill: black;");
        return pane;
    }

    private TitledPane createVisualizationSettings() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(5));

        // Energy visualization
        CheckBox energyBox = new CheckBox("Show Energy");
        energyBox.setStyle("-fx-text-fill: black;");
        energyBox.setSelected(viewConfig.isShowEnergy());
        energyBox.selectedProperty().addListener((obs, old, newValue) -> 
            viewConfig.setShowEnergy(newValue));

        // Age visualization
        CheckBox ageBox = new CheckBox("Show Age");
        ageBox.setStyle("-fx-text-fill: black;");
        ageBox.setSelected(viewConfig.isShowAge());
        ageBox.selectedProperty().addListener((obs, old, newValue) -> 
            viewConfig.setShowAge(newValue));

        // Specialization indicators
        CheckBox specBox = new CheckBox("Show Specialization");
        specBox.setStyle("-fx-text-fill: black;");
        specBox.setSelected(viewConfig.isShowSpecialization());
        specBox.selectedProperty().addListener((obs, old, newValue) -> 
            viewConfig.setShowSpecialization(newValue));

        grid.addRow(0, energyBox);
        grid.addRow(1, ageBox);
        grid.addRow(2, specBox);

        TitledPane pane = new TitledPane("Visualization", grid);
        pane.setCollapsible(true);
        pane.setStyle("-fx-text-fill: black;");
        return pane;
    }
}
