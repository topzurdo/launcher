package com.topzurdo.mod.gui.components.atoms;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Consumer;

/**
 * Toggle switch component
 */
public class Toggle implements com.topzurdo.mod.gui.UIComponent {

    private int x, y, width, height;
    private static final int DEFAULT_WIDTH = 36;
    private static final int DEFAULT_HEIGHT = 18;

    private String label;
    private boolean value;
    private Consumer<Boolean> onChange;
    private float animProgress = 0f;

    public Toggle(int x, int y, boolean value, Consumer<Boolean> onChange) {
        this(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, null, value, onChange);
    }

    public Toggle(int x, int y, int width, int height, String label, Boolean value, Consumer<Boolean> onChange) {
        this.x = x;
        this.y = y;
        this.width = width > 0 ? width : DEFAULT_WIDTH;
        this.height = height > 0 ? height : DEFAULT_HEIGHT;
        this.label = label;
        this.value = value != null ? value : false;
        this.onChange = onChange;
        this.animProgress = this.value ? 1f : 0f;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        float target = value ? 1f : 0f;
        animProgress += (target - animProgress) * (1f - (float) Math.pow(0.75f, Math.min(delta * 60f, 2f)));

        int toggleW = DEFAULT_WIDTH;
        int toggleH = DEFAULT_HEIGHT;
        int toggleX = x + width - toggleW;
        int toggleY = y + (height - toggleH) / 2;
        boolean hover = isMouseOver(mouseX, mouseY);
        int radius = toggleH / 2;

        // Трек: неактивен #334155; активен — градиент #FF6B35 → #FF8F35 (iOS-style), плавное заполнение
        int trackBg = OceanTheme.BG_TRACK;
        if (hover) trackBg = UIRenderHelper.lerpColor(trackBg, OceanTheme.ACCENT, 0.3f);
        UIRenderHelper.fillRoundRect(ms, toggleX, toggleY, toggleW, toggleH, radius, trackBg);
        if (animProgress > 0.01f) {
            int fillW = Math.max(0, (int) (toggleW * animProgress));
            if (fillW > 0) {
                UIRenderHelper.drawGradientRectHorizontal(ms, toggleX, toggleY, fillW, toggleH, OceanTheme.ACCENT, OceanTheme.ACCENT_GRADIENT_END);
            }
        }
        if (hover && value) {
            UIRenderHelper.drawRoundBorder(ms, toggleX, toggleY, toggleW, toggleH, radius, 1, UIRenderHelper.withAlpha(com.topzurdo.mod.gui.theme.DesignTokens.fgPrimary(), 0.25f));
        }

        // Knob: белый круг с лёгкой тенью
        int knobSize = toggleH - 4;
        int knobRadius = knobSize / 2;
        int knobX = toggleX + 2 + (int) ((toggleW - knobSize - 4) * animProgress);
        int knobCenterX = knobX + knobRadius;
        int knobCenterY = toggleY + toggleH / 2;
        UIRenderHelper.drawCircle(ms, knobCenterX + 1, knobCenterY + 1, knobRadius, 0x30000000);
        UIRenderHelper.drawCircle(ms, knobCenterX, knobCenterY, knobRadius, com.topzurdo.mod.gui.theme.DesignTokens.fgPrimary());

        // Label
        if (label != null && !label.isEmpty()) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            mc.textRenderer.draw(ms, label, x + 5, y + (height - 8) / 2, com.topzurdo.mod.gui.theme.DesignTokens.fgPrimary());
        }
    }

    public void tick() {
        // Animation is handled in render
    }

    public void onMouseClick() {
        value = !value;
        if (onChange != null) onChange.accept(value);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            onMouseClick();
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void setValue(boolean value) { this.value = value; }
    public boolean getValue() { return value; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setPartialTicks(float pt) { /* ignored */ }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
