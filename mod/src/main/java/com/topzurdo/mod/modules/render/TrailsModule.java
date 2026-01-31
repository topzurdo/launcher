package com.topzurdo.mod.modules.render;

import java.util.LinkedList;

import com.mojang.blaze3d.systems.RenderSystem;
import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

/**
 * Trails Module - 3D ribbon trail behind player.
 * Renders in world space using WorldRenderEvents.
 */
public class TrailsModule extends Module {

    private static class TrailPoint {
        final Vec3d pos;
        final long time;
        float alpha = 1f;

        TrailPoint(Vec3d pos) {
            this.pos = pos;
            this.time = System.currentTimeMillis();
        }
    }

    private final Setting<String> style;
    private final Setting<Integer> trailLength;
    private final Setting<Float> trailWidth;
    private final Setting<Integer> trailColor;

    private final LinkedList<TrailPoint> trail = new LinkedList<>();
    private Vec3d lastPos = null;
    private static final int MAX_POINTS = 60;
    private static final double MIN_DISTANCE = 0.05;

    public TrailsModule() {
        super("trails", "Trails", "3D след за игроком", Category.RENDER);

        style = addSetting(Setting.ofOptions("style", "Стиль", "Вид следа", "line",
                "line", "ribbon", "rainbow"));
        trailLength = addSetting(Setting.ofInt("length", "Длина", "Количество точек", 30, 10, 60));
        trailWidth = addSetting(Setting.ofFloat("width", "Ширина", "Толщина ленты", 0.15f, 0.05f, 0.5f));
        trailColor = addSetting(Setting.ofInt("color", "Цвет", "Цвет следа (RGB)", 0x22D3EE, 0x000000, 0xFFFFFF));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        trail.clear();
        lastPos = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        trail.clear();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Get player position at waist height
        Vec3d currentPos = mc.player.getPos().add(0, 0.8, 0);

        if (lastPos == null) {
            lastPos = currentPos;
            return;
        }

        // Add point if moved enough
        double dist = currentPos.distanceTo(lastPos);
        if (dist > MIN_DISTANCE) {
            trail.addFirst(new TrailPoint(currentPos));
            lastPos = currentPos;

            // Limit trail length
            int maxLen = trailLength.getValue();
            while (trail.size() > maxLen) {
                trail.removeLast();
            }
        }

        // Update alpha based on age
        long now = System.currentTimeMillis();
        for (int i = 0; i < trail.size(); i++) {
            TrailPoint p = trail.get(i);
            float ageAlpha = 1f - ((float) i / trail.size());
            float timeAlpha = Math.max(0, 1f - (now - p.time) / 2000f);
            p.alpha = Math.min(ageAlpha, timeAlpha);
        }

        // Remove dead points
        while (!trail.isEmpty() && trail.getLast().alpha <= 0) {
            trail.removeLast();
        }
    }

    /**
     * Called from WorldRenderEvents.END to render trail in 3D world.
     */
    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || trail.size() < 2) return;

        // Don't render in first person - interferes with PvP
        if (mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        String currentStyle = style.getValue();
        if (currentStyle == null) currentStyle = "line";
        int baseColor = trailColor.getValue();
        float width = trailWidth.getValue();
        // Толщина линии: для line — из настройки (масштаб 4–20 px), для ribbon — в рендере
        float linePx = Math.max(1f, Math.min(20f, width * 40f));
        RenderSystem.lineWidth(linePx);

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Matrix4f matrix = matrices.peek().getModel();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        if ("ribbon".equals(currentStyle) || "rainbow".equals(currentStyle)) {
            renderRibbon(buffer, tessellator, matrix, baseColor, width, "rainbow".equals(currentStyle));
        } else {
            renderLine(buffer, tessellator, matrix, baseColor);
        }

        matrices.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    private void renderLine(BufferBuilder buffer, Tessellator tessellator, Matrix4f matrix, int baseColor) {
        buffer.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < trail.size(); i++) {
            TrailPoint p = trail.get(i);
            int color = getColor(baseColor, i, p.alpha);

            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            float a = p.alpha * 0.8f;

            buffer.vertex(matrix, (float) p.pos.x, (float) p.pos.y, (float) p.pos.z)
                  .color(r, g, b, a)
                  .next();
        }

        tessellator.draw();
    }

    private void renderRibbon(BufferBuilder buffer, Tessellator tessellator, Matrix4f matrix,
                              int baseColor, float width, boolean rainbow) {
        if (trail.size() < 2) return;

        buffer.begin(GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < trail.size(); i++) {
            TrailPoint p = trail.get(i);

            // Calculate perpendicular direction for ribbon width
            Vec3d dir;
            if (i == 0 && trail.size() > 1) {
                dir = trail.get(1).pos.subtract(p.pos).normalize();
            } else if (i == trail.size() - 1) {
                dir = p.pos.subtract(trail.get(i - 1).pos).normalize();
            } else {
                dir = trail.get(i + 1).pos.subtract(trail.get(i - 1).pos).normalize();
            }

            // Perpendicular in XZ plane (horizontal ribbon)
            Vec3d perp = new Vec3d(-dir.z, 0, dir.x).normalize().multiply(width * (1f - (float)i / trail.size() * 0.5f));

            int color = rainbow ? hsvToRgb((float) i / trail.size(), 0.8f, 1f) : getColor(baseColor, i, p.alpha);

            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            float a = p.alpha * 0.7f;

            // Two vertices for ribbon
            buffer.vertex(matrix, (float)(p.pos.x + perp.x), (float) p.pos.y, (float)(p.pos.z + perp.z))
                  .color(r, g, b, a)
                  .next();
            buffer.vertex(matrix, (float)(p.pos.x - perp.x), (float) p.pos.y, (float)(p.pos.z - perp.z))
                  .color(r, g, b, a)
                  .next();
        }

        tessellator.draw();
    }

    private int getColor(int base, int index, float alpha) {
        // Slight color shift based on position
        float t = (float) index / Math.max(1, trail.size());
        int r = (base >> 16) & 0xFF;
        int g = (base >> 8) & 0xFF;
        int b = base & 0xFF;

        // Fade to darker
        r = (int)(r * (1f - t * 0.3f));
        g = (int)(g * (1f - t * 0.3f));
        b = (int)(b * (1f - t * 0.3f));

        return (r << 16) | (g << 8) | b;
    }

    private int hsvToRgb(float h, float s, float v) {
        int i = (int)(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }

        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    /**
     * Get trail points for external rendering if needed.
     */
    public LinkedList<TrailPoint> getTrailPoints() {
        return trail;
    }
}
