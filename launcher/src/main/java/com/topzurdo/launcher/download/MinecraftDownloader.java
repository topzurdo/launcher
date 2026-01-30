package com.topzurdo.launcher.download;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.topzurdo.launcher.TopZurdoLauncher;

/**
 * Minecraft and Fabric downloader
 * Handles downloading all game files, libraries, and assets
 */
public class MinecraftDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftDownloader.class);
    private static final Gson GSON = new Gson();

    // Minecraft version manifest URL
    private static final String VERSION_MANIFEST_URL =
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    // Target versions
    private static final String MC_VERSION = "1.16.5";
    private static final String FABRIC_VERSION = "0.12.12";
    private static final String FABRIC_VERSION_ID = "1.16.5-fabric-0.12.12";
    private static final String FABRIC_JSON_URL =
        "https://meta.fabricmc.net/v2/versions/loader/%s/%s/profile/json";
    private static final String MOD_JAR_NAME = "topzurdo-mod-1.0.0.jar";

    // DEV MODE: Мод ищется только локально, без скачивания с GitHub
    private static final boolean DEV_MODE = true;

    private final Path minecraftDir;
    private final Path versionsDir;
    private final Path librariesDir;
    private final Path assetsDir;

    public MinecraftDownloader() {
        this.minecraftDir = TopZurdoLauncher.MINECRAFT_DIR;
        this.versionsDir = minecraftDir.resolve("versions");
        this.librariesDir = minecraftDir.resolve("libraries");
        this.assetsDir = minecraftDir.resolve("assets");
    }

    /**
     * Check if Minecraft is already installed
     */
    public boolean isMinecraftInstalled() {
        Path versionDir = versionsDir.resolve(MC_VERSION);
        Path jarFile = versionDir.resolve(MC_VERSION + ".jar");
        Path jsonFile = versionDir.resolve(MC_VERSION + ".json");

        return Files.exists(jarFile) && Files.exists(jsonFile);
    }

    /**
     * Check if Fabric is installed
     */
    public boolean isFabricInstalled() {
        Path fabricVersionDir = versionsDir.resolve(FABRIC_VERSION_ID);
        Path jsonFile = fabricVersionDir.resolve(FABRIC_VERSION_ID + ".json");
        if (!Files.exists(jsonFile)) {
            return false;
        }
        try {
            JsonObject json = GSON.fromJson(Files.readString(jsonFile), JsonObject.class);
            if (json == null) {
                return false;
            }
            if (!json.has("mainClass")) {
                return false;
            }
            String mainClass = json.get("mainClass").getAsString();
            return mainClass != null && !mainClass.isEmpty();
        } catch (Exception e) {
            LOGGER.error("Error checking Fabric installation: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean isModInstalled() {
        Path modsDir = TopZurdoLauncher.MODS_DIR;
        Path modJar = modsDir.resolve(MOD_JAR_NAME);
        return Files.exists(modJar);
    }

    /**
     * Ensure all required Fabric files exist
     */
    public void ensureFabricFiles(Consumer<String> statusCallback) {
        if (!isFabricInstalled()) {
            return; // Fabric not installed, will be installed during downloadMinecraft
        }

        try {
            LOGGER.info("Fabric files verified");
        } catch (Exception e) {
            LOGGER.error("Error ensuring Fabric files: {}", e.getMessage(), e);
        }
    }

    /**
     * Download Minecraft with progress callback
     */
    public void downloadMinecraft(Consumer<Double> progressCallback, Consumer<String> statusCallback)
            throws Exception {

        LOGGER.info("Starting Minecraft download for version {}", MC_VERSION);

        try {
            // Step 1: Download version manifest
            statusCallback.accept("Получение информации о версии Minecraft...");
            progressCallback.accept(0.05);

            JsonObject versionManifest = downloadJson(VERSION_MANIFEST_URL);
            JsonArray versions = versionManifest.getAsJsonArray("versions");

            String versionUrl = null;
            for (JsonElement elem : versions) {
                JsonObject version = elem.getAsJsonObject();
                if (MC_VERSION.equals(version.get("id").getAsString())) {
                    versionUrl = version.get("url").getAsString();
                    break;
                }
            }

            if (versionUrl == null) {
                throw new RuntimeException("Version " + MC_VERSION + " not found in manifest");
            }

            // Step 2: Download version JSON
            statusCallback.accept("Загрузка данных версии...");
            progressCallback.accept(0.10);

            JsonObject versionJson = downloadJson(versionUrl);

            // Save version JSON
            Path versionDir = versionsDir.resolve(MC_VERSION);
            Files.createDirectories(versionDir);
            Path jsonPath = versionDir.resolve(MC_VERSION + ".json");
            Files.writeString(jsonPath, GSON.toJson(versionJson));

            // Step 3: Download client JAR
            statusCallback.accept("Загрузка клиента Minecraft (JAR файл)...");
            progressCallback.accept(0.15);

            JsonObject downloads = versionJson.getAsJsonObject("downloads");
            JsonObject client = downloads.getAsJsonObject("client");
            String clientUrl = client.get("url").getAsString();
            long clientSize = client.get("size").getAsLong();

            Path jarPath = versionDir.resolve(MC_VERSION + ".jar");
            downloadFile(clientUrl, jarPath, clientSize, progress -> {
                progressCallback.accept(0.15 + progress * 0.25);
            });

            // Step 4: Download libraries
            statusCallback.accept("Загрузка библиотек...");
            JsonArray libraries = versionJson.getAsJsonArray("libraries");
            downloadLibraries(libraries, progress -> {
                progressCallback.accept(0.40 + progress * 0.30);
            }, statusCallback);

            // Step 5: Download assets
            JsonObject assetIndex = versionJson.getAsJsonObject("assetIndex");
            downloadAssets(assetIndex, progress -> {
                progressCallback.accept(0.70 + progress * 0.20);
            }, statusCallback);

            // Step 6: Install Fabric
            statusCallback.accept("Установка Fabric...");
            progressCallback.accept(0.90);
            downloadFabric(statusCallback);

            // Step 7: Download TopZurdo mod
            statusCallback.accept("Установка TopZurdo мода...");
            progressCallback.accept(0.95);
            installTopZurdoMod();

            statusCallback.accept("Установка завершена!");
            progressCallback.accept(1.0);

            LOGGER.info("Minecraft download completed successfully");

        } catch (Exception e) {
            LOGGER.error("Failed to download Minecraft", e);
            throw e;
        }
    }

    /**
     * Download libraries
     */
    private void downloadLibraries(JsonArray libraries, Consumer<Double> progressCallback,
                                   Consumer<String> statusCallback) throws Exception {

        List<LibraryDownload> downloads = new ArrayList<>();

        for (JsonElement elem : libraries) {
            JsonObject library = elem.getAsJsonObject();

            // Skip if library is for different OS
            if (library.has("natives")) {
                JsonObject natives = library.getAsJsonObject("natives");
                String osName = getOsName();
                if (!natives.has(osName)) {
                    continue;
                }
            }

            Path libPath = resolveLibraryPath(library);
            if (libPath != null && !Files.exists(libPath)) {
                downloads.add(new LibraryDownload(
                    getLibraryDownloadUrl(library),
                    libPath,
                    getLibrarySize(library)
                ));
            }
        }

        LOGGER.info("Need to download {} libraries", downloads.size());
        if (downloads.size() > 0 && statusCallback != null) {
            statusCallback.accept(String.format("Загрузка библиотек (0/%d)...", downloads.size()));
        }

        for (int i = 0; i < downloads.size(); i++) {
            LibraryDownload download = downloads.get(i);
            double progress = (double) i / downloads.size();

            if (statusCallback != null && i % 10 == 0) {
                statusCallback.accept(String.format("Загрузка библиотек (%d/%d)...", i, downloads.size()));
            }

            try {
                downloadFile(download.url, download.path, download.size, p -> {
                    progressCallback.accept(progress + (p / downloads.size()));
                });
            } catch (Exception e) {
                LOGGER.warn("Failed to download library {}: {}", download.path, e.getMessage());
            }
        }

        if (downloads.size() > 0 && statusCallback != null) {
            statusCallback.accept(String.format("Загрузка библиотек (%d/%d) завершена", downloads.size(), downloads.size()));
        }
    }

    /**
     * Download assets
     */
    private void downloadAssets(JsonObject assetIndex, Consumer<Double> progressCallback,
                               Consumer<String> statusCallback) throws Exception {

        String assetUrl = assetIndex.get("url").getAsString();
        String assetId = assetIndex.get("id").getAsString();

        Path assetDir = assetsDir.resolve("indexes");
        Files.createDirectories(assetDir);
        Path indexPath = assetDir.resolve(assetId + ".json");

        // Download asset index
        JsonObject indexJson = downloadJson(assetUrl);
        Files.writeString(indexPath, GSON.toJson(indexJson));

        // Download objects
        JsonObject objects = indexJson.getAsJsonObject("objects");
        List<JsonObject> assetList = new ArrayList<>();

        for (var entry : objects.entrySet()) {
            assetList.add(entry.getValue().getAsJsonObject());
        }

        LOGGER.info("Downloading {} assets", assetList.size());
        if (statusCallback != null) {
            statusCallback.accept(String.format("Загрузка ресурсов (0/%d файлов)...", assetList.size()));
        }

        Path objectsDir = assetsDir.resolve("objects");
        Files.createDirectories(objectsDir);

        int downloadedCount = 0;
        int totalToDownload = 0;
        for (JsonObject asset : assetList) {
            String hash = asset.get("hash").getAsString();
            String prefix = hash.substring(0, 2);
            Path assetPath = objectsDir.resolve(prefix).resolve(hash);
            if (!Files.exists(assetPath)) {
                totalToDownload++;
            }
        }

        for (int i = 0; i < assetList.size(); i++) {
            JsonObject asset = assetList.get(i);
            String hash = asset.get("hash").getAsString();
            long size = asset.get("size").getAsLong();

            String prefix = hash.substring(0, 2);
            Path assetPath = objectsDir.resolve(prefix).resolve(hash);

            if (!Files.exists(assetPath)) {
                String assetFileUrl = "https://resources.download.minecraft.net/" + prefix + "/" + hash;

                try {
                    downloadFile(assetFileUrl, assetPath, size, p -> {});
                    downloadedCount++;
                    if (statusCallback != null && downloadedCount % 50 == 0) {
                        statusCallback.accept(String.format("Загрузка ресурсов (%d/%d файлов)...", downloadedCount, totalToDownload));
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to download asset {}: {}", hash, e.getMessage());
                }
            }

            if (i % 100 == 0) {
                double progress = (double) i / assetList.size();
                progressCallback.accept(progress);
            }
        }

        if (statusCallback != null) {
            statusCallback.accept(String.format("Загрузка ресурсов завершена (%d файлов)", downloadedCount));
        }
    }

    /**
     * Install TopZurdo mod
     */
    public void installTopZurdoMod() throws Exception {
        Path modsDir = TopZurdoLauncher.MODS_DIR;
        Files.createDirectories(modsDir);

        Path modJar = modsDir.resolve(MOD_JAR_NAME);

        // Remove existing mod
        if (Files.exists(modJar)) {
            Files.delete(modJar);
            LOGGER.info("Removed existing mod file");
        }

        // Find mod in launcher JAR resources
        Path modSource = findModInLauncher();
        if (modSource != null && Files.exists(modSource)) {
            Files.copy(modSource, modJar, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("TopZurdo mod installed from launcher JAR: {}", modJar);
            return;
        }

        // DEV MODE: Look for mod in project directory
        if (DEV_MODE) {
            Path projectRoot = Path.of(System.getProperty("user.dir")).getParent();
            if (projectRoot == null) {
                projectRoot = Path.of(System.getProperty("user.dir"));
            }

            Path[] candidates = new Path[] {
                projectRoot.resolve("mod").resolve("build").resolve("libs").resolve(MOD_JAR_NAME)
            };

            for (Path candidate : candidates) {
                if (candidate != null && Files.exists(candidate)) {
                    Files.copy(candidate, modJar, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("TopZurdo mod installed from project: {}", modJar);
                    return;
                }
            }
        }

        throw new RuntimeException("TopZurdo mod JAR not found in launcher or project");
    }

    /**
     * Find mod JAR in launcher resources
     */
    private Path findModInLauncher() {
        try {
            // Try to extract from launcher JAR resources
            String resourcePath = "/mods/" + MOD_JAR_NAME;
            InputStream is = getClass().getResourceAsStream(resourcePath);

            if (is != null) {
                // Create temporary file
                Path tempFile = Files.createTempFile("topzurdo-mod-", ".jar");
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                is.close();
                return tempFile;
            }
        } catch (Exception e) {
            LOGGER.debug("Mod not found in launcher resources: {}", e.getMessage());
        }
        return null;
    }

    private void downloadFabric(Consumer<String> statusCallback) throws Exception {
        boolean fabricInstalled = isFabricInstalled();

        if (fabricInstalled) {
            LOGGER.info("Fabric already installed, checking for required files...");
            return;
        }

        LOGGER.info("Installing Fabric {}...", FABRIC_VERSION);

        Path fabricDir = versionsDir.resolve(FABRIC_VERSION_ID);
        Files.createDirectories(fabricDir);

        // Load vanilla version JSON as base
        Path vanillaJsonPath = versionsDir.resolve(MC_VERSION).resolve(MC_VERSION + ".json");
        if (!Files.exists(vanillaJsonPath)) {
            throw new RuntimeException("Vanilla version " + MC_VERSION + " must be installed first");
        }

        // Download Fabric profile JSON
        statusCallback.accept("Fabric: загрузка профиля...");
        String fabricJsonUrl = String.format(FABRIC_JSON_URL, MC_VERSION, FABRIC_VERSION);
        LOGGER.info("Downloading Fabric profile from: {}", fabricJsonUrl);

        JsonObject fabricProfile = downloadJson(fabricJsonUrl);
        if (fabricProfile == null) {
            throw new RuntimeException("Failed to download Fabric profile");
        }

        // Extract version info from Fabric profile
        // Fabric API v2 returns the version JSON directly, not wrapped in "versionInfo"
        JsonObject versionInfo = fabricProfile.has("versionInfo")
            ? fabricProfile.getAsJsonObject("versionInfo")
            : fabricProfile; // If no "versionInfo" wrapper, use the whole object

        if (versionInfo == null || versionInfo.size() == 0) {
            throw new RuntimeException("Fabric profile missing versionInfo");
        }

        // Ensure required Fabric fields
        versionInfo.addProperty("id", FABRIC_VERSION_ID);
        if (!versionInfo.has("inheritsFrom")) {
            versionInfo.addProperty("inheritsFrom", MC_VERSION);
        }

        // Verify mainClass is correct for Fabric
        String mainClass = versionInfo.has("mainClass")
            ? versionInfo.get("mainClass").getAsString()
            : "net.fabricmc.loader.launch.knot.KnotClient";
        versionInfo.addProperty("mainClass", mainClass);

        LOGGER.info("Fabric mainClass: {}", mainClass);

        // Save Fabric version JSON
        Path fabricJsonPath = fabricDir.resolve(FABRIC_VERSION_ID + ".json");
        Files.writeString(fabricJsonPath, GSON.toJson(versionInfo));
        LOGGER.info("Saved Fabric version JSON to: {}", fabricJsonPath);

        // Copy vanilla JAR as Fabric JAR
        Path vanillaJar = versionsDir.resolve(MC_VERSION).resolve(MC_VERSION + ".jar");
        Path fabricJar = fabricDir.resolve(FABRIC_VERSION_ID + ".jar");
        if (Files.exists(vanillaJar) && !Files.exists(fabricJar)) {
            Files.copy(vanillaJar, fabricJar, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Copied vanilla JAR as Fabric JAR");
        }

        // Download Fabric libraries
        JsonArray fabricLibraries = versionInfo.getAsJsonArray("libraries");
        if (fabricLibraries != null && fabricLibraries.size() > 0) {
            statusCallback.accept("Fabric: загрузка библиотек (" + fabricLibraries.size() + ")...");
            LOGGER.info("Downloading {} Fabric libraries...", fabricLibraries.size());
            downloadLibraries(fabricLibraries, p -> {}, statusCallback);
        }

        LOGGER.info("Fabric installation completed successfully");
    }

    /**
     * Get OS name for native libraries
     */
    private static String getOsName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }

    /**
     * Resolve library path from library JSON object
     */
    private Path resolveLibraryPath(JsonObject library) {
        // First try downloads.artifact format (standard Minecraft)
        JsonObject downloads = library.getAsJsonObject("downloads");
        if (downloads != null) {
            JsonObject artifact = downloads.getAsJsonObject("artifact");
            if (artifact != null && artifact.has("path")) {
                String path = artifact.get("path").getAsString();
                return librariesDir.resolve(path);
            }
        }

        // Fallback to Maven coordinates (Fabric libraries)
        if (library.has("name")) {
            String name = library.get("name").getAsString();
            String path = buildMavenPath(name);
            if (path != null) {
                return librariesDir.resolve(path);
            }
        }

        return null;
    }

    /**
     * Build Maven path from library name
     */
    private String buildMavenPath(String name) {
        String[] parts = name.split(":");
        if (parts.length < 3) return null;

        String groupId = parts[0].replace('.', '/');
        String artifactId = parts[1];
        String version = parts[2];
        String classifier = "";
        String ext = "jar";

        // Handle classifier and extension in version part
        if (version.contains("@")) {
            String[] versionParts = version.split("@");
            version = versionParts[0];
            ext = versionParts[1];
        }

        if (parts.length > 3) {
            classifier = "-" + parts[3];
        }

        return String.format("%s/%s/%s/%s%s-%s%s.%s",
            groupId, artifactId, version, artifactId, version, classifier, ext);
    }

    /**
     * Get library download URL
     */
    private String getLibraryDownloadUrl(JsonObject library) {
        // Try downloads.artifact first
        JsonObject downloads = library.getAsJsonObject("downloads");
        if (downloads != null) {
            JsonObject artifact = downloads.getAsJsonObject("artifact");
            if (artifact != null && artifact.has("url")) {
                return artifact.get("url").getAsString();
            }
        }

        // Fallback to Maven central
        if (library.has("name")) {
            String name = library.get("name").getAsString();
            return "https://libraries.minecraft.net/" + buildMavenPath(name);
        }

        return null;
    }

    /**
     * Get library size
     */
    private long getLibrarySize(JsonObject library) {
        JsonObject downloads = library.getAsJsonObject("downloads");
        if (downloads != null) {
            JsonObject artifact = downloads.getAsJsonObject("artifact");
            if (artifact != null && artifact.has("size")) {
                return artifact.get("size").getAsLong();
            }
        }
        return 0;
    }

    /**
     * Download file with progress callback
     */
    private void downloadFile(String url, Path targetPath, long expectedSize, Consumer<Double> progressCallback) throws Exception {
        LOGGER.info("Downloading: {} -> {}", url, targetPath);

        Files.createDirectories(targetPath.getParent());

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Zurdo-Launcher/1.0");
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP " + responseCode + " for " + url);
        }

        long fileSize = connection.getContentLengthLong();
        if (expectedSize > 0 && fileSize != expectedSize) {
            LOGGER.warn("File size mismatch: expected {}, got {}", expectedSize, fileSize);
        }

        try (InputStream is = connection.getInputStream();
             FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int read;

            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                totalRead += read;

                if (progressCallback != null && fileSize > 0) {
                    double progress = (double) totalRead / fileSize;
                    progressCallback.accept(progress);
                }
            }
        }

        LOGGER.info("Downloaded: {} ({} bytes)", targetPath, Files.size(targetPath));
    }

    /**
     * Download JSON from URL
     */
    private JsonObject downloadJson(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Zurdo-Launcher/1.0");
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP " + responseCode + " for " + url);
        }

        try (InputStream is = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return GSON.fromJson(content.toString(), JsonObject.class);
        }
    }

    /**
     * Library download info holder
     */
    private static class LibraryDownload {
        final String url;
        final Path path;
        final long size;

        LibraryDownload(String url, Path path, long size) {
            this.url = url;
            this.path = path;
            this.size = size;
        }
    }
}
