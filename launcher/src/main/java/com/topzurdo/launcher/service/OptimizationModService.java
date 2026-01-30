package com.topzurdo.launcher.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.topzurdo.launcher.TopZurdoLauncher;

/**
 * Сервис установки модов оптимизации для Fabric 1.16.5.
 * Использует Modrinth API для скачивания лучших перформанс-модов.
 *
 * Включает:
 * - Sodium (FPS boost)
 * - Lithium (server/tick optimization)
 * - Starlight (lighting engine)
 * - LazyDFU (faster startup)
 * - FerriteCore (RAM reduction)
 * - Entity Culling (skip hidden entities)
 * - Dynamic FPS (reduce FPS when unfocused)
 * - Krypton (network optimization)
 * - Smooth Boot (load optimization)
 * - Cull Leaves (leaf optimization)
 * - Enhanced Block Entities (block entity optimization)
 * - ModernFix (memory & load time)
 */
public class OptimizationModService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationModService.class);
    private static final String MODRINTH_API = "https://api.modrinth.com/v2";
    private static final String MC_VERSION = "1.16.5";
    private static final String LOADER = "fabric";

    private final Path modsDir;
    private final ExecutorService executor;

    public OptimizationModService() {
        this.modsDir = TopZurdoLauncher.MODS_DIR;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "OptimizationMod-Installer");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Установить моды оптимизации асинхронно.
     * FerriteCore (память), RuOK (общая оптимизация), Francium (Random).
     */
    public CompletableFuture<InstallResult> installOptimizationMods(ProgressCallback callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(modsDir);
                List<String> installed = new ArrayList<>();
                List<String> failed = new ArrayList<>();

                // Modrinth project slugs for Fabric 1.16.5 performance mods
                // Order: essential first, then additional optimizations
                String[] mods = {
                    "sodium",           // Essential: Modern rendering engine, huge FPS boost
                    "lithium",          // Essential: General optimization (AI, tick, etc.)
                    "starlight",        // Essential: Optimized lighting engine
                    "lazydfu",          // Essential: Faster game startup
                    "ferrite-core",     // Essential: RAM usage reduction
                    "entityculling",    // Recommended: Don't render hidden entities
                    "dynamic-fps",      // Recommended: Lower FPS when unfocused
                    "krypton",          // Recommended: Network optimization
                    "smoothboot-fabric",// Recommended: Smoother loading
                    "cull-leaves",      // Optional: Leaf culling for FPS
                    "ebe",              // Optional: Enhanced Block Entities
                    "hydrogen",         // Optional: More memory optimization (experimental)
                };
                String[] names = {
                    "Sodium",
                    "Lithium",
                    "Starlight",
                    "LazyDFU",
                    "FerriteCore",
                    "Entity Culling",
                    "Dynamic FPS",
                    "Krypton",
                    "Smooth Boot",
                    "Cull Leaves",
                    "Enhanced Block Entities",
                    "Hydrogen",
                };

                for (int i = 0; i < mods.length; i++) {
                    if (callback != null) {
                        callback.onProgress(names[i], i + 1, mods.length);
                    }
                    try {
                        // Check if already installed
                        if (isModInstalled(mods[i], names[i])) {
                            LOGGER.info("{} already installed, skipping", names[i]);
                            installed.add(names[i] + " (уже установлен)");
                            continue;
                        }

                        String url = fetchDownloadUrl(mods[i]);
                        if (url != null) {
                            String fileName = url.substring(url.lastIndexOf('/') + 1);
                            Path dest = modsDir.resolve(fileName);

                            // Skip if file exists
                            if (Files.exists(dest)) {
                                LOGGER.info("{} file exists, skipping", names[i]);
                                installed.add(names[i] + " (уже есть)");
                                continue;
                            }

                            downloadFile(url, dest);
                            installed.add(names[i]);
                            LOGGER.info("Installed {} to {}", names[i], dest);
                        } else {
                            // Not found for this MC version - skip silently for optional mods
                            LOGGER.debug("{} not found for {}", names[i], MC_VERSION);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to install {}: {}", names[i], e.getMessage());
                        // Don't add to failed for optional mods that might not exist
                        if (i < 5) { // Only first 5 are essential
                            failed.add(names[i] + ": " + e.getMessage());
                        }
                    }
                }

                return new InstallResult(installed, failed);
            } catch (Exception e) {
                LOGGER.error("Optimization mod install failed", e);
                return new InstallResult(List.of(), List.of("Ошибка: " + e.getMessage()));
            }
        }, executor);
    }

    private String fetchDownloadUrl(String projectSlug) throws IOException {
        String apiUrl = MODRINTH_API + "/project/" + projectSlug + "/version?game_versions=%5B%22"
            + MC_VERSION + "%22%5D&loaders=%5B%22" + LOADER + "%22%5D";
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Zurdo-Launcher/1.0");

        try (InputStream is = conn.getInputStream()) {
            String json = new String(is.readAllBytes());
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            if (arr.isEmpty()) return null;
            JsonObject ver = arr.get(0).getAsJsonObject();
            JsonArray files = ver.getAsJsonArray("files");
            if (files == null || files.isEmpty()) return null;
            for (JsonElement f : files) {
                JsonObject file = f.getAsJsonObject();
                if (file.has("primary") && file.get("primary").getAsBoolean()) {
                    return file.get("url").getAsString();
                }
            }
            return files.get(0).getAsJsonObject().get("url").getAsString();
        } finally {
            conn.disconnect();
        }
    }

    private void downloadFile(String url, Path dest) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "TopZurdo-Launcher/1.0");
        conn.setInstanceFollowRedirects(true);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP " + responseCode + " for " + url);
        }

        try (InputStream is = conn.getInputStream()) {
            Files.copy(is, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Check if a mod is already installed by looking for jar files with matching name pattern.
     */
    private boolean isModInstalled(String projectSlug, String modName) {
        if (!Files.exists(modsDir)) return false;
        try {
            String searchName = modName.toLowerCase().replace(" ", "");
            String searchSlug = projectSlug.toLowerCase().replace("-", "");

            return Files.list(modsDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .anyMatch(p -> {
                    String fileName = p.getFileName().toString().toLowerCase().replace("-", "").replace("_", "");
                    return fileName.contains(searchName) || fileName.contains(searchSlug);
                });
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get list of currently installed optimization mods.
     */
    public List<String> getInstalledOptMods() {
        List<String> result = new ArrayList<>();
        String[] mods = { "sodium", "lithium", "starlight", "lazydfu", "ferrite", "entityculling",
                         "dynamic-fps", "krypton", "smoothboot", "cull-leaves", "ebe", "hydrogen" };
        String[] names = { "Sodium", "Lithium", "Starlight", "LazyDFU", "FerriteCore",
                          "Entity Culling", "Dynamic FPS", "Krypton", "Smooth Boot",
                          "Cull Leaves", "Enhanced Block Entities", "Hydrogen" };

        for (int i = 0; i < mods.length; i++) {
            if (isModInstalled(mods[i], names[i])) {
                result.add(names[i]);
            }
        }
        return result;
    }

    public void shutdown() {
        executor.shutdown();
    }

    public interface ProgressCallback {
        void onProgress(String modName, int current, int total);
    }

    public static class InstallResult {
        public final List<String> installed;
        public final List<String> failed;

        public InstallResult(List<String> installed, List<String> failed) {
            this.installed = installed;
            this.failed = failed;
        }

        public boolean isSuccess() {
            return failed.isEmpty();
        }

        public boolean hasInstalled() {
            return !installed.isEmpty();
        }
    }
}
