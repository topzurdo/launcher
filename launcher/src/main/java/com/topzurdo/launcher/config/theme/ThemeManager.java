package com.topzurdo.launcher.config.theme;

import javafx.scene.Scene;

/**
 * Theme (light/dark) and CSS reapplication.
 */
public final class ThemeManager {

    public enum Mode { LIGHT, DARK }

    private static Mode currentMode = Mode.DARK;

    public static void setMode(Mode mode) {
        currentMode = mode;
    }

    public static Mode getMode() { return currentMode; }

    public static void reapplyToScene(Scene scene, String stylesheetUrl) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        if (stylesheetUrl != null && !stylesheetUrl.isEmpty()) {
            scene.getStylesheets().add(stylesheetUrl);
        }
    }

    private ThemeManager() {}
}
