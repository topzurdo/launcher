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
public class Slider implements com.topzurdo.mod.gui.UIComponent {

    private int x, y, width, height;
    private static final int DEFAULT_HEIGHT = 24;
    private static final int TRACK_HEIGHT = 6;
    private static final int THUMB_RADIUS = 10;

    private String label;
    private float value;
    private float min, max, step;
    private boolean showDecimals;
    private Consumer<Float> onChange;
    private boolean dragging;
    private long lastChangeTime = 0;

    public Slider(int x, int y, int width, float value, float min, float max, Consumer<Float> onChange) {
        this(x, y, width, DEFAULT_HEIGHT, null, min, max, value, 1, false, v -> { if (onChange != null) onChange.accept(v.floatValue()); });
    }

    public Slider(int x, int y, int width, int height, String label, double min, double max, double value, double step, boolean showDecimals, Consumer<Double> onChange) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height > 0 ? height : DEFAULT_HEIGHT;
        this.label = label;
        this.value = (float) value;
        this.min = (float) min;
        this.max = (float) max;
        this.step = (float) step;
        this.showDecimals = showDecimals;
        this.onChange = onChange != null ? v -> onChange.accept((double) v) : null;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        int labelHeight = 0;
        if (label != null && !label.isEmpty()) {
            tr.draw(ms, label, x + 5, y + 4, com.topzurdo.mod.gui.theme.DesignTokens.fgPrimary());
            labelHeight = 16;
        }

        int trackY = y + labelHeight + (height - labelHeight - TRACK_HEIGHT) / 2;
        int valueReserve = 52;
        int sliderWidth = Math.max(60, width - valueReserve);
        boolean hover = isMouseOver(mouseX, mouseY);
        int trackRadius = TRACK_HEIGHT / 2;

        // Track background #334155, height 6
        UIRenderHelper.fillRoundRect(ms, x, trackY, sliderWidth, TRACK_HEIGHT, trackRadius, OceanTheme.BG_TRACK);

        // Fill: градиент #FF6B35 → #00D9FF
        float normalized = (max - min) > 0 ? (value - min) / (max - min) : 0;
        int filledWidth = (int) (sliderWidth * normalized);
        if (filledWidth > 0) {
            UIRenderHelper.drawGradientRectHorizontal(ms, x, trackY, filledWidth, TRACK_HEIGHT, OceanTheme.ACCENT, OceanTheme.ACCENT_SECONDARY);
        }

        // Thumb: 20px circle, белый с тенью (центр по normalized)
        int thumbCenterX = x + (int) ((sliderWidth - 2 * THUMB_RADIUS) * normalized) + THUMB_RADIUS;
        int thumbCenterY = trackY + TRACK_HEIGHT / 2;
        UIRenderHelper.drawCircle(ms, thumbCenterX + 1, thumbCenterY + 1, THUMB_RADIUS, 0x40000000);
        UIRenderHelper.drawCircle(ms, thumbCenterX, thumbCenterY, THUMB_RADIUS, com.topzurdo.mod.gui.theme.DesignTokens.fgPrimary());

        // Значение в badge: фон #FF6B35, rounded
        String valueText = showDecimals ? String.format("%.2f", value) : String.format("%.0f", value);
        int badgeW = tr.getWidth(valueText) + 12;
        int badgeH = 16;
        int badgeX = x + sliderWidth + 6;
        if (badgeX + badgeW > x + width - 4) {
            badgeX = x + width - badgeW - 4;
        }
        int badgeY = trackY - (badgeH - TRACK_HEIGHT) / 2;
        UIRenderHelper.fillRoundRect(ms, badgeX, badgeY, badgeW, badgeH, 6, OceanTheme.ACCENT);
        tr.draw(ms, valueText, badgeX + 6, badgeY + 4, com.topzurdo.mod.gui.theme.DesignTokens.fgPrimary());
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
            if (onChange != null) onChange.accept(value);
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

    // Overload for simpler call signature
    public boolean mouseDragged(double mouseX, int mouseY, int button) {
        if (dragging) {
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(int mouseX, int mouseY, double delta) {
        if (isMouseOver(mouseX, mouseY)) {
            float change = (float) delta * step;
            value = Math.max(min, Math.min(max, value + change));
            if (onChange != null) onChange.accept(value);
            return true;
        }
        return false;
    }

    private void updateValue(double mouseX) {
        int valueReserve = 52;
        int sliderWidth = Math.max(60, width - valueReserve);
        float normalized = (float) Math.max(0, Math.min(1, (mouseX - x - THUMB_RADIUS) / (sliderWidth - 2 * THUMB_RADIUS)));
        float newValue = min + normalized * (max - min);
        if (step > 0) {
            newValue = Math.round(newValue / step) * step;
        }
        value = Math.max(min, Math.min(max, newValue));
        long now = System.currentTimeMillis();
        if (onChange != null) {
            if (now - lastChangeTime >= 50) {
                onChange.accept(value);
                lastChangeTime = now;
            }
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void setValue(float value) { this.value = Math.max(min, Math.min(max, value)); }
    public float getValue() { return value; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setPartialTicks(float pt) { /* ignored */ }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
