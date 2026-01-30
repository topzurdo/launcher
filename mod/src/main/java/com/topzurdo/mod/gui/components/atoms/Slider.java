package com.topzurdo.mod.gui.components.atoms;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Consumer;

/**
 * Slider component for numeric values
 */
public class Slider {

    private int x, y, width;
    private static final int HEIGHT = 16;
    private static final int TRACK_HEIGHT = 4;

    private float value;
    private float min, max;
    private Consumer<Float> onChange;
    private boolean dragging;

    public Slider(int x, int y, int width, float value, float min, float max, Consumer<Float> onChange) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.value = value;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        int trackY = y + (HEIGHT - TRACK_HEIGHT) / 2;

        // Track background
        UIRenderHelper.drawRoundRect(ms, x, trackY, width, TRACK_HEIGHT, 2, OceanTheme.BG_ELEVATED);

        // Filled track
        float normalized = (value - min) / (max - min);
        int filledWidth = (int) (width * normalized);
        UIRenderHelper.drawRoundRect(ms, x, trackY, filledWidth, TRACK_HEIGHT, 2, OceanTheme.ACCENT);

        // Knob
        int knobX = x + filledWidth - 6;
        UIRenderHelper.drawRoundRect(ms, knobX, y, 12, HEIGHT, 6, OceanTheme.ACCENT);

        // Value text
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        String valueText = String.format("%.2f", value);
        tr.draw(ms, valueText, x + width + 5, y + 4, OceanTheme.TEXT_SECONDARY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            dragging = true;
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    private void updateValue(double mouseX) {
        float normalized = (float) Math.max(0, Math.min(1, (mouseX - x) / width));
        value = min + normalized * (max - min);
        if (onChange != null) onChange.accept(value);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEIGHT;
    }

    public void setValue(float value) { this.value = Math.max(min, Math.min(max, value)); }
    public float getValue() { return value; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return HEIGHT; }
}
