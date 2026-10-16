package de.lifecircles;

import de.lifecircles.service.SimulationConfig;
import de.lifecircles.view.EnvironmentPanel;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainControllerLayoutTest {
    @Test void bottomControlsLeaveWorldFullWidthAndSunSliderUpdatesLiveConfig() throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.startup(() -> {
            SimulationConfig config=SimulationConfig.getInstance(); double sun=config.getSunRayRate();
            MainController controller=null;
            try {
                controller=new MainController();
                new Scene(controller,1200,800);
                controller.resize(1200,800); controller.applyCss(); controller.layout();
                assertNull(controller.getRight());
                assertEquals(1200,controller.getCenter().getBoundsInParent().getWidth(),1);
                assertTrue(controller.getCenter().getBoundsInParent().getHeight()>450);
                EnvironmentPanel panel=(EnvironmentPanel)controller.lookupAll(".titled-pane").stream()
                        .filter(n->n instanceof TitledPane t && t.getText().equals("Environment"))
                        .findFirst().orElseThrow().getParent();
                Slider slider=(Slider)panel.lookup(".slider");
                slider.setValue(0); assertEquals(0,config.getSunRayRate());
                slider.setValue(37.5); assertEquals(37.5,config.getSunRayRate());
                result.complete(null);
            } catch(Throwable failure) {result.completeExceptionally(failure);}
            finally {config.setSunRayRate(sun); if(controller!=null) controller.shutdown();}
        });
        try {result.get(45,TimeUnit.SECONDS);} finally {Platform.exit();}
    }
}
