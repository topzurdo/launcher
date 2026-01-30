package com.topzurdo.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * UI Rendering Helper - common drawing utilities
 */
public class UIRenderHelper {

    public static void drawRect(MatrixStack ms, int x, int y, int w, int h, int color) {
        DrawableHelper.fill(ms, x, y, x + w, y + h, color);
    }

    public static void drawRoundRect(MatrixStack ms, int x, int y, int w, int h, int radius, int color) {
        // Simple rounded rect (without actual rounding for simplicity)
        DrawableHelper.fill(ms, x + radius, y, x + w - radius, y + h, color);
        DrawableHelper.fill(ms, x, y + radius, x + w, y + h - radius, color);
        // Corners
        DrawableHelper.fill(ms, x, y, x + radius, y + radius, color);
        DrawableHelper.fill(ms, x + w - radius, y, x + w, y + radius, color);
        DrawableHelper.fill(ms, x, y + h - radius, x + radius, y + h, color);
        DrawableHelper.fill(ms, x + w - radius, y + h - radius, x + w, y + h, color);
    }

    public static void drawRoundBorder(MatrixStack ms, int x, int y, int w, int h, int radius, int thickness, int color) {
        // Top
        DrawableHelper.fill(ms, x + radius, y, x + w - radius, y + thickness, color);
        // Bottom
        DrawableHelper.fill(ms, x + radius, y + h - thickness, x + w - radius, y + h, color);
        // Left
        DrawableHelper.fill(ms, x, y + radius, x + thickness, y + h - radius, color);
        // Right
        DrawableHelper.fill(ms, x + w - thickness, y + radius, x + w, y + h - radius, color);
    }

    public static void drawGradientRect(MatrixStack ms, int x, int y, int w, int h, int colorTop, int colorBottom) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.getBuffer();
        Matrix4f matrix = ms.peek().getModel();

        float aT = (colorTop >> 24 & 255) / 255f;
        float rT = (colorTop >> 16 & 255) / 255f;
        float gT = (colorTop >> 8 & 255) / 255f;
        float bT = (colorTop & 255) / 255f;

        float aB = (colorBottom >> 24 & 255) / 255f;
        float rB = (colorBottom >> 16 & 255) / 255f;
        float gB = (colorBottom >> 8 & 255) / 255f;
        float bB = (colorBottom & 255) / 255f;

        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x + w, y, 0).color(rT, gT, bT, aT).next();
        buffer.vertex(matrix, x, y, 0).color(rT, gT, bT, aT).next();
        buffer.vertex(matrix, x, y + h, 0).color(rB, gB, bB, aB).next();
        buffer.vertex(matrix, x + w, y + h, 0).color(rB, gB, bB, aB).next();
        tess.draw();

        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    public static void drawShadow(MatrixStack ms, int x, int y, int w, int h, int shadowSize) {
        int color1 = 0x40000000;
        int color2 = 0x00000000;
        // Bottom shadow
        drawGradientRect(ms, x, y + h, w, shadowSize, color1, color2);
        // Right shadow
        drawGradientRectHorizontal(ms, x + w, y, shadowSize, h, color1, color2);
    }

    public static void drawGradientRectHorizontal(MatrixStack ms, int x, int y, int w, int h, int colorLeft, int colorRight) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.getBuffer();
        Matrix4f matrix = ms.peek().getModel();

        float aL = (colorLeft >> 24 & 255) / 255f;
        float rL = (colorLeft >> 16 & 255) / 255f;
        float gL = (colorLeft >> 8 & 255) / 255f;
        float bL = (colorLeft & 255) / 255f;

        float aR = (colorRight >> 24 & 255) / 255f;
        float rR = (colorRight >> 16 & 255) / 255f;
        float gR = (colorRight >> 8 & 255) / 255f;
        float bR = (colorRight & 255) / 255f;

        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x + w, y, 0).color(rR, gR, bR, aR).next();
        buffer.vertex(matrix, x, y, 0).color(rL, gL, bL, aL).next();
        buffer.vertex(matrix, x, y + h, 0).color(rL, gL, bL, aL).next();
        buffer.vertex(matrix, x + w, y + h, 0).color(rR, gR, bR, aR).next();
        tess.draw();

        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    public static int adjustAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, a2 = (c2 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF, r2 = (c2 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF, g2 = (c2 >> 8) & 0xFF;
        int b1 = c1 & 0xFF, b2 = c2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
