package com.topzurdo.mod.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

/**
 * GUI utility methods
 */
public class GuiUtil {

    public static TextRenderer getTextRenderer() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static int getTextWidth(String text) {
        return getTextRenderer().getWidth(text);
    }

    public static int getTextHeight() {
        return getTextRenderer().fontHeight;
    }

    public static String trimToWidth(String text, int maxWidth) {
        TextRenderer tr = getTextRenderer();
        if (tr.getWidth(text) <= maxWidth) return text;

        String ellipsis = "...";
        int ellipsisWidth = tr.getWidth(ellipsis);

        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (tr.getWidth(sb.toString() + c) + ellipsisWidth > maxWidth) {
                return sb + ellipsis;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Truncate text to fit within maxWidth
     */
    public static String truncate(TextRenderer tr, String text, int maxWidth) {
        if (text == null) return "";
        if (tr.getWidth(text) <= maxWidth) return text;

        String ellipsis = "...";
        int ellipsisWidth = tr.getWidth(ellipsis);

        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (tr.getWidth(sb.toString() + c) + ellipsisWidth > maxWidth) {
                return sb + ellipsis;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Resolve localization key (returns key itself for now)
     */
    public static String resolveKey(String key) {
        if (key == null) return "";
        // TODO: Implement actual localization lookup
        return key;
    }

    /**
     * Wrap hint text to fit width
     */
    public static java.util.List<String> wrapHint(TextRenderer tr, String text, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        if (tr.getWidth(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.length() > 0 ? current + " " + word : word;
            if (tr.getWidth(test) > maxWidth) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            } else {
                current = new StringBuilder(test);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }
}
