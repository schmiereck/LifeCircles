package de.lifecircles.view;

import de.lifecircles.model.reproduction.ReproductionManager;
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

        // Energy threshold
        Label energyLabel = new Label("Energy Threshold:");
        energyLabel.setStyle("-fx-text-fill: black;");
        Slider energySlider = new Slider(0.1, 1.0, ReproductionManager.getEnergyThreshold());
        energySlider.setShowTickLabels(true);
        energySlider.valueProperty().addListener((obs, old, newValue) -> 
            ReproductionManager.setEnergyThreshold(newValue.doubleValue()));

        // Mutation rate
        Label mutationLabel = new Label("Mutation Rate:");
        mutationLabel.setStyle("-fx-text-fill: black;");
        Slider mutationSlider = new Slider(0.0, 0.5, ReproductionManager.getMutationRate());
        mutationSlider.setShowTickLabels(true);
        mutationSlider.valueProperty().addListener((obs, old, newValue) -> 
            ReproductionManager.setMutationRate(newValue.doubleValue()));

        // Reproduction desire threshold
        Label desireLabel = new Label("Reproduction Desire Threshold:");
        desireLabel.setStyle("-fx-text-fill: black;");
        Slider desireSlider = new Slider(0.0, 1.0, ReproductionManager.getReproductionDesireThreshold());
        desireSlider.setShowTickLabels(true);
        desireSlider.valueProperty().addListener((obs, old, newValue) -> 
            ReproductionManager.setReproductionDesireThreshold(newValue.doubleValue()));

        grid.addRow(0, energyLabel, energySlider);
        grid.addRow(1, mutationLabel, mutationSlider);
        grid.addRow(2, desireLabel, desireSlider);

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
