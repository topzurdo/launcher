package com.topzurdo.launcher.config.theme;

import javafx.geometry.Insets;

/**
 * Design tokens: spacing, radii, colors for UI.
 */
public final class DesignTokens {

    public static final double SPACING_8 = 8;
    public static final double SPACING_12 = 12;
    public static final double SPACING_16 = 16;
    public static final double SPACING_24 = 24;
    public static final double SPACING_32 = 32;
    public static final double SPACING_64 = 64;

    public static Insets insets(double horizontal, double vertical) {
        return new Insets(vertical, horizontal, vertical, horizontal);
    }

    private DesignTokens() {}
}
