package com.topzurdo.launcher.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.topzurdo.launcher.TopZurdoLauncher;
import com.topzurdo.launcher.util.OSUtils;

/**
 * Launcher configuration manager
 * Handles saving/loading user preferences
 */
public class LauncherConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(LauncherConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = TopZurdoLauncher.CONFIG_DIR.resolve("launcher.json");

    private static LauncherConfig instance;

    // User settings
    private String username = "";
    private int allocatedRamMb = 4096;
    private String javaPath = "";
    private boolean autoConnect = true;
    private String lastServer = "mc.funtime.su";
    private boolean fullscreen = false;
    private int windowWidth = 1280;
    private int windowHeight = 720;
    private boolean showFps = true;
    private boolean autoUpdate = true;
    private String language = "ru";
    private boolean fabricDebugLogging = false;
    private boolean lightTheme = false;
    private String jvmProfile = "MEDIUM";
    private boolean autoRam = false;

    // Visual preferences - passed to mod
    private int preferredColor = 0x22D3EE; // Default cyan
    private boolean customColorEnabled = false;

    // JVM Arguments (default = MEDIUM profile)
    private String jvmArgs = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 " +
            "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch " +
            "-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M " +
            "-XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 " +
            "-XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 " +
            "-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem " +
            "-XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true";

    private LauncherConfig() {}

    /**
     * Load configuration from file or create default
     */
    public static LauncherConfig load() {
        if (instance != null) {
            return instance;
        }

        if (Files.exists(CONFIG_FILE)) {
            try {
                String json = Files.readString(CONFIG_FILE);
                instance = GSON.fromJson(json, LauncherConfig.class);
                LOGGER.info("Loaded configuration from {}", CONFIG_FILE);
            } catch (IOException e) {
                LOGGER.error("Failed to load config, using defaults", e);
                instance = new LauncherConfig();
            }
        } else {
            instance = new LauncherConfig();
            instance.detectJavaPath();
            instance.save();
        }

        return instance;
    }

    /**
     * Save configuration to file
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, GSON.toJson(this));
            LOGGER.info("Saved configuration to {}", CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    /**
     * Detect Java installation path
     * Prefers Java 8/11/16 for Fabric 1.16.5 compatibility
     */
    private void detectJavaPath() {
        // Try to find Java 8 first (best for Fabric 1.16.5)
        String[] java8Paths = {
            "C:\\Program Files\\Java\\jre1.8.0_431\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk1.8.0_431\\bin\\java.exe",
            "C:\\Program Files\\Eclipse Adoptium\\jdk-8",
            "C:\\Program Files\\Java\\jre-1.8",
            "C:\\Program Files\\Java\\jdk-1.8"
        };

        // Check for Java 8 in common locations
        for (String basePath : java8Paths) {
            Path path = Path.of(basePath);
            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    Path javaBin = path.resolve("bin").resolve("java.exe");
                    if (Files.exists(javaBin)) {
                        this.javaPath = javaBin.toString();
                        LOGGER.info("Found Java 8 at: {}", this.javaPath);
                        return;
                    }
                } else if (basePath.endsWith(".exe")) {
                    this.javaPath = basePath;
                    LOGGER.info("Found Java 8 at: {}", this.javaPath);
                    return;
                }
            }
        }

        // Search for any Java 8 installation
        Path javaDir = Path.of("C:\\Program Files\\Java");
        if (Files.exists(javaDir)) {
            try (var stream = Files.list(javaDir)) {
                var java8 = stream
                    .filter(p -> p.getFileName().toString().contains("1.8") ||
                                 p.getFileName().toString().contains("jdk-8") ||
                                 p.getFileName().toString().contains("jre-8"))
                    .findFirst();
                if (java8.isPresent()) {
                    Path javaBin = java8.get().resolve("bin").resolve("java.exe");
                    if (Files.exists(javaBin)) {
                        this.javaPath = javaBin.toString();
                        LOGGER.info("Found Java 8 at: {}", this.javaPath);
                        return;
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("Error searching for Java 8", e);
            }
        }

        // Fallback to current Java (may cause issues with Fabric 1.16.5 if Java 17+)
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Path javaBin = Path.of(javaHome, "bin", "java.exe");
            if (Files.exists(javaBin)) {
                this.javaPath = javaBin.toString();
            } else {
                javaBin = Path.of(javaHome, "bin", "java");
                if (Files.exists(javaBin)) {
                    this.javaPath = javaBin.toString();
                }
            }
        }

        if (this.javaPath.isEmpty()) {
            this.javaPath = "java";
        }

        LOGGER.warn("Java 8 not found, using: {} - Fabric 1.16.5 may have issues with Java 17+", this.javaPath);
    }

    /**
     * Get recommended RAM based on system (50–70% of available memory).
     * Uses com.sun.management on supported JVMs, fallback to Runtime estimate.
     */
    public static int getRecommendedRam() {
        long totalMb = OSUtils.getTotalMemoryMb();
        if (totalMb <= 0) {
            totalMb = Runtime.getRuntime().maxMemory() / (1024 * 1024) * 4;
        }
        if (totalMb < 4096) {
            totalMb = 8192; // Assume at least 8GB
        }
        // 50–70% of total RAM for Minecraft (document recommendation)
        int recommended = (int) (totalMb * 0.6);
        return Math.max(2048, Math.min(8192, recommended));
    }

    // Getters and Setters

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAllocatedRamMb() {
        return allocatedRamMb;
    }

    public void setAllocatedRamMb(int allocatedRamMb) {
        this.allocatedRamMb = allocatedRamMb;
    }

    public String getJavaPath() {
        return javaPath;
    }

    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public String getLastServer() {
        return lastServer;
    }

    public void setLastServer(String lastServer) {
        this.lastServer = lastServer;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public boolean isShowFps() {
        return showFps;
    }

    public void setShowFps(boolean showFps) {
        this.showFps = showFps;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(String jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public boolean isFabricDebugLogging() {
        return fabricDebugLogging;
    }

    public void setFabricDebugLogging(boolean fabricDebugLogging) {
        this.fabricDebugLogging = fabricDebugLogging;
    }

    public boolean isLightTheme() {
        return lightTheme;
    }

    public void setLightTheme(boolean lightTheme) {
        this.lightTheme = lightTheme;
    }

    public String getJvmProfile() {
        return jvmProfile;
    }

    public void setJvmProfile(String jvmProfile) {
        this.jvmProfile = jvmProfile;
        JvmProfile profile = JvmProfile.fromId(jvmProfile);
        this.jvmArgs = profile.getJvmArgs();
    }

    public boolean isAutoRam() {
        return autoRam;
    }

    public void setAutoRam(boolean autoRam) {
        this.autoRam = autoRam;
    }

    /** Returns effective JVM args (from profile or custom). */
    public String getEffectiveJvmArgs() {
        return jvmArgs;
    }

    public static LauncherConfig getInstance() {
        return instance != null ? instance : load();
    }

    // Preferred color settings

    public int getPreferredColor() {
        return preferredColor;
    }

    public void setPreferredColor(int preferredColor) {
        this.preferredColor = preferredColor;
    }

    public boolean isCustomColorEnabled() {
        return customColorEnabled;
    }

    public void setCustomColorEnabled(boolean customColorEnabled) {
        this.customColorEnabled = customColorEnabled;
    }

    /**
     * Get preferred color as hex string (without #)
     */
    public String getPreferredColorHex() {
        return String.format("%06X", preferredColor & 0xFFFFFF);
    }

    /**
     * Set preferred color from hex string
     */
    public void setPreferredColorFromHex(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            this.preferredColor = Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid color hex: {}", hex);
        }
    }
}
