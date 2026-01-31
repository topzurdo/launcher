package com.topzurdo.mod.modules.render;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
 * Uses interpolation and smoothed normals for a fluid, non-blocky trail.
 */
public class TrailsModule extends Module {

    /** One point: 4 positions from feet (0) to head (3) for full character height. */
    private static class TrailPoint {
        final Vec3d[] pos;
        final long time;
        float alpha = 1f;

        TrailPoint(Vec3d feet, Vec3d knee, Vec3d waist, Vec3d head) {
            this.pos = new Vec3d[] { feet, knee, waist, head };
            this.time = System.currentTimeMillis();
        }
    }

    private final Setting<String> style;
    private final Setting<Integer> trailLength;
    private final Setting<Float> trailWidth;
    private final Setting<Integer> trailColor;

    private final LinkedList<TrailPoint> trail = new LinkedList<>();
    private Vec3d lastWaistPos = null;
    private static final double[] HEIGHT_OFFSETS = { 0, 0.4, 0.8, 1.6 };
    private static final int MAX_POINTS = 60;
    /** Smaller distance = more points = smoother trail. */
    private static final double MIN_DISTANCE = 0.02;
    /** Subdivisions per segment for smooth rendering (no blocky segments). */
    private static final int SMOOTH_SUBDIVISIONS = 4;

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
        lastWaistPos = null;
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

        Vec3d base = mc.player.getPos();
        Vec3d waist = base.add(0, 0.8, 0);

        if (lastWaistPos == null) {
            lastWaistPos = waist;
            return;
        }

        double dist = waist.distanceTo(lastWaistPos);
        if (dist > MIN_DISTANCE) {
            Vec3d feet = base.add(0, 0, 0);
            Vec3d knee = base.add(0, HEIGHT_OFFSETS[1], 0);
            Vec3d waistPt = base.add(0, HEIGHT_OFFSETS[2], 0);
            Vec3d head = base.add(0, HEIGHT_OFFSETS[3], 0);
            trail.addFirst(new TrailPoint(feet, knee, waistPt, head));
            lastWaistPos = waist;

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
        // Depth test ON so trail is occluded by player when viewed from front (trail stays behind)

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

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    /** Build smoothed points for one height level (0=feet .. 3=head). */
    private void buildSmoothedPointsAtLevel(int level, List<Vec3d> outPos, List<Float> outAlpha) {
        outPos.clear();
        outAlpha.clear();
        if (trail.isEmpty() || level < 0 || level >= 4) return;
        int n = trail.size();
        for (int i = 0; i < n; i++) {
            TrailPoint p = trail.get(i);
            Vec3d pos = p.pos[level];
            if (i + 1 < n) {
                TrailPoint next = trail.get(i + 1);
                Vec3d nextPos = next.pos[level];
                for (int k = 0; k < SMOOTH_SUBDIVISIONS; k++) {
                    float t = (float) k / SMOOTH_SUBDIVISIONS;
                    float t1 = 1f - t;
                    outPos.add(new Vec3d(
                        pos.x * t1 + nextPos.x * t,
                        pos.y * t1 + nextPos.y * t,
                        pos.z * t1 + nextPos.z * t
                    ));
                    outAlpha.add(p.alpha * t1 + next.alpha * t);
                }
            } else {
                outPos.add(pos);
                outAlpha.add(p.alpha);
            }
        }
    }

    /** Smoothed direction at index i (averaged with neighbors to reduce zigzag). */
    private Vec3d smoothedDirection(List<Vec3d> pos, int i, int radius) {
        int lo = Math.max(0, i - radius);
        int hi = Math.min(pos.size() - 1, i + radius);
        if (lo >= hi) return Vec3d.ZERO;
        Vec3d d = pos.get(hi).subtract(pos.get(lo));
        return d.lengthSquared() > 1e-12 ? d.normalize() : new Vec3d(1, 0, 0);
    }

    private void renderLine(BufferBuilder buffer, Tessellator tessellator, Matrix4f matrix, int baseColor) {
        List<Vec3d> pos = new ArrayList<>();
        List<Float> alphas = new ArrayList<>();
        for (int level = 0; level < 4; level++) {
            buildSmoothedPointsAtLevel(level, pos, alphas);
            if (pos.size() < 2) continue;
            buffer.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < pos.size(); i++) {
                Vec3d p = pos.get(i);
                float alpha = alphas.get(i);
                float t = (float) i / Math.max(1, pos.size());
                int color = getColor(baseColor, t, alpha);
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;
                float a = alpha * 0.85f;
                buffer.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).next();
            }
            tessellator.draw();
        }
    }

    private void renderRibbon(BufferBuilder buffer, Tessellator tessellator, Matrix4f matrix,
                              int baseColor, float width, boolean rainbow) {
        List<Vec3d>[] posByLevel = new List[] { new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>() };
        List<Float>[] alphasByLevel = new List[] { new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>() };
        int minSize = Integer.MAX_VALUE;
        for (int level = 0; level < 4; level++) {
            buildSmoothedPointsAtLevel(level, posByLevel[level], alphasByLevel[level]);
            minSize = Math.min(minSize, posByLevel[level].size());
        }
        if (minSize < 2) return;

        // Draw 3 bands: feet-knee, knee-waist, waist-head (full character height)
        for (int band = 0; band < 3; band++) {
            int l0 = band, l1 = band + 1;
            List<Vec3d> pos0 = posByLevel[l0], pos1 = posByLevel[l1];
            List<Float> alphas0 = alphasByLevel[l0], alphas1 = alphasByLevel[l1];
            buffer.begin(GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < minSize; i++) {
                Vec3d p0 = pos0.get(i), p1 = pos1.get(i);
                float alpha = (alphas0.get(i) + alphas1.get(i)) * 0.5f;
                float t = (float) i / Math.max(1, minSize);
                int color = rainbow ? hsvToRgb(t, 0.85f, 1f) : getColor(baseColor, t, alpha);
                float r = ((color >> 16) & 0xFF) / 255f, g = ((color >> 8) & 0xFF) / 255f, b = (color & 0xFF) / 255f, a = alpha * 0.75f;
                buffer.vertex(matrix, (float) p0.x, (float) p0.y, (float) p0.z).color(r, g, b, a).next();
                buffer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, a).next();
            }
            tessellator.draw();
        }
    }

    private int getColor(int base, float t, float alpha) {
        int r = (base >> 16) & 0xFF;
        int g = (base >> 8) & 0xFF;
        int b = base & 0xFF;
        r = (int)(r * (1f - t * 0.35f));
        g = (int)(g * (1f - t * 0.35f));
        b = (int)(b * (1f - t * 0.35f));
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
