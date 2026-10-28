package de.lifecircles.view;

import de.lifecircles.service.SimulationConfig;
import de.lifecircles.model.EnergyType;
import de.lifecircles.service.brownian.BrownianModel;
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
    /** Shows config values a scenario set on creation (e.g. WORLD2 switches on zones and adhesion). */
    private final Runnable syncFromConfig;

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
        Label zoneWidthValue = new Label();
        Slider zoneWidth = new Slider(0, 3200, config.getSunZoneWidth());
        zoneWidth.setId("sun-zone-width");
        zoneWidth.setShowTickLabels(true);
        zoneWidth.setShowTickMarks(true);
        zoneWidth.setMajorTickUnit(800);
        zoneWidth.setBlockIncrement(100);
        zoneWidth.setPrefWidth(270);
        Runnable refreshZoneWidth = () -> zoneWidthValue.setText(config.getSunZoneWidth() == 0 ? "Zone width: one cycle per world (0)"
                : String.format("Zone width: %.0f px, zones repeat every %.0f px", config.getSunZoneWidth(), 4 * config.getSunZoneWidth()));
        zoneWidth.valueProperty().addListener((obs, old, next) -> {
            config.setSunZoneWidth(Math.round(next.doubleValue() / 100) * 100);
            refreshZoneWidth.run();
        });
        refreshZoneWidth.run();
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
        // Mechanics: gravity relative to the standard value, movement and rotation friction (damping rates).
        Label mechanicsValue = new Label();
        Slider gravity = new Slider(0, 300, config.getGravity() / SimulationConfig.GRAVITY * 100);
        gravity.setId("gravity");
        Slider friction = new Slider(0, 4, config.getViscosity());
        friction.setId("friction");
        Slider rotationFriction = new Slider(0, 4, config.getRotationalFriction());
        rotationFriction.setId("rotation-friction");
        for (Slider slider : new Slider[]{gravity, friction, rotationFriction}) {
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            slider.setPrefWidth(270);
        }
        gravity.setMajorTickUnit(100);
        friction.setMajorTickUnit(1);
        rotationFriction.setMajorTickUnit(1);
        Runnable refreshMechanics = () -> mechanicsValue.setText(String.format(
                "Gravity %.0f %% (%.1f) · friction %.2f · rotation friction %.2f",
                config.getGravity() / SimulationConfig.GRAVITY * 100, config.getGravity(), config.getViscosity(), config.getRotationalFriction()));
        gravity.valueProperty().addListener((obs, old, next) -> {
            config.setGravity(next.doubleValue() / 100 * SimulationConfig.GRAVITY);
            refreshMechanics.run();
        });
        friction.valueProperty().addListener((obs, old, next) -> {
            config.setViscosity(next.doubleValue());
            refreshMechanics.run();
        });
        rotationFriction.valueProperty().addListener((obs, old, next) -> {
            config.setRotationalFriction(next.doubleValue());
            refreshMechanics.run();
        });
        refreshMechanics.run();
        // Blocker friction (BODENREIBUNG.md), experimental: grip on terrain without anchors.
        CheckBox blockerFriction = new CheckBox("Friction where cells press on terrain (rolling, walking)");
        blockerFriction.setId("blocker-friction");
        blockerFriction.setSelected(config.isBlockerFriction());
        Label blockerFrictionValue = new Label();
        Slider blockerFrictionCoefficient = new Slider(0, 3, config.getBlockerFrictionCoefficient());
        blockerFrictionCoefficient.setId("blocker-friction-coefficient");
        blockerFrictionCoefficient.setShowTickLabels(true);
        blockerFrictionCoefficient.setShowTickMarks(true);
        blockerFrictionCoefficient.setMajorTickUnit(1);
        blockerFrictionCoefficient.setPrefWidth(270);
        Slider blockerRolling = new Slider(0, 1, config.getBlockerRollingResistance());
        blockerRolling.setId("blocker-rolling-resistance");
        blockerRolling.setShowTickLabels(true);
        blockerRolling.setShowTickMarks(true);
        blockerRolling.setMajorTickUnit(.25);
        blockerRolling.setPrefWidth(270);
        Runnable refreshBlockerFriction = () -> blockerFrictionValue.setText(String.format("Terrain friction %.2f · rolling resistance %.2f%s",
                config.getBlockerFrictionCoefficient(), config.getBlockerRollingResistance(), config.isBlockerFriction() ? "" : " (off)"));
        blockerFriction.selectedProperty().addListener((obs, old, next) -> {
            config.setBlockerFriction(next);
            refreshBlockerFriction.run();
        });
        blockerFrictionCoefficient.valueProperty().addListener((obs, old, next) -> {
            config.setBlockerFrictionCoefficient(next.doubleValue());
            refreshBlockerFriction.run();
        });
        blockerRolling.valueProperty().addListener((obs, old, next) -> {
            config.setBlockerRollingResistance(next.doubleValue());
            refreshBlockerFriction.run();
        });
        refreshBlockerFriction.run();
        // Brownian background motion (BROWNSCHE_BEWEGUNG.md): random hits without a current; RACE keeps it off.
        ChoiceBox<BrownianModel> brownianModel = new ChoiceBox<>();
        brownianModel.setId("brownian-model");
        brownianModel.getItems().addAll(BrownianModel.values());
        brownianModel.setValue(config.getBrownianModel());
        Slider brownianDensity = new Slider(0, 100, config.getBrownianDensity());
        brownianDensity.setId("brownian-density");
        brownianDensity.setShowTickLabels(true);
        brownianDensity.setShowTickMarks(true);
        brownianDensity.setMajorTickUnit(25);
        brownianDensity.setPrefWidth(270);
        Slider brownianStrength = new Slider(0, 50, config.getBrownianStrength());
        brownianStrength.setId("brownian-strength");
        brownianStrength.setShowTickLabels(true);
        brownianStrength.setShowTickMarks(true);
        brownianStrength.setMajorTickUnit(10);
        brownianStrength.setPrefWidth(270);
        Label brownianValue = new Label();
        Runnable refreshBrownian = () -> brownianValue.setText(String.format("Brownian %s: %.0f hits/s (radius 10 px) · %.1f px/s per hit",
                config.getBrownianModel(), config.getBrownianDensity(), config.getBrownianStrength()));
        brownianModel.valueProperty().addListener((obs, old, next) -> {
            if (next != null) config.setBrownianModel(next);
            refreshBrownian.run();
        });
        brownianDensity.valueProperty().addListener((obs, old, next) -> {
            config.setBrownianDensity(next.doubleValue());
            refreshBrownian.run();
        });
        brownianStrength.valueProperty().addListener((obs, old, next) -> {
            config.setBrownianStrength(next.doubleValue());
            refreshBrownian.run();
        });
        refreshBrownian.run();
        // Durable adhesion (ZELLVERBINDUNGEN.md); open NONE worlds, training worlds only with the opt-in.
        CheckBox adhesion = new CheckBox("Cell adhesion: forces held during brain pause,\nattraction only between matching actor colour and body type");
        adhesion.setId("cell-adhesion");
        adhesion.setSelected(config.isCellAdhesion());
        adhesion.selectedProperty().addListener((obs, old, next) -> config.setCellAdhesion(next));
        CheckBox trainingAdhesion = new CheckBox("Adhesion, bonds and ground hold also in training worlds");
        trainingAdhesion.setId("training-adhesion");
        trainingAdhesion.setSelected(config.isTrainingAdhesion());
        trainingAdhesion.selectedProperty().addListener((obs, old, next) -> config.setTrainingAdhesion(next));
        // Experimental stages 3/4, off by default; they need cell adhesion to act.
        CheckBox bonds = new CheckBox("Experimental: touching matching attractive actors lock into bonds");
        bonds.setId("adhesion-bonds");
        bonds.setSelected(config.isAdhesionBonds());
        bonds.selectedProperty().addListener((obs, old, next) -> config.setAdhesionBonds(next));
        CheckBox birthBonds = new CheckBox("Experimental: daughter stays bonded if the dividing actor attracts");
        birthBonds.setId("adhesion-birth-bonds");
        birthBonds.setSelected(config.isAdhesionBirthBonds());
        birthBonds.selectedProperty().addListener((obs, old, next) -> config.setAdhesionBirthBonds(next));
        // Ground hold (BODENHALT.md), experimental: surface fields and anchors.
        CheckBox groundHold = new CheckBox("Experimental: terrain attracts attracting actors (grey matches best)");
        groundHold.setId("ground-adhesion");
        groundHold.setSelected(config.isGroundAdhesion());
        groundHold.selectedProperty().addListener((obs, old, next) -> config.setGroundAdhesion(next));
        CheckBox anchors = new CheckBox("Experimental: actors gripping terrain for 0.5 s anchor there");
        anchors.setId("ground-anchors");
        anchors.setSelected(config.isGroundAnchors());
        anchors.selectedProperty().addListener((obs, old, next) -> config.setGroundAnchors(next));
        CheckBox groundOnly = new CheckBox("Anchors only on the ground (no climbing walls/platforms)");
        groundOnly.setId("ground-anchors-ground-only");
        groundOnly.setSelected(config.isGroundAnchorsGroundOnly());
        groundOnly.selectedProperty().addListener((obs, old, next) -> config.setGroundAnchorsGroundOnly(next));
        Label stickinessValue = new Label();
        double maxAttraction = config.getCellActorMaxAttractiveForceStrength();
        Slider stickiness = new Slider(0, 100, -config.getGroundStickiness() / maxAttraction * 100);
        stickiness.setId("ground-stickiness");
        stickiness.setShowTickLabels(true);
        stickiness.setShowTickMarks(true);
        stickiness.setMajorTickUnit(25);
        stickiness.setPrefWidth(270);
        Runnable refreshStickiness = () -> stickinessValue.setText(String.format("Terrain stickiness: %.0f %% of maximum attraction",
                -config.getGroundStickiness() / maxAttraction * 100));
        stickiness.valueProperty().addListener((obs, old, next) -> {
            config.setGroundStickiness(-next.doubleValue() / 100 * maxAttraction);
            refreshStickiness.run();
        });
        refreshStickiness.run();
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
        syncFromConfig = () -> {
            // Slider listeners write back (rounded, clamped); keep the exact config values and restore them afterwards.
            double sunRate = config.getSunRayRate(), width = config.getSunZoneWidth(), season = config.getSeasonDuration(),
                    winterShare = config.getWinterIntensity(), halfLife = config.getRemainsForceHalfLife(),
                    rejectionShare = config.getAdhesionRejection(), sticky = config.getGroundStickiness(),
                    gravityValue = config.getGravity(), frictionValue = config.getViscosity(), rotationValue = config.getRotationalFriction(),
                    terrainFriction = config.getBlockerFrictionCoefficient(), terrainRolling = config.getBlockerRollingResistance(),
                    hits = config.getBrownianDensity(), hitStrength = config.getBrownianStrength();
            rate.setMax(Math.max(rate.getMax(), sunRate)); rate.setValue(sunRate);
            sourceType.setValue(config.getSunEnergyType()); zones.setSelected(config.isSunSpectralZones());
            zoneWidth.setValue(width); seasonLength.setValue(season / 60); winter.setValue(winterShare * 100);
            adhesion.setSelected(config.isCellAdhesion()); trainingAdhesion.setSelected(config.isTrainingAdhesion());
            bonds.setSelected(config.isAdhesionBonds()); birthBonds.setSelected(config.isAdhesionBirthBonds());
            groundHold.setSelected(config.isGroundAdhesion()); anchors.setSelected(config.isGroundAnchors());
            groundOnly.setSelected(config.isGroundAnchorsGroundOnly()); blockerFriction.setSelected(config.isBlockerFriction());
            blockerFrictionCoefficient.setValue(terrainFriction); blockerRolling.setValue(terrainRolling);
            brownianModel.setValue(config.getBrownianModel()); brownianDensity.setValue(hits); brownianStrength.setValue(hitStrength);
            remainsHalfLife.setValue(Double.isInfinite(halfLife) ? 61 : Math.min(60, halfLife / 60));
            rejection.setValue(rejectionShare * 100); stickiness.setValue(-sticky / maxAttraction * 100);
            gravity.setValue(gravityValue / SimulationConfig.GRAVITY * 100); friction.setValue(frictionValue); rotationFriction.setValue(rotationValue);
            config.setSunRayRate(sunRate); config.setSunZoneWidth(width); config.setSeasonDuration(season);
            config.setWinterIntensity(winterShare); config.setRemainsForceHalfLife(halfLife);
            config.setAdhesionRejection(rejectionShare); config.setGroundStickiness(sticky);
            config.setGravity(gravityValue); config.setViscosity(frictionValue); config.setRotationalFriction(rotationValue);
            config.setBlockerFrictionCoefficient(terrainFriction); config.setBlockerRollingResistance(terrainRolling);
            config.setBrownianDensity(hits); config.setBrownianStrength(hitStrength);
            refreshBrownian.run();
            refresh.run(); refreshZoneWidth.run(); refreshSeason.run(); refreshAdhesion.run(); refreshStickiness.run(); refreshMechanics.run(); refreshBlockerFriction.run();
        };
        VBox content = new VBox(8, title, rate, value, hint, new Label("Sun energy form:"), sourceType, zones,
                new Label("Spectral zone width (px, 0 = one cycle per world):"), zoneWidth, zoneWidthValue,
                new Label("Year length (simulation minutes):"), seasonLength,
                new Label("Winter light (% of summer):"), winter, seasonValue,
                new Label("Gravity (% of standard):"), gravity, new Label("Friction (movement damping):"), friction,
                new Label("Rotation friction:"), rotationFriction, mechanicsValue,
                blockerFriction, new Label("Terrain friction coefficient:"), blockerFrictionCoefficient,
                new Label("Rolling resistance (contact patch / radius):"), blockerRolling, blockerFrictionValue,
                new Label("Brownian motion (all worlds except RACE):"), brownianModel,
                new Label("Brownian density (hits per second):"), brownianDensity,
                new Label("Brownian strength (px/s per hit):"), brownianStrength, brownianValue,
                adhesion, trainingAdhesion, bonds, birthBonds, new Label("Remains force half-life (minutes, right end = forever):"), remainsHalfLife,
                new Label("Rejection of foreign attraction (%):"), rejection, adhesionValue,
                groundHold, anchors, groundOnly, new Label("Terrain stickiness (% of max attraction):"), stickiness, stickinessValue);
        content.setPadding(new Insets(5));
        getChildren().add(new TitledPane("Environment", content));
    }

    /** Re-read the switches and sliders from the config, e.g. after a new scenario. */
    public void syncFromConfig() { syncFromConfig.run(); }
}
