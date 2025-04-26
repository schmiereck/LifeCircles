package de.lifecircles;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class for LifeCircles.
 * Launches the JavaFX application and sets up the main window.
 */
public class Main extends Application {
    private MainController mainController;

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