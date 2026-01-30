package com.topzurdo.launcher.service;

import com.topzurdo.launcher.config.LauncherConfig;
import com.topzurdo.launcher.game.GameLauncher;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

/**
 * Service for managing game launching
 */
public class GameService {

    private static final Logger LOG = LoggerFactory.getLogger(GameService.class);

    private final LauncherConfig config;
    private final GameLauncher launcher;
    private Process gameProcess;

    public GameService(LauncherConfig config) {
        this.config = config;
        this.launcher = new GameLauncher(config);
    }

    public void launchGame(String username, Consumer<String> statusCallback, Runnable onComplete, Consumer<Exception> onError) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> statusCallback.accept("Запуск игры..."));

                gameProcess = launcher.launch(username);

                // Monitor game output
                new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(gameProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            LOG.debug("[Minecraft] {}", line);
                        }
                    } catch (Exception e) {
                        LOG.warn("Error reading game output", e);
                    }
                }, "GameOutputReader").start();

                // Wait for game to finish
                int exitCode = gameProcess.waitFor();
                LOG.info("Game exited with code: {}", exitCode);

                Platform.runLater(onComplete);

            } catch (Exception e) {
                LOG.error("Failed to launch game", e);
                Platform.runLater(() -> onError.accept(e));
            }
        }, "GameLauncherThread").start();
    }

    public boolean isGameRunning() {
        return gameProcess != null && gameProcess.isAlive();
    }

    public void stopGame() {
        if (gameProcess != null && gameProcess.isAlive()) {
            gameProcess.destroy();
            gameProcess = null;
        }
    }
}
