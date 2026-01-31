package com.topzurdo.mod.gui.components.atoms;

import com.topzurdo.mod.gui.OceanTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Text label component
 */
public class Label {

    private int x, y;
    private String text;
    private int color;
    private float scale;

    public Label(int x, int y, String text) {
        this(x, y, text, com.topzurdo.mod.gui.theme.DesignTokens.fgPrimary(), 1.0f);
    }

    public Label(int x, int y, String text, int color, float scale) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.scale = scale;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        if (scale != 1.0f) {
            ms.push();
            ms.scale(scale, scale, 1f);
            tr.draw(ms, text, x / scale, y / scale, color);
            ms.pop();
        } else {
            tr.draw(ms, text, x, y, color);
        }
    }

    public void setText(String text) { this.text = text; }
    public String getText() { return text; }
    public void setColor(int color) { this.color = color; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
}
