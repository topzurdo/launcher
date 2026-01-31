package com.topzurdo.launcher.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.topzurdo.launcher.TopZurdoLauncher;

/**
 * Метаданные сборки: загрузчик (Fabric/Forge), версия MC, версия модпака.
 * Файл metadata.json рядом с launcher.json — лаунчер строит UI по нему без перекомпиляции.
 */
public class LauncherMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(LauncherMetadata.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path METADATA_FILE = TopZurdoLauncher.CONFIG_DIR.resolve("metadata.json");

    private static LauncherMetadata instance;

    private String loader = "Fabric";
    private String mcVersion = "1.16.5";
    private String modpackVersion = "1.0.2";

    public static LauncherMetadata load() {
        if (instance != null) return instance;
        if (Files.exists(METADATA_FILE)) {
            try {
                String json = Files.readString(METADATA_FILE);
                instance = GSON.fromJson(json, LauncherMetadata.class);
                if (instance.loader == null) instance.loader = "Fabric";
                if (instance.mcVersion == null) instance.mcVersion = "1.16.5";
                if (instance.modpackVersion == null) instance.modpackVersion = "1.0.2";
                LOGGER.info("Loaded metadata: {} {} v{}", instance.loader, instance.mcVersion, instance.modpackVersion);
            } catch (IOException e) {
                LOGGER.warn("Failed to load metadata.json, using defaults: {}", e.getMessage());
                instance = new LauncherMetadata();
            }
        } else {
            instance = new LauncherMetadata();
            instance.save();
        }
        return instance;
    }

    public void save() {
        try {
            Files.createDirectories(METADATA_FILE.getParent());
            Files.writeString(METADATA_FILE, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.warn("Failed to save metadata.json: {}", e.getMessage());
        }
    }

    public String getLoader() { return loader; }
    public void setLoader(String loader) { this.loader = loader; }
    public String getMcVersion() { return mcVersion; }
    public void setMcVersion(String mcVersion) { this.mcVersion = mcVersion; }
    public String getModpackVersion() { return modpackVersion; }
    public void setModpackVersion(String modpackVersion) { this.modpackVersion = modpackVersion; }

    /** Строка для NavRail: "v1.0.0 | Fabric 1.16.5" */
    public String getVersionLine() {
        return "v" + modpackVersion + " | " + loader + " " + mcVersion;
    }
}
