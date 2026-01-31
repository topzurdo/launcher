package com.topzurdo.launcher.service;

import com.topzurdo.launcher.config.LauncherConfig;
import com.topzurdo.launcher.download.MinecraftDownloader;
import com.topzurdo.launcher.game.GameLauncher;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Service for managing game launching
 */
public class GameService {

    private static final Logger LOG = LoggerFactory.getLogger(GameService.class);

    private final LauncherConfig config;
    private final GameLauncher launcher;
    private Process gameProcess;
    private final AtomicBoolean launchInProgress = new AtomicBoolean(false);
    private Consumer<String> statusCallback;
    private Runnable onGameStarted;
    private Runnable onGameExited;

    public GameService(LauncherConfig config) {
        this.config = config;
        this.launcher = new GameLauncher(config);
    }

    public void setStatusCallback(Consumer<String> c) { this.statusCallback = c; }
    public void setOnGameStarted(Runnable r) { this.onGameStarted = r; }
    public void setOnGameExited(Runnable r) { this.onGameExited = r; }

    public boolean isLaunchInProgress() { return launchInProgress.get(); }

    public CompletableFuture<Boolean> launchGame(LauncherConfig cfg) {
        if (!launchInProgress.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(false);
        }
        String username = cfg.getUsername() != null && !cfg.getUsername().isEmpty() ? cfg.getUsername() : "Player";
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        launchGame(username,
            s -> { if (statusCallback != null) statusCallback.accept(s); },
            () -> {
                launchInProgress.set(false);
                if (onGameExited != null) onGameExited.run();
                future.complete(true);
            },
            e -> {
                launchInProgress.set(false);
                future.completeExceptionally(e);
            });
        if (onGameStarted != null) onGameStarted.run();
        return future;
    }

    public void shutdown() {
        stopGame();
    }

    public void launchGame(String username, Consumer<String> statusCallback, Runnable onComplete, Consumer<Exception> onError) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> statusCallback.accept("Проверка нативных библиотек..."));
                MinecraftDownloader downloader = new MinecraftDownloader();
                downloader.ensureNatives(s -> Platform.runLater(() -> statusCallback.accept(s)));

                // DEV: перед запуском копировать свежий JAR мода из mod/build/libs в mods
                try {
                    downloader.installTopZurdoMod();
                } catch (Exception e) {
                    LOG.warn("Could not update mod from project (using existing mod if present): {}", e.getMessage());
                }

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
