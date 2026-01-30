package com.topzurdo.launcher;

import com.topzurdo.launcher.config.LauncherConfig;
import com.topzurdo.launcher.ui.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TopZurdo Launcher - Main Application
 */
public class TopZurdoLauncher extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(TopZurdoLauncher.class);

    public static final String APP_NAME = "TopZurdo Launcher";
    public static final String VERSION = "1.0.0";

    private MainController controller;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        LOG.info("Starting {} v{}", APP_NAME, VERSION);

        try {
            controller = new MainController(stage);
            Scene scene = new Scene(controller.getRoot(), 1000, 650);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle(APP_NAME);
            stage.setMinWidth(800);
            stage.setMinHeight(550);
            stage.initStyle(StageStyle.UNDECORATED);

            // Load icon
            try {
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
            } catch (Exception e) {
                LOG.warn("Could not load icon: {}", e.getMessage());
            }

            stage.show();
            LOG.info("Launcher started successfully");

        } catch (Exception e) {
            LOG.error("Failed to start launcher", e);
            throw new RuntimeException("Failed to start launcher", e);
        }
    }

    @Override
    public void stop() {
        LOG.info("Shutting down launcher");
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
