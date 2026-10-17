package de.lifecircles.view;

import de.lifecircles.service.SimulationConfig;
import de.lifecircles.model.EnergyType;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
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
        CheckBox zones = new CheckBox("Spectral zones: 000 | 001 | 010 | 100\n(narrow mixed borders, form chosen where a ray hits)");
        zones.setId("sun-spectral-zones");
        zones.setSelected(config.isSunSpectralZones());
        sourceType.setDisable(zones.isSelected());
        zones.selectedProperty().addListener((obs, old, next) -> {
            config.setSunSpectralZones(next);
            sourceType.setDisable(next);
        });
        Label seasonValue = new Label();
        Slider seasonLength = new Slider(0, 60, config.getSeasonDuration() / 60);
        seasonLength.setId("season-length");
        seasonLength.setShowTickLabels(true);
        seasonLength.setShowTickMarks(true);
        seasonLength.setMajorTickUnit(15);
        seasonLength.setPrefWidth(270);
        Slider winter = new Slider(0, 100, config.getWinterIntensity() * 100);
        winter.setId("winter-intensity");
        winter.setShowTickLabels(true);
        winter.setShowTickMarks(true);
        winter.setMajorTickUnit(25);
        winter.setPrefWidth(270);
        Runnable refreshSeason = () -> seasonValue.setText(config.getSeasonDuration() == 0 ? "Seasons off (0 = constant summer)"
                : String.format("Year: %.1f min · winter light %.0f %% of summer", config.getSeasonDuration() / 60,
                        config.getWinterIntensity() * 100));
        seasonLength.valueProperty().addListener((obs, old, next) -> {
            config.setSeasonDuration(next.doubleValue() * 60);
            refreshSeason.run();
        });
        winter.valueProperty().addListener((obs, old, next) -> {
            config.setWinterIntensity(next.doubleValue() / 100);
            refreshSeason.run();
        });
        refreshSeason.run();
        // Durable adhesion (ZELLVERBINDUNGEN.md); acts only in open NONE worlds.
        CheckBox adhesion = new CheckBox("Cell adhesion: forces held during brain pause,\nattraction only between matching actor colour and body type");
        adhesion.setId("cell-adhesion");
        adhesion.setSelected(config.isCellAdhesion());
        adhesion.selectedProperty().addListener((obs, old, next) -> config.setCellAdhesion(next));
        Label adhesionValue = new Label();
        // The last slider position means "never": dead cells keep carrying like wood.
        Slider remainsHalfLife = new Slider(1, 61, Double.isInfinite(config.getRemainsForceHalfLife()) ? 61
                : Math.min(60, config.getRemainsForceHalfLife() / 60));
        remainsHalfLife.setId("remains-force-half-life");
        remainsHalfLife.setShowTickLabels(true);
        remainsHalfLife.setShowTickMarks(true);
        remainsHalfLife.setMajorTickUnit(15);
        remainsHalfLife.setPrefWidth(270);
        Slider rejection = new Slider(0, 100, config.getAdhesionRejection() * 100);
        rejection.setId("adhesion-rejection");
        rejection.setShowTickLabels(true);
        rejection.setShowTickMarks(true);
        rejection.setMajorTickUnit(25);
        rejection.setPrefWidth(270);
        Runnable refreshAdhesion = () -> adhesionValue.setText(String.format("Remains hold: %s · foreign rejection %.0f %%",
                Double.isInfinite(config.getRemainsForceHalfLife()) ? "forever (wood)"
                        : String.format("half-life %.1f min", config.getRemainsForceHalfLife() / 60),
                config.getAdhesionRejection() * 100));
        remainsHalfLife.valueProperty().addListener((obs, old, next) -> {
            config.setRemainsForceHalfLife(next.doubleValue() > 60.5 ? Double.POSITIVE_INFINITY : next.doubleValue() * 60);
            refreshAdhesion.run();
        });
        rejection.valueProperty().addListener((obs, old, next) -> {
            config.setAdhesionRejection(next.doubleValue() / 100);
            refreshAdhesion.run();
        });
        refreshAdhesion.run();
        VBox content = new VBox(8, title, rate, value, hint, new Label("Sun energy form:"), sourceType, zones,
                new Label("Year length (simulation minutes):"), seasonLength,
                new Label("Winter light (% of summer):"), winter, seasonValue,
                adhesion, new Label("Remains force half-life (minutes, right end = forever):"), remainsHalfLife,
                new Label("Rejection of foreign attraction (%):"), rejection, adhesionValue);
        content.setPadding(new Insets(5));
        getChildren().add(new TitledPane("Environment", content));
    }
}
