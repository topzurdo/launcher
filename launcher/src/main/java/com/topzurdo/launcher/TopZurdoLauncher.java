package com.topzurdo.launcher;

import java.nio.file.Path;

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

    private static TopZurdoLauncher instance;

    /** Config directory (e.g. .topzurdo in user home). */
    public static final Path CONFIG_DIR = Path.of(System.getProperty("user.home", "."), ".topzurdo");
    /** Minecraft game directory (e.g. .topzurdo/minecraft). */
    public static final Path MINECRAFT_DIR = CONFIG_DIR.resolve("minecraft");
    /** Mods directory inside game dir. */
    public static final Path MODS_DIR = MINECRAFT_DIR.resolve("mods");

    private MainController controller;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        instance = this;
        this.primaryStage = stage;
        LOG.info("Starting {} v{}", APP_NAME, VERSION);

        try {
            controller = new MainController(stage);
            Scene scene = new Scene(controller.getRoot(), 1000, 650);
            // Premium dark + gold: load tokens first, then main styles
            try {
                var tokens = getClass().getResource("/config/theme/tokens-dark.css");
                if (tokens != null) scene.getStylesheets().add(tokens.toExternalForm());
            } catch (Exception e) { LOG.warn("Theme tokens not loaded: {}", e.getMessage()); }
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

    public static TopZurdoLauncher getInstance() { return instance; }

    public Stage getPrimaryStage() { return primaryStage; }

    public void closeWindow() {
        if (primaryStage != null) primaryStage.close();
    }

    public void minimizeWindow() {
        if (primaryStage != null) primaryStage.setIconified(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
