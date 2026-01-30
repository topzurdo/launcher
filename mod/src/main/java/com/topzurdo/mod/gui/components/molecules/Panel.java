package com.topzurdo.mod.gui.components.molecules;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel container component
 */
public class Panel {

    private int x, y, width, height;
    private int bgColor;
    private int borderColor;
    private int borderRadius;
    private boolean hasShadow;

    public Panel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bgColor = OceanTheme.BG_PANEL;
        this.borderColor = OceanTheme.BORDER;
        this.borderRadius = 8;
        this.hasShadow = true;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        if (hasShadow) {
            UIRenderHelper.drawShadow(ms, x, y, width, height, 4);
        }
        UIRenderHelper.drawRoundRect(ms, x, y, width, height, borderRadius, bgColor);
        UIRenderHelper.drawRoundBorder(ms, x, y, width, height, borderRadius, 1, borderColor);
    }

    public Panel setBgColor(int color) { this.bgColor = color; return this; }
    public Panel setBorderColor(int color) { this.borderColor = color; return this; }
    public Panel setBorderRadius(int radius) { this.borderRadius = radius; return this; }
    public Panel setShadow(boolean shadow) { this.hasShadow = shadow; return this; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
