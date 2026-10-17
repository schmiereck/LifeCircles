package de.lifecircles.view;

import de.lifecircles.service.SimulationConfig;
import de.lifecircles.model.EnergyType;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Live controls for environmental energy supply. */
public class EnvironmentPanel extends VBox {
    public EnvironmentPanel() {
        SimulationConfig config = SimulationConfig.getInstance();
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #333333;");
        Label title = new Label("Sunlight (rays / simulation second)");
        Label value = new Label();
        Slider rate = new Slider(0, Math.max(100, config.getSunRayRate()), config.getSunRayRate());
        rate.setShowTickLabels(true);
        rate.setShowTickMarks(true);
        rate.setMajorTickUnit(25);
        rate.setBlockIncrement(1);
        rate.setPrefWidth(270);
        Runnable refresh = () -> value.setText(String.format("%.1f rays/s per %.0f px of world width",
                config.getSunRayRate(), config.getSunRaySpacingPx()));
        rate.valueProperty().addListener((obs, old, next) -> {
            config.setSunRayRate(next.doubleValue());
            refresh.run();
        });
        refresh.run();
        Label hint = new Label("0 = no sunlight; energy per ray stays constant.\nActual ray count also follows sun intensity.");
        ChoiceBox<EnergyType> sourceType = new ChoiceBox<>();
        sourceType.setId("sun-energy-type");
        sourceType.getItems().setAll(EnergyType.ALL);
        sourceType.setConverter(new StringConverter<>() {
            @Override public String toString(EnergyType type) { return type == null ? "" : type.getDisplayName(); }
            @Override public EnergyType fromString(String text) {
                return EnergyType.ALL.stream().filter(type -> type.getDisplayName().equals(text)).findFirst().orElseThrow();
            }
        });
        sourceType.setValue(config.getSunEnergyType());
        sourceType.valueProperty().addListener((obs, old, next) -> config.setSunEnergyType(next));
        VBox content = new VBox(8, title, rate, value, hint, new Label("Sun energy form:"), sourceType);
        content.setPadding(new Insets(5));
        getChildren().add(new TitledPane("Environment", content));
    }
}
