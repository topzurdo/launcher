package com.topzurdo.launcher.game;

import com.topzurdo.launcher.config.LauncherConfig;
import com.topzurdo.launcher.download.MinecraftDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles launching Minecraft with configured settings
 */
public class GameLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(GameLauncher.class);
    /** Must match MinecraftDownloader.FABRIC_VERSION_ID so version dir and JAR are found */
    private static final String FABRIC_VERSION_ID = "1.16.5-fabric-0.15.11";

    private final LauncherConfig config;

    public GameLauncher(LauncherConfig config) {
        this.config = config;
    }

    public Process launch(String username) throws IOException {
        LOG.info("Launching Minecraft for user: {}", username);

        Path gameDir = config.getMinecraftDir();
        Path javaPath = findJava();

        List<String> command = new ArrayList<>();
        command.add(javaPath.toString());

        // JVM arguments: память и пути
        command.add("-Xms512M");
        command.add("-Xmx" + config.getMaxMemoryMB() + "M");
        String jvmArgs = config.getEffectiveJvmArgs();
        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            for (String arg : jvmArgs.split("\\s+")) {
                if (!arg.isEmpty()) command.add(arg);
            }
        }
        command.add("-Djava.library.path=" + gameDir.resolve("natives"));
        command.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + gameDir.resolve("natives"));

        // Classpath
        command.add("-cp");
        command.add(buildClasspath(gameDir));

        // Main class
        command.add("net.fabricmc.loader.impl.launch.knot.KnotClient");

        // Game arguments
        command.add("--username");
        command.add(username);
        command.add("--version");
        command.add(FABRIC_VERSION_ID);
        command.add("--gameDir");
        command.add(gameDir.toString());
        command.add("--assetsDir");
        command.add(gameDir.resolve("assets").toString());
        command.add("--assetIndex");
        command.add("1.16");
        command.add("--accessToken");
        command.add("0");

        LOG.debug("Launch command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(gameDir.toFile());
        pb.redirectErrorStream(true);
        pb.inheritIO();

        return pb.start();
    }

    private Path findJava() {
        // Try JAVA_HOME first
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            Path java = Path.of(javaHome, "bin", isWindows() ? "java.exe" : "java");
            if (java.toFile().exists()) return java;
        }

        // Try system java
        return Path.of(isWindows() ? "java.exe" : "java");
    }

    private String buildClasspath(Path gameDir) {
        StringBuilder cp = new StringBuilder();
        try {
            // Libraries from version JSON with OS rules (avoids mixing lwjgl 3.2.1 + 3.2.2 -> native crash)
            MinecraftDownloader downloader = new MinecraftDownloader();
            List<Path> libPaths = downloader.getClasspathLibraries(gameDir);
            for (Path p : libPaths) {
                if (cp.length() > 0) cp.append(File.pathSeparator);
                cp.append(p.toAbsolutePath().toString());
            }
        } catch (Exception e) {
            LOG.warn("Could not build classpath from version JSON, falling back to full scan: {}", e.getMessage());
            File libDir = gameDir.resolve("libraries").toFile();
            addJarsFromDirectory(libDir, cp);
        }

        // Add Fabric version JAR (game jar for this profile)
        File versionDir = gameDir.resolve("versions").resolve(FABRIC_VERSION_ID).toFile();
        addJarsFromDirectory(versionDir, cp);

        // Add mods
        File modsDir = gameDir.resolve("mods").toFile();
        addJarsFromDirectory(modsDir, cp);

        return cp.toString();
    }

    private void addJarsFromDirectory(File dir, StringBuilder cp) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                addJarsFromDirectory(file, cp);
            } else if (file.getName().endsWith(".jar")) {
                if (cp.length() > 0) cp.append(File.pathSeparator);
                cp.append(file.getAbsolutePath());
            }
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
