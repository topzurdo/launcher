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

    /**
     * Simple fill (alias for DrawableHelper.fill)
     */
    public static void fill(MatrixStack ms, int x1, int y1, int x2, int y2, int color) {
        DrawableHelper.fill(ms, x1, y1, x2, y2, color);
    }

    /**
     * Draw 1px border around a rectangle
     */
    public static void drawBorder1px(MatrixStack ms, int x, int y, int w, int h, int color) {
        DrawableHelper.fill(ms, x, y, x + w, y + 1, color);          // Top
        DrawableHelper.fill(ms, x, y + h - 1, x + w, y + h, color);  // Bottom
        DrawableHelper.fill(ms, x, y, x + 1, y + h, color);          // Left
        DrawableHelper.fill(ms, x + w - 1, y, x + w, y + h, color);  // Right
    }

    /**
     * Draw a simple circle (approximation)
     */
    public static void drawCircle(MatrixStack ms, int cx, int cy, int radius, int color) {
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                if (i * i + j * j <= radius * radius) {
                    DrawableHelper.fill(ms, cx + i, cy + j, cx + i + 1, cy + j + 1, color);
                }
            }
        }
    }

    /**
     * Set alpha on a color
     */
    public static int withAlpha(int color, float alpha) {
        int a = (int) (alpha * 255) & 0xFF;
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Filled rounded rect (simplified - just uses regular fill)
     */
    public static void fillRoundRect(MatrixStack ms, int x, int y, int w, int h, int radius, int color) {
        // Simple approximation
        DrawableHelper.fill(ms, x + radius, y, x + w - radius, y + h, color);
        DrawableHelper.fill(ms, x, y + radius, x + w, y + h - radius, color);
        // Corners approximation
        DrawableHelper.fill(ms, x + 1, y + 1, x + radius, y + radius, color);
        DrawableHelper.fill(ms, x + w - radius, y + 1, x + w - 1, y + radius, color);
        DrawableHelper.fill(ms, x + 1, y + h - radius, x + radius, y + h - 1, color);
        DrawableHelper.fill(ms, x + w - radius, y + h - radius, x + w - 1, y + h - 1, color);
    }

    /**
     * Blend two colors together
     */
    public static int blendColors(int c1, int c2, float ratio) {
        return lerpColor(c1, c2, ratio);
    }

    /**
     * Rounded border with single thickness (overload)
     */
    public static void drawRoundBorder(MatrixStack ms, int x, int y, int w, int h, int radius, int color) {
        drawRoundBorder(ms, x, y, w, h, radius, 1, color);
    }

    // Animation state
    private static float animationTick = 0f;
    private static float partialTicks = 0f;

    public static void tickAnimation() {
        animationTick += 0.05f;
        if (animationTick > 1000f) animationTick = 0f;
    }

    public static void setPartialTicks(float partial) {
        partialTicks = partial;
    }

    public static float getAnimationTick() {
        return animationTick;
    }

    /**
     * Vertical gradient fill
     */
    public static void fillVerticalGradient(MatrixStack ms, int x, int y, int w, int h, int colorTop, int colorBottom) {
        drawGradientRect(ms, x, y, w, h, colorTop, colorBottom);
    }

    /**
     * Draw floating particles effect
     */
    public static void drawFloatingParticles(MatrixStack ms, int x, int y, int w, int h, int color, int count) {
        java.util.Random rand = new java.util.Random((long)(animationTick * 10));
        for (int i = 0; i < count; i++) {
            float px = x + rand.nextFloat() * w;
            float py = y + rand.nextFloat() * h;
            float offset = (float) Math.sin(animationTick + i * 0.5f) * 10f;
            py += offset;
            py = py % h;
            if (py < 0) py += h;

            int alpha = (int) ((0.3f + 0.2f * Math.sin(animationTick + i)) * 255);
            alpha = Math.max(0, Math.min(255, alpha));
            int c = (alpha << 24) | (color & 0x00FFFFFF);
            DrawableHelper.fill(ms, (int)px, (int)py, (int)px + 2, (int)py + 2, c);
        }
    }

    /**
     * Draw scanlines effect
     */
    public static void drawScanlines(MatrixStack ms, int x, int y, int w, int h, int spacing, float alpha) {
        int color = withAlpha(0xFF000000, alpha);
        for (int sy = y; sy < y + h; sy += spacing) {
            DrawableHelper.fill(ms, x, sy, x + w, sy + 1, color);
        }
    }

    /**
     * Draw glowing border
     */
    public static void drawGlowingBorder(MatrixStack ms, int x, int y, int w, int h, int color, int glowSize) {
        // Inner border
        drawBorder1px(ms, x, y, w, h, color);
        // Outer glow layers
        for (int i = 1; i <= glowSize; i++) {
            float alpha = (1f - (float) i / glowSize) * 0.3f;
            int glowColor = withAlpha(color, alpha);
            drawBorder1px(ms, x - i, y - i, w + i * 2, h + i * 2, glowColor);
        }
    }

    /**
     * Draw corner accents
     */
    public static void drawCornerAccents(MatrixStack ms, int x, int y, int w, int h, int color, int size) {
        // Top-left
        DrawableHelper.fill(ms, x, y, x + size, y + 2, color);
        DrawableHelper.fill(ms, x, y, x + 2, y + size, color);
        // Top-right
        DrawableHelper.fill(ms, x + w - size, y, x + w, y + 2, color);
        DrawableHelper.fill(ms, x + w - 2, y, x + w, y + size, color);
        // Bottom-left
        DrawableHelper.fill(ms, x, y + h - 2, x + size, y + h, color);
        DrawableHelper.fill(ms, x, y + h - size, x + 2, y + h, color);
        // Bottom-right
        DrawableHelper.fill(ms, x + w - size, y + h - 2, x + w, y + h, color);
        DrawableHelper.fill(ms, x + w - 2, y + h - size, x + w, y + h, color);
    }

    /**
     * Draw vignette effect (darker corners)
     */
    public static void drawVignette(MatrixStack ms, int x, int y, int w, int h, float intensity) {
        int vignetteSize = Math.min(w, h) / 3;
        int baseAlpha = (int)(intensity * 255);

        // Four corners with radial gradient approximation
        for (int i = 0; i < vignetteSize; i++) {
            float t = 1f - (float)i / vignetteSize;
            int alpha = (int)(baseAlpha * t * t);
            int c = (alpha << 24);

            // Top-left
            DrawableHelper.fill(ms, x + i, y, x + i + 1, y + vignetteSize - i, c);
            DrawableHelper.fill(ms, x, y + i, x + vignetteSize - i, y + i + 1, c);

            // Top-right
            DrawableHelper.fill(ms, x + w - i - 1, y, x + w - i, y + vignetteSize - i, c);
            DrawableHelper.fill(ms, x + w - vignetteSize + i, y + i, x + w, y + i + 1, c);

            // Bottom-left
            DrawableHelper.fill(ms, x + i, y + h - vignetteSize + i, x + i + 1, y + h, c);
            DrawableHelper.fill(ms, x, y + h - i - 1, x + vignetteSize - i, y + h - i, c);

            // Bottom-right
            DrawableHelper.fill(ms, x + w - i - 1, y + h - vignetteSize + i, x + w - i, y + h, c);
            DrawableHelper.fill(ms, x + w - vignetteSize + i, y + h - i - 1, x + w, y + h - i, c);
        }
    }
}
