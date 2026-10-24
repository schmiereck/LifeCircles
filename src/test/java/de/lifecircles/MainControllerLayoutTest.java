package de.lifecircles;

import de.lifecircles.service.SimulationConfig;
import de.lifecircles.service.SimulationSettingsFile;
import de.lifecircles.view.EnvironmentPanel;
import de.lifecircles.service.BrainEnergyCosts;
import de.lifecircles.model.EnergyType;
import de.lifecircles.service.trainStrategy.TrainMode;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ScrollPane;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainControllerLayoutTest {
    @Test void bottomControlsLeaveWorldFullWidthAndSunSliderUpdatesLiveConfig() throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.startup(() -> {
            SimulationConfig config=SimulationConfig.getInstance(); double sun=config.getSunRayRate();
            double brainMultiplier=config.getBrainEnergyCosts().work();
            double lifespan=config.getCellDeathAge();
            EnergyType sunEnergyType=config.getSunEnergyType();
            MainController controller=null;
            try {
                controller=new MainController();
                new Scene(controller,1200,800);
                controller.resize(1200,800); controller.applyCss(); controller.layout();
                assertNotNull(controller.lookup("#new-simulation-button"));
                assertEquals("New…",((Button)controller.lookup("#new-simulation-button")).getText());
                var dialog=controller.createScenarioDialog();
                @SuppressWarnings("unchecked") ComboBox<TrainMode> modes=(ComboBox<TrainMode>)dialog.getDialogPane().lookup("#new-scenario-choice");
                assertTrue(modes.getItems().contains(TrainMode.CARRION_RAIN));
                assertTrue(modes.getItems().contains(TrainMode.CARRION_CONVERSION));
                assertTrue(modes.getItems().contains(TrainMode.WORLD2));
                assertFalse(modes.getItems().contains(TrainMode.EVOLUTIONARY));
                TrainMode previousMode=config.getTrainMode();
                modes.setValue(TrainMode.CARRION_RAIN);
                assertNull(dialog.getResultConverter().call(javafx.scene.control.ButtonType.CANCEL));
                assertEquals(previousMode,config.getTrainMode());
                assertEquals(TrainMode.CARRION_RAIN,dialog.getResultConverter().call(dialog.getDialogPane().getButtonTypes().getFirst()));
                assertNull(controller.getRight());
                assertEquals(1200,controller.getCenter().getBoundsInParent().getWidth(),1);
                assertTrue(controller.getCenter().getBoundsInParent().getHeight()>450);
                EnvironmentPanel panel=(EnvironmentPanel)controller.lookupAll(".titled-pane").stream()
                        .filter(n->n instanceof TitledPane t && t.getText().equals("Environment"))
                        .findFirst().orElseThrow().getParent();
                Slider slider=(Slider)panel.lookup(".slider");
                slider.setValue(0); assertEquals(0,config.getSunRayRate());
                slider.setValue(37.5); assertEquals(37.5,config.getSunRayRate());
                @SuppressWarnings("unchecked") ChoiceBox<EnergyType> sourceType=(ChoiceBox<EnergyType>)controller.lookup("#sun-energy-type");
                assertEquals(sunEnergyType,sourceType.getValue());
                sourceType.setValue(EnergyType.SECONDARY);assertEquals(EnergyType.SECONDARY,config.getSunEnergyType());
                sourceType.setValue(EnergyType.PRIMARY);assertEquals(EnergyType.PRIMARY,config.getSunEnergyType());
                CheckBox adhesion=(CheckBox)controller.lookup("#cell-adhesion");
                adhesion.setSelected(false);assertFalse(config.isCellAdhesion());adhesion.setSelected(true);assertTrue(config.isCellAdhesion());
                Slider zoneWidth=(Slider)controller.lookup("#sun-zone-width");
                double savedZoneWidth=config.getSunZoneWidth();
                zoneWidth.setValue(1630);assertEquals(1600,config.getSunZoneWidth());zoneWidth.setValue(0);assertEquals(0,config.getSunZoneWidth());
                config.setSunZoneWidth(800);panel.syncFromConfig();assertEquals(800,zoneWidth.getValue());
                // Loaded settings outside the slider grid survive the panel sync exactly.
                double savedHalfLife=config.getRemainsForceHalfLife(),savedRejection=config.getAdhesionRejection();
                config.setSunZoneWidth(5000);config.setRemainsForceHalfLife(450.5);config.setAdhesionRejection(.333);panel.syncFromConfig();
                assertEquals(5000,config.getSunZoneWidth());assertEquals(450.5,config.getRemainsForceHalfLife());assertEquals(.333,config.getAdhesionRejection());
                assertEquals(.333*100,((Slider)controller.lookup("#adhesion-rejection")).getValue(),1e-9);
                config.setRemainsForceHalfLife(savedHalfLife);config.setAdhesionRejection(savedRejection);
                config.setSunZoneWidth(savedZoneWidth);panel.syncFromConfig();
                CheckBox training=(CheckBox)controller.lookup("#training-adhesion");
                assertFalse(training.isSelected());
                training.setSelected(true);assertTrue(config.isTrainingAdhesion());training.setSelected(false);assertFalse(config.isTrainingAdhesion());
                CheckBox bonds=(CheckBox)controller.lookup("#adhesion-bonds"),birthBonds=(CheckBox)controller.lookup("#adhesion-birth-bonds");
                assertFalse(bonds.isSelected());assertFalse(birthBonds.isSelected());
                bonds.setSelected(true);assertTrue(config.isAdhesionBonds());bonds.setSelected(false);assertFalse(config.isAdhesionBonds());
                birthBonds.setSelected(true);assertTrue(config.isAdhesionBirthBonds());birthBonds.setSelected(false);assertFalse(config.isAdhesionBirthBonds());
                CheckBox groundHold=(CheckBox)controller.lookup("#ground-adhesion"),anchors=(CheckBox)controller.lookup("#ground-anchors"),
                        groundOnly=(CheckBox)controller.lookup("#ground-anchors-ground-only");
                assertFalse(groundHold.isSelected());assertFalse(anchors.isSelected());assertFalse(groundOnly.isSelected());
                groundHold.setSelected(true);assertTrue(config.isGroundAdhesion());groundHold.setSelected(false);assertFalse(config.isGroundAdhesion());
                anchors.setSelected(true);assertTrue(config.isGroundAnchors());anchors.setSelected(false);assertFalse(config.isGroundAnchors());
                groundOnly.setSelected(true);assertTrue(config.isGroundAnchorsGroundOnly());groundOnly.setSelected(false);assertFalse(config.isGroundAnchorsGroundOnly());
                Slider stickiness=(Slider)controller.lookup("#ground-stickiness");
                stickiness.setValue(50);assertEquals(-.5*config.getCellActorMaxAttractiveForceStrength(),config.getGroundStickiness(),1e-9);
                stickiness.setValue(0);assertEquals(0,config.getGroundStickiness(),1e-12);
                Slider halfLife=(Slider)controller.lookup("#remains-force-half-life");
                halfLife.setValue(10);assertEquals(600,config.getRemainsForceHalfLife());
                halfLife.setValue(61);assertEquals(Double.POSITIVE_INFINITY,config.getRemainsForceHalfLife());
                halfLife.setValue(5);assertEquals(300,config.getRemainsForceHalfLife());
                Slider rejection=(Slider)controller.lookup("#adhesion-rejection");
                rejection.setValue(100);assertEquals(1,config.getAdhesionRejection());rejection.setValue(50);assertEquals(.5,config.getAdhesionRejection());
                Slider brainSlider=(Slider)controller.lookup("#brain-energy-slider");
                assertEquals(2,brainSlider.getValue());
                brainSlider.setValue(4);assertEquals(new BrainEnergyCosts(100,100),config.getBrainEnergyCosts());
                brainSlider.setValue(0);assertEquals(new BrainEnergyCosts(0,0),config.getBrainEnergyCosts());
                Slider lifespanSlider=(Slider)controller.lookup("#cell-lifespan-slider");
                ScrollPane controls=(ScrollPane)((javafx.scene.layout.VBox)controller.getBottom()).getChildren().getFirst();
                var viewport=controls.lookup(".viewport");
                assertTrue(lifespanSlider.localToScene(lifespanSlider.getBoundsInLocal()).getMaxY()
                        <=viewport.localToScene(viewport.getBoundsInLocal()).getMaxY(),"Lifespan slider should fit the bottom controls");
                assertEquals(Math.log10(lifespan),lifespanSlider.getValue());
                lifespanSlider.setValue(2);assertEquals(100,config.getCellDeathAge());
                lifespanSlider.setValue(3);assertEquals(1000,config.getCellDeathAge());
                loadAppliesSavedSettingsToThePanel(controller,config);
                result.complete(null);
            } catch(Throwable failure) {result.completeExceptionally(failure);}
            finally {config.setSunRayRate(sun);config.setSunEnergyType(sunEnergyType);config.setBrainEnergyMultiplier(brainMultiplier);config.setCellDeathAge(lifespan); if(controller!=null) controller.shutdown();}
        });
        try {result.get(45,TimeUnit.SECONDS);} finally {Platform.exit();}
    }

    private static EnvironmentPanel environmentPanel(MainController controller) {
        return (EnvironmentPanel)controller.lookupAll(".titled-pane").stream()
                .filter(n->n instanceof TitledPane t && t.getText().equals("Environment")).findFirst().orElseThrow().getParent();
    }

    /** The Load button path: saved sun rate and adhesion switches reach config and controls. */
    private static void loadAppliesSavedSettingsToThePanel(MainController controller,SimulationConfig config) throws Exception {
        java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("load-settings");
        String cells=dir.resolve("saved.cells").toString(),stash=dir.resolve("stash.cells").toString();
        SimulationSettingsFile.save(stash,config,0);
        try {
            de.lifecircles.service.FileService.getInstance().saveCellsToFile(cells,java.util.List.of());
            config.setSunRayRate(16.5);config.setCellAdhesion(true);config.setAdhesionBonds(true);config.setAdhesionBirthBonds(true);
            config.setGroundAdhesion(true);config.setGroundAnchors(true);config.setTrainingAdhesion(true);
            SimulationSettingsFile.save(cells,config,199_080);
            config.setSunRayRate(60);config.setCellAdhesion(false);config.setAdhesionBonds(false);config.setAdhesionBirthBonds(false);
            config.setGroundAdhesion(false);config.setGroundAnchors(false);config.setTrainingAdhesion(false);
            environmentPanel(controller).syncFromConfig();
            controller.loadCells(cells);
            assertEquals(16.5,config.getSunRayRate());assertTrue(config.isCellAdhesion());assertTrue(config.isAdhesionBonds());
            Slider sun=(Slider)environmentPanel(controller).lookup(".slider");
            assertEquals(16.5,sun.getValue(),1e-9);
            for(String id:new String[]{"#cell-adhesion","#adhesion-bonds","#adhesion-birth-bonds","#ground-adhesion","#ground-anchors","#training-adhesion"})
                assertTrue(((CheckBox)controller.lookup(id)).isSelected(),id);
            // Load & Merge keeps the environment unless asked to take the settings; never the step count.
            config.setSunRayRate(60);config.setCellAdhesion(false);environmentPanel(controller).syncFromConfig();
            controller.mergeCells(cells,false);
            assertEquals(60,config.getSunRayRate());assertFalse(((CheckBox)controller.lookup("#cell-adhesion")).isSelected());
            controller.mergeCells(cells,true);
            assertEquals(16.5,config.getSunRayRate());assertTrue(((CheckBox)controller.lookup("#cell-adhesion")).isSelected());
            assertNotNull(controller.lookup("#merge-loads-settings"));
        } finally {
            SimulationSettingsFile.load(stash,config);
            environmentPanel(controller).syncFromConfig();
        }
    }
}
