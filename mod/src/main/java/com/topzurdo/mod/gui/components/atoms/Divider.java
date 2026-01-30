package com.topzurdo.mod.gui.components.atoms;

import com.topzurdo.mod.gui.OceanTheme;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Horizontal divider line component
 */
public class Divider {

    private int x, y, width;
    private int color;

    public Divider(int x, int y, int width) {
        this(x, y, width, OceanTheme.BORDER);
    }

    public Divider(int x, int y, int width, int color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.color = color;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        DrawableHelper.fill(ms, x, y, x + width, y + 1, color);
    }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setWidth(int width) { this.width = width; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return 1; }
}
