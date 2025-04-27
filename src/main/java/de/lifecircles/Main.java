package de.lifecircles;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Map;
import de.lifecircles.service.TrainMode;
import de.lifecircles.service.SimulationConfig;

/**
 * Main application class for LifeCircles.
 * Launches the JavaFX application and sets up the main window.
 */
public class Main extends Application {
    private MainController mainController;

    @Override
    public void init() throws Exception {
        Map<String, String> named = getParameters().getNamed();
        String modeStr = named.get("trainMode");
        if (modeStr != null) {
            try {
                TrainMode mode = TrainMode.valueOf(modeStr.toUpperCase());
                SimulationConfig.getInstance().setTrainMode(mode);
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown trainMode: " + modeStr);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        mainController = new MainController();

        Scene scene = new Scene(mainController, 1200, 800);
        primaryStage.setTitle("LifeCircles Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Add window close handler
        primaryStage.setOnCloseRequest(e -> {
            mainController.shutdown();
        });
    }

    @Override
    public void stop() {
        if (mainController != null) {
            mainController.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}