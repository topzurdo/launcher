package com.topzurdo.mod.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;

import org.lwjgl.opengl.GL11;

/**
 * China Hat - 3D cone rendered above player's head.
 * Fixed: Tilts with head to avoid view obstruction.
 */
public class ChinaHatModule extends Module {

    private final Setting<Integer> hatColor;
    private final Setting<Float> hatSize;
    private final Setting<Float> hatHeight;
    private final Setting<Integer> segments;
    private final Setting<Boolean> rainbow;
    private final Setting<Boolean> outline;

    private float rainbowHue = 0f;

    public ChinaHatModule() {
        super("china_hat", "China Hat", "3D конус-шляпа над головой", Category.RENDER);

        hatColor = addSetting(Setting.ofColor("color", "Цвет", "Цвет шляпы", 0xD4A574));
        hatSize = addSetting(Setting.ofFloat("size", "Размер", "Радиус основания", 0.7f, 0.3f, 1.5f));
        hatHeight = addSetting(Setting.ofFloat("height", "Высота", "Высота конуса", 0.4f, 0.2f, 0.8f));
        segments = addSetting(Setting.ofInt("segments", "Сегменты", "Количество граней", 16, 6, 32));
        rainbow = addSetting(Setting.ofBoolean("rainbow", "Rainbow", "Радужный цвет", false));
        outline = addSetting(Setting.ofBoolean("outline", "Обводка", "Рисовать контур", true));
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;

        if (rainbow.getValue()) {
            rainbowHue += 0.01f;
            if (rainbowHue > 1f) rainbowHue = 0f;
        }
    }

    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Hide in first person
        if (mc.options.getPerspective().isFirstPerson()) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        renderHatForPlayer(matrices, mc.player, camPos, tickDelta);
    }

    private void renderHatForPlayer(MatrixStack matrices, PlayerEntity player, Vec3d camPos, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        // Interpolate rotations
        float yaw = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw); // Use head yaw
        float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.pitch);

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        matrices.push();
        // Move to head center
        matrices.translate(x - camPos.x, y + player.getEyeHeight(player.getPose()) - camPos.y, z - camPos.z);

        // Apply rotations (Yaw then Pitch)
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-yaw));
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(pitch));

        // Move up to top of head
        matrices.translate(0, 0.35, 0); // Offset above eye level

        // Correct for sneaking
        if (player.isInSneakingPose()) {
            matrices.translate(0, 0.05, 0); // Sneaking height adjustment
        }

        Matrix4f matrix = matrices.peek().getModel();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        float radius = hatSize.getValue();
        float height = hatHeight.getValue();
        int segs = segments.getValue();

        int color = rainbow.getValue() ? hsvToRgb(rainbowHue, 0.7f, 0.9f) : hatColor.getValue();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.85f;

        // Draw cone
        buffer.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, 0, height, 0).color(r * 1.2f, g * 1.2f, b * 1.2f, a).next();

        for (int i = 0; i <= segs; i++) {
            float angle = (float)(i * Math.PI * 2 / segs);
            float px = (float)(Math.cos(angle) * radius);
            float pz = (float)(Math.sin(angle) * radius);
            float shade = 0.7f + 0.3f * (float)Math.cos(angle);
            buffer.vertex(matrix, px, 0, pz).color(r * shade, g * shade, b * shade, a).next();
        }
        tessellator.draw();

        // Base
        buffer.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, 0, 0, 0).color(r * 0.6f, g * 0.6f, b * 0.6f, a).next();
        for (int i = segs; i >= 0; i--) {
            float angle = (float)(i * Math.PI * 2 / segs);
            float px = (float)(Math.cos(angle) * radius);
            float pz = (float)(Math.sin(angle) * radius);
            buffer.vertex(matrix, px, 0, pz).color(r * 0.5f, g * 0.5f, b * 0.5f, a).next();
        }
        tessellator.draw();

        // Outline
        if (outline.getValue()) {
            RenderSystem.lineWidth(2f);
            int outlineColor = darkenColor(color, 0.4f);
            float or = ((outlineColor >> 16) & 0xFF) / 255f;
            float og = ((outlineColor >> 8) & 0xFF) / 255f;
            float ob = (outlineColor & 0xFF) / 255f;

            buffer.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < segs; i++) {
                float angle = (float)(i * Math.PI * 2 / segs);
                float px = (float)(Math.cos(angle) * radius);
                float pz = (float)(Math.sin(angle) * radius);
                buffer.vertex(matrix, px, 0, pz).color(or, og, ob, 1f).next();
            }
            tessellator.draw();

            buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < segs; i += segs / 4) {
                float angle = (float)(i * Math.PI * 2 / segs);
                float px = (float)(Math.cos(angle) * radius);
                float pz = (float)(Math.sin(angle) * radius);
                buffer.vertex(matrix, 0, height, 0).color(or, og, ob, 1f).next();
                buffer.vertex(matrix, px, 0, pz).color(or, og, ob, 1f).next();
            }
            tessellator.draw();
        }

        matrices.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    private int darkenColor(int color, float factor) {
        int r = (int)(((color >> 16) & 0xFF) * factor);
        int g = (int)(((color >> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
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
}
