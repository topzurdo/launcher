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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
    private static final String FABRIC_VERSION = "0.15.11";
    private static final String FABRIC_VERSION_ID = "1.16.5-fabric-0.15.11";
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
     * Ensure native libraries (LWJGL .dll etc.) are extracted to gameDir/natives.
     * Called before launch so that already-installed games get natives if they were missing.
     */
    public void ensureNatives(Consumer<String> statusCallback) {
        Path nativesDir = minecraftDir.resolve("natives");
        if (Files.exists(nativesDir)) {
            try {
                boolean hasNative = Files.list(nativesDir).anyMatch(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib");
                });
                if (hasNative) {
                    LOGGER.debug("Natives already present");
                    return;
                }
            } catch (Exception e) {
                LOGGER.warn("Could not check natives dir: {}", e.getMessage());
            }
        }
        Path versionJsonPath = versionsDir.resolve(MC_VERSION).resolve(MC_VERSION + ".json");
        if (!Files.exists(versionJsonPath)) {
            LOGGER.warn("Version JSON not found, cannot ensure natives");
            return;
        }
        try {
            JsonObject versionJson = GSON.fromJson(Files.readString(versionJsonPath), JsonObject.class);
            JsonArray libraries = versionJson.getAsJsonArray("libraries");
            extractNatives(libraries, statusCallback != null ? statusCallback : s -> {});
        } catch (Exception e) {
            LOGGER.error("Failed to ensure natives: {}", e.getMessage(), e);
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

            // Step 4b: Download and extract native libraries (LWJGL etc.) to natives/
            statusCallback.accept("Распаковка нативных библиотек...");
            extractNatives(libraries, statusCallback);

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
     * Download native classifier JARs (e.g. natives-windows) and extract them to gameDir/natives
     * so LWJGL can load lwjgl.dll etc.
     */
    private void extractNatives(JsonArray libraries, Consumer<String> statusCallback) throws Exception {
        Path nativesDir = minecraftDir.resolve("natives");
        Files.createDirectories(nativesDir);
        String osName = getOsName();

        List<NativeExtract> toExtract = new ArrayList<>();
        for (JsonElement elem : libraries) {
            JsonObject library = elem.getAsJsonObject();
            if (!library.has("natives")) continue;
            JsonObject natives = library.getAsJsonObject("natives");
            if (!natives.has(osName)) continue;
            String classifierKey = natives.get(osName).getAsString();
            JsonObject downloads = library.getAsJsonObject("downloads");
            if (downloads == null || !downloads.has("classifiers")) continue;
            JsonObject classifiers = downloads.getAsJsonObject("classifiers");
            if (!classifiers.has(classifierKey)) continue;
            JsonObject classifierEntry = classifiers.getAsJsonObject(classifierKey);
            String path = classifierEntry.get("path").getAsString();
            String url = classifierEntry.get("url").getAsString();
            Path jarPath = librariesDir.resolve(path);
            toExtract.add(new NativeExtract(url, jarPath, classifierEntry.has("size") ? classifierEntry.get("size").getAsLong() : 0));
        }

        for (int i = 0; i < toExtract.size(); i++) {
            NativeExtract ne = toExtract.get(i);
            if (statusCallback != null) {
                statusCallback.accept(String.format("Нативные библиотеки (%d/%d)...", i + 1, toExtract.size()));
            }
            if (!Files.exists(ne.jarPath)) {
                Files.createDirectories(ne.jarPath.getParent());
                downloadFile(ne.url, ne.jarPath, ne.size, p -> {});
            }
            extractZipTo(ne.jarPath, nativesDir);
        }
        if (statusCallback != null && !toExtract.isEmpty()) {
            statusCallback.accept("Нативные библиотеки распакованы");
        }
    }

    private void extractZipTo(Path zipPath, Path destDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                if (entry.getName().startsWith("META-INF/")) continue;
                Path out = destDir.resolve(entry.getName()).normalize();
                Path destAbs = destDir.toAbsolutePath().normalize();
                if (!out.toAbsolutePath().normalize().startsWith(destAbs)) continue; // security: no zip slip
                Files.createDirectories(out.getParent());
                try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = zis.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
        LOGGER.info("Extracted natives from {} to {}", zipPath, destDir);
    }

    private static class NativeExtract {
        final String url;
        final Path jarPath;
        final long size;

        NativeExtract(String url, Path jarPath, long size) {
            this.url = url;
            this.jarPath = jarPath;
            this.size = size;
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

        // DEV MODE: Look for mod in project directory. When running via "gradlew :launcher:run",
        // user.dir is often the launcher subdirectory, so try both user.dir and its parent (repo root).
        if (DEV_MODE) {
            Path cwd = Path.of(System.getProperty("user.dir"));
            Path[] possibleRoots = { cwd, cwd.getParent() };
            String[] jarNames = { "topzurdo-1.0.0.jar", MOD_JAR_NAME };
            for (Path projectRoot : possibleRoots) {
                if (projectRoot == null) continue;
                Path modLibs = projectRoot.resolve("mod").resolve("build").resolve("libs");
                for (String jarName : jarNames) {
                    Path candidate = modLibs.resolve(jarName);
                    if (Files.exists(candidate)) {
                        Files.copy(candidate, modJar, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.info("TopZurdo mod installed from project: {}", modJar);
                        return;
                    }
                }
            }
        }

        throw new RuntimeException(
            "TopZurdo mod JAR not found. Build the mod first: run .\\gradlew.bat :mod:build from project root, then start the launcher again.");
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

        // Download Fabric profile JSON (fallback to bundled profile if meta.fabricmc.net is unreachable)
        statusCallback.accept("Fabric: загрузка профиля...");
        String fabricJsonUrl = String.format(FABRIC_JSON_URL, MC_VERSION, FABRIC_VERSION);
        LOGGER.info("Downloading Fabric profile from: {}", fabricJsonUrl);

        JsonObject fabricProfile = null;
        try {
            fabricProfile = downloadJson(fabricJsonUrl);
        } catch (Exception e) {
            LOGGER.warn("Could not download Fabric profile ({}), trying bundled profile", e.getMessage());
            fabricProfile = loadBundledFabricProfile();
        }
        if (fabricProfile == null) {
            throw new RuntimeException("Failed to load Fabric profile (meta.fabricmc.net unreachable and no bundled profile)");
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
        return resolveLibraryPath(library, librariesDir);
    }

    private Path resolveLibraryPath(JsonObject library, Path libDir) {
        // First try downloads.artifact format (standard Minecraft)
        JsonObject downloads = library.getAsJsonObject("downloads");
        if (downloads != null) {
            JsonObject artifact = downloads.getAsJsonObject("artifact");
            if (artifact != null && artifact.has("path")) {
                String path = artifact.get("path").getAsString();
                return libDir.resolve(path);
            }
        }

        // Fallback to Maven coordinates (Fabric libraries)
        if (library.has("name")) {
            String name = library.get("name").getAsString();
            String path = buildMavenPath(name);
            if (path != null) {
                return libDir.resolve(path);
            }
        }

        return null;
    }

    /**
     * Check if a library rule matches current OS (for "rules" array in version JSON).
     */
    private static boolean ruleMatches(JsonObject rule) {
        if (!rule.has("os")) return true;
        JsonObject os = rule.getAsJsonObject("os");
        if (!os.has("name")) return true;
        String ruleOs = os.get("name").getAsString();
        return getOsName().equals(ruleOs);
    }

    /**
     * Whether this library should be included on current OS (evaluate "rules").
     */
    private static boolean libraryMatchesRules(JsonObject library) {
        if (!library.has("rules")) return true;
        JsonArray rules = library.getAsJsonArray("rules");
        for (JsonElement e : rules) {
            JsonObject rule = e.getAsJsonObject();
            if (ruleMatches(rule)) {
                return "allow".equals(rule.get("action").getAsString());
            }
        }
        return false; // no matching rule -> disallow (e.g. lwjgl 3.2.1 only has allow+osx, so exclude on Windows)
    }

    /**
     * Build classpath library list from version JSONs with OS rules applied.
     * Excludes OS-specific libraries (e.g. lwjgl 3.2.1 on Windows) to avoid native version mismatch crash.
     */
    public List<Path> getClasspathLibraries(Path gameDir) throws Exception {
        Path libDir = gameDir.resolve("libraries");
        Path versDir = gameDir.resolve("versions");
        List<Path> out = new ArrayList<>();
        java.util.Set<Path> seen = new java.util.HashSet<>();

        // Vanilla 1.16.5 libraries (with rules)
        Path vanillaJson = versDir.resolve(MC_VERSION).resolve(MC_VERSION + ".json");
        if (Files.exists(vanillaJson)) {
            JsonObject vanilla = GSON.fromJson(Files.readString(vanillaJson), JsonObject.class);
            JsonArray libs = vanilla.getAsJsonArray("libraries");
            for (JsonElement e : libs) {
                JsonObject lib = e.getAsJsonObject();
                if (!libraryMatchesRules(lib)) continue;
                Path p = resolveLibraryPath(lib, libDir);
                if (p != null && Files.exists(p) && seen.add(p)) out.add(p);
            }
        }

        // Fabric profile libraries (no OS rules in practice)
        Path fabricJson = versDir.resolve(FABRIC_VERSION_ID).resolve(FABRIC_VERSION_ID + ".json");
        if (Files.exists(fabricJson)) {
            JsonObject fabric = GSON.fromJson(Files.readString(fabricJson), JsonObject.class);
            if (fabric.has("libraries")) {
                JsonArray libs = fabric.getAsJsonArray("libraries");
                for (JsonElement e : libs) {
                    JsonObject lib = e.getAsJsonObject();
                    Path p = resolveLibraryPath(lib, libDir);
                    if (p != null && Files.exists(p) && seen.add(p)) out.add(p);
                }
            }
        }

        return out;
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

        return String.format("%s/%s/%s/%s-%s%s.%s",
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

        // Fallback: Maven-style (Fabric profile has "name" + optional "url" base; vanilla fallback was libraries.minecraft.net)
        if (library.has("name")) {
            String name = library.get("name").getAsString();
            String path = buildMavenPath(name);
            if (path == null) return null;
            String baseUrl;
            if (library.has("url")) {
                baseUrl = library.get("url").getAsString();
                if (!baseUrl.endsWith("/")) baseUrl += "/";
            } else {
                baseUrl = getMavenBaseUrl(name);
            }
            return baseUrl + path;
        }

        return null;
    }

    /**
     * Maven base URL when library has no "url" (e.g. vanilla-style JSON). Fabric/net.fabricmc -> maven.fabricmc.net; rest -> Maven Central.
     */
    private String getMavenBaseUrl(String mavenName) {
        if (mavenName.startsWith("net.fabricmc:") || mavenName.startsWith("net.fabricmc.")) {
            return "https://maven.fabricmc.net/";
        }
        return "https://repo1.maven.org/maven2/";
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
     * Load Fabric profile from bundled resource when meta.fabricmc.net is unreachable (e.g. DNS block).
     */
    private JsonObject loadBundledFabricProfile() {
        String resourcePath = "/fabric/1.16.5-fabric-0.15.11-profile.json";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            return GSON.fromJson(new InputStreamReader(is), JsonObject.class);
        } catch (Exception e) {
            LOGGER.debug("Bundled Fabric profile not found or invalid: {}", e.getMessage());
            return null;
        }
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
