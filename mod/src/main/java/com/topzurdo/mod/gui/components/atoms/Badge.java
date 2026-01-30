package com.topzurdo.mod.gui.components.atoms;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Badge/pill component for status indicators
 */
public class Badge {

    private int x, y;
    private String text;
    private int bgColor;
    private int textColor;

    public Badge(int x, int y, String text, int bgColor, int textColor) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.bgColor = bgColor;
        this.textColor = textColor;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        int textWidth = tr.getWidth(text);
        int width = textWidth + 8;
        int height = 14;

        UIRenderHelper.drawRoundRect(ms, x, y, width, height, height / 2, bgColor);
        tr.draw(ms, text, x + 4, y + 3, textColor);
    }

    public void setText(String text) { this.text = text; }
    public void setColors(int bgColor, int textColor) {
        this.bgColor = bgColor;
        this.textColor = textColor;
    }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
}
