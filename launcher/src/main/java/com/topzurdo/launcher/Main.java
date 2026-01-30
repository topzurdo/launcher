package com.topzurdo.launcher;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

import javax.swing.JOptionPane;

/**
 * Main entry point for fat JAR
 * This class is needed to bypass JavaFX module restrictions in fat JAR
 *
 * JavaFX checks if the main class extends Application and if the module system
 * is properly set up. By using a separate main class that doesn't extend Application,
 * we can launch the actual Application class without module system issues.
 *
 * Also sets up crash logging and uncaught exception handler so errors are visible
 * when the app is run without console (e.g. from jpackage EXE).
 */
public class Main {

    private static final Path LOGS_DIR = Paths.get(System.getProperty("user.home"), ".topzurdo", "logs");
    private static final Path CRASH_LOG = LOGS_DIR.resolve("crash.log");

    public static void main(String[] args) {
        // Add shutdown hook to ensure clean exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Cleanup on exit
            System.exit(0);
        }));

        ensureLogsDir();
        setUncaughtExceptionHandler();
        cleanupInstaller();

        try {
            TopZurdoLauncher.main(args);
        } catch (Throwable t) {
            handleCrash(t, "Startup failed");
            // Ensure process exits even if dialog fails
            System.exit(1);
        }
    }

    private static void cleanupInstaller() {
        try {
            // Check if running from installed location and delete installer
            String userHome = System.getProperty("user.home");
            Path installerPath = Paths.get(userHome, "Desktop", "TopZurdo-Installer.exe");

            if (Files.exists(installerPath)) {
                // Schedule deletion after a delay to ensure the launcher has fully started
                Thread cleanupThread = new Thread(() -> {
                    try {
                        Thread.sleep(5000); // Wait 5 seconds
                        Files.deleteIfExists(installerPath);
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                });
                cleanupThread.setDaemon(true);
                cleanupThread.start();
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    private static void ensureLogsDir() {
        try {
            Files.createDirectories(LOGS_DIR);
        } catch (IOException e) {
            // best effort; crash handler will try to write anyway
        }
    }

    private static void setUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            handleCrash(throwable, "Uncaught exception in thread " + thread.getName());
        });
    }

    private static void handleCrash(Throwable t, String context) {
        String stackTrace = getStackTrace(t);
        String logLine = String.format("%s%n--- %s ---%n%s%n",
            Instant.now(), context, stackTrace);

        try {
            Files.write(CRASH_LOG, logLine.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // ignore
        }

        System.err.println(context);
        t.printStackTrace(System.err);

        showCrashDialog(t.getMessage(), stackTrace, CRASH_LOG.toString());
        System.exit(1);
    }

    private static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        Throwable cause = t.getCause();
        while (cause != null) {
            sw.append("\nCaused by: ");
            cause.printStackTrace(new PrintWriter(sw));
            cause = cause.getCause();
        }
        return sw.toString();
    }

    private static void showCrashDialog(String message, String stackTrace, String crashLogPath) {
        try {
            String text = "An error occurred:\n\n"
                + (message != null ? message + "\n\n" : "")
                + "Crash log: " + crashLogPath + "\n\n"
                + "You can send this file when reporting the issue.";
            JOptionPane.showMessageDialog(null, text, "Zurdo Launcher - Error",
                JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ignored) {
            // headless or Swing not available - print to console
            System.err.println("ERROR: " + (message != null ? message : "Unknown error"));
            System.err.println("Crash log: " + crashLogPath);
        } finally {
            // Always exit, even if dialog fails
            System.exit(1);
        }
    }
}
