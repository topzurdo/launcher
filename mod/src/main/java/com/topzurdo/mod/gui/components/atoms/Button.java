package com.topzurdo.mod.gui.components.atoms;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Basic Button component
 */
public class Button {

    private int x, y, width, height;
    private String text;
    private Runnable onClick;
    private boolean hovered;
    private boolean pressed;

    public Button(int x, int y, int width, int height, String text, Runnable onClick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
        this.onClick = onClick;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        hovered = isMouseOver(mouseX, mouseY);
        
        int bgColor = pressed ? OceanTheme.ACCENT_DARK : (hovered ? OceanTheme.ACCENT : OceanTheme.BG_ELEVATED);
        int borderColor = hovered ? OceanTheme.ACCENT : OceanTheme.BORDER;

        UIRenderHelper.drawRoundRect(ms, x, y, width, height, 4, bgColor);
        UIRenderHelper.drawRoundBorder(ms, x, y, width, height, 4, 1, borderColor);

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int textWidth = tr.getWidth(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        tr.draw(ms, text, textX, textY, OceanTheme.TEXT_PRIMARY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            pressed = true;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && pressed) {
            pressed = false;
            if (isMouseOver(mouseX, mouseY) && onClick != null) {
                onClick.run();
            }
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setText(String text) { this.text = text; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
