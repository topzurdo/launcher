package com.topzurdo.launcher.service;

import com.topzurdo.launcher.config.LauncherConfig;
import com.topzurdo.launcher.ui.effects.EffectsEngine;
import javafx.application.Platform;

import java.util.function.Consumer;

/**
 * Aggregates launcher services and notifies when each is ready.
 */
public class LauncherServices {

    private final LauncherConfig config;
    private Consumer<AuthService> onAuthReady;
    private Consumer<DownloadService> onDownloadReady;
    private Consumer<GameService> onGameReady;
    private Consumer<ServerStatusService> onServerStatusReady;
    private Consumer<EffectsEngine> onEffectsReady;

    public LauncherServices(LauncherConfig config) {
        this.config = config;
    }

    public static LauncherServices startAsync() {
        LauncherConfig config = LauncherConfig.load();
        LauncherServices services = new LauncherServices(config);
        new Thread(() -> {
            AuthService auth = new AuthService();
            Platform.runLater(() -> {
                if (services.onAuthReady != null) services.onAuthReady.accept(auth);
            });
            DownloadService download = new DownloadService();
            Platform.runLater(() -> {
                if (services.onDownloadReady != null) services.onDownloadReady.accept(download);
            });
            GameService game = new GameService(config);
            Platform.runLater(() -> {
                if (services.onGameReady != null) services.onGameReady.accept(game);
            });
            ServerStatusService serverStatus = new ServerStatusService();
            Platform.runLater(() -> {
                if (services.onServerStatusReady != null) services.onServerStatusReady.accept(serverStatus);
            });
            EffectsEngine effects = new EffectsEngine();
            Platform.runLater(() -> {
                if (services.onEffectsReady != null) services.onEffectsReady.accept(effects);
            });
        }, "LauncherServices-Init").start();
        return services;
    }

    public void onAuthReady(Consumer<AuthService> c) { this.onAuthReady = c; }
    public void onDownloadReady(Consumer<DownloadService> c) { this.onDownloadReady = c; }
    public void onGameReady(Consumer<GameService> c) { this.onGameReady = c; }
    public void onServerStatusReady(Consumer<ServerStatusService> c) { this.onServerStatusReady = c; }
    public void onEffectsReady(Consumer<EffectsEngine> c) { this.onEffectsReady = c; }
}
