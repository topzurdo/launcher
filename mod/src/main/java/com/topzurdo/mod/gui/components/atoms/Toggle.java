package com.topzurdo.mod.gui.components.atoms;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Consumer;

/**
 * Toggle switch component
 */
public class Toggle {

    private int x, y;
    private static final int WIDTH = 36;
    private static final int HEIGHT = 18;

    private boolean value;
    private Consumer<Boolean> onChange;
    private float animProgress = 0f;

    public Toggle(int x, int y, boolean value, Consumer<Boolean> onChange) {
        this.x = x;
        this.y = y;
        this.value = value;
        this.onChange = onChange;
        this.animProgress = value ? 1f : 0f;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        // Animate
        float target = value ? 1f : 0f;
        animProgress += (target - animProgress) * 0.3f;

        int bgColor = UIRenderHelper.lerpColor(OceanTheme.BG_ELEVATED, OceanTheme.ACCENT, animProgress);
        UIRenderHelper.drawRoundRect(ms, x, y, WIDTH, HEIGHT, HEIGHT / 2, bgColor);

        // Knob
        int knobSize = HEIGHT - 4;
        int knobX = x + 2 + (int) ((WIDTH - knobSize - 4) * animProgress);
        UIRenderHelper.drawRoundRect(ms, knobX, y + 2, knobSize, knobSize, knobSize / 2, OceanTheme.TEXT_PRIMARY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            value = !value;
            if (onChange != null) onChange.accept(value);
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public void setValue(boolean value) { this.value = value; }
    public boolean getValue() { return value; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }
}
