package com.topzurdo.launcher.service;

import com.topzurdo.launcher.download.MinecraftDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Service for managing Minecraft download and installation.
 *
 * <p>Downloads and installs:</p>
 * <ul>
 *   <li>Minecraft client (vanilla)</li>
 *   <li>Forge mod loader</li>
 *   <li>TopZurdo mod</li>
 *   <li>Required libraries and assets</li>
 * </ul>
 *
 * <p>Progress is reported via callbacks (0.0 to 1.0).</p>
 * <p>Status messages are "elegantified" for premium UI experience.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * DownloadService download = new DownloadService();
 * download.setProgressCallback(progress -> updateProgressBar(progress));
 * download.setStatusCallback(status -> updateStatusLabel(status));
 *
 * if (!download.isFullyInstalled()) {
 *     download.startDownload()
 *         .thenAccept(success -> {
 *             if (success) {
 *                 launchGame();
 *             }
 *         });
 * }
 * }</pre>
 *
 * @author TopZurdo Team
 * @version 1.0.0
 * @see MinecraftDownloader
 */
public class DownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadService.class);

    private final MinecraftDownloader downloader;
    private final OptimizationModService optimizationService;
    private final ExecutorService executor;
    private final AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private Consumer<Double> progressCallback;
    private Consumer<String> statusCallback;

    public DownloadService() {
        this.downloader = new MinecraftDownloader();
        this.optimizationService = new OptimizationModService();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DownloadService-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Set callback for progress updates (0.0 to 1.0)
     */
    public void setProgressCallback(Consumer<Double> callback) {
        this.progressCallback = callback;
    }

    /**
     * Set callback for status updates
     */
    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    /**
     * Check if Minecraft is fully installed
     */
    public boolean isFullyInstalled() {
        return downloader.isMinecraftInstalled()
            && downloader.isFabricInstalled()
            && downloader.isModInstalled();
    }

    /**
     * Check if Minecraft base is installed
     */
    public boolean isMinecraftInstalled() {
        return downloader.isMinecraftInstalled();
    }

    /**
     * Check if Fabric is installed
     */
    public boolean isFabricInstalled() {
        return downloader.isFabricInstalled();
    }

    /**
     * Check if mod is installed
     */
    public boolean isModInstalled() {
        return downloader.isModInstalled();
    }

    /**
     * Check if download is in progress
     */
    public boolean isDownloading() {
        return downloadInProgress.get();
    }

    /**
     * Start the download process
     * Downloads Minecraft, Forge, and mod
     * @return CompletableFuture that completes when download finishes
     */
    public CompletableFuture<Boolean> startDownload() {
        if (!downloadInProgress.compareAndSet(false, true)) {
            LOGGER.warn("Download already in progress");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Starting Minecraft download");
        updateStatus("Подготовка к загрузке...");
        updateProgress(0.0);

        return CompletableFuture.supplyAsync(() -> {
            try {
                    final double[] currentProgress = {0.0};
                    downloader.downloadMinecraft(
                progress -> {
                    // Reserve 90% for MC download, 10% for optimization mods
                    currentProgress[0] = progress * 0.9;
                    updateProgress(currentProgress[0]);
                },
                status -> {
                    // Реалтайм детальный прогресс с процентом
                    String detailedStatus = getDetailedStatus(status, currentProgress[0]);
                    updateStatus(detailedStatus);
                }
            );

                LOGGER.info("Minecraft download completed, installing optimization mods...");
                updateStatus("Установка модов оптимизации...");
                updateProgress(0.92);

                // Auto-install optimization mods (Sodium, Lithium, etc.)
                try {
                    optimizationService.installOptimizationMods((modName, current, total) -> {
                        updateStatus("Установка " + modName + "...");
                        updateProgress(0.9 + (0.1 * current / total));
                    }).get(); // Wait for completion
                    LOGGER.info("Optimization mods installed successfully");
                } catch (Exception e) {
                    LOGGER.warn("Some optimization mods failed to install: {}", e.getMessage());
                    // Continue anyway - optimization mods are optional
                }

                LOGGER.info("Download completed successfully");
                updateStatus("Загрузка завершена");
                updateProgress(1.0);

                return true;

            } catch (Exception e) {
                LOGGER.error("Download failed", e);
                updateStatus("Ошибка загрузки: " + e.getMessage());
                throw new RuntimeException(e);

            } finally {
                downloadInProgress.set(false);
            }
        }, executor);
    }

    /**
     * Cancel current download (if supported)
     */
    public void cancelDownload() {
        // TODO: Implement download cancellation in MinecraftDownloader
        LOGGER.info("Download cancellation requested");
        downloadInProgress.set(false);
    }

    /**
     * Convert status message to elegant language with detailed progress
     */
    private String getDetailedStatus(String status, double progress) {
        if (status == null) return String.format("Подготовка... (%.0f%%)", progress * 100);

        String lower = status.toLowerCase();
        int percent = (int)(progress * 100);

        // Детальные сообщения для каждого этапа
        if (lower.contains("информации о версии") || lower.contains("version")) {
            return String.format("Получение информации о версии Minecraft... (%d%%)", percent);
        }
        if (lower.contains("данных версии") || lower.contains("version json")) {
            return String.format("Загрузка данных версии 1.16.5... (%d%%)", percent);
        }
        if (lower.contains("клиента") || lower.contains("client")) {
            return String.format("Загрузка клиента Minecraft... (%d%%)", percent);
        }
        if (lower.contains("библиотек") || lower.contains("librar")) {
            return String.format("Загрузка библиотек и зависимостей... (%d%%)", percent);
        }
        if (lower.contains("ресурсов") || lower.contains("asset")) {
            return String.format("Загрузка ресурсов игры (текстуры, звуки)... (%d%%)", percent);
        }
        if (lower.contains("fabric") || lower.contains("профиля")) {
            return String.format("Установка Fabric Loader... (%d%%)", percent);
        }
        if (lower.contains("topzurdo") || lower.contains("мода")) {
            return String.format("Установка TopZurdo мода... (%d%%)", percent);
        }
        if (lower.contains("завершена") || lower.contains("complet")) {
            return String.format("Установка завершена! (%d%%)", percent);
        }

        return String.format("%s (%d%%)", status, percent);
    }

    /**
     * Shutdown the service
     */
    public void shutdown() {
        LOGGER.info("Shutting down DownloadService");
        executor.shutdown();
        optimizationService.shutdown();
    }

    /**
     * Get installed optimization mods
     */
    public java.util.List<String> getInstalledOptimizationMods() {
        return optimizationService.getInstalledOptMods();
    }

    /**
     * Manually install optimization mods
     */
    public CompletableFuture<OptimizationModService.InstallResult> installOptimizationMods() {
        updateStatus("Установка модов оптимизации...");
        return optimizationService.installOptimizationMods((modName, current, total) -> {
            updateStatus("Установка " + modName + "...");
            updateProgress((double) current / total);
        });
    }

    private void updateProgress(double progress) {
        if (progressCallback != null) {
            progressCallback.accept(progress);
        }
    }

    private void updateStatus(String status) {
        if (statusCallback != null) {
            statusCallback.accept(status);
        }
    }
}
