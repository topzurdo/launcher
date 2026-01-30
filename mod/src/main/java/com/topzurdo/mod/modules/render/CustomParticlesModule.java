package com.topzurdo.mod.modules.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import com.topzurdo.mod.modules.render.WorldParticle3D.ParticleShape;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

public class CustomParticlesModule extends Module {

    // Hit Settings
    private final Setting<String> particleType;
    private final Setting<Integer> particleColor;
    private final Setting<Integer> particleCount;
    private final Setting<Float> particleScale;
    private final Setting<Boolean> onlyOnCrit;

    // Ambient Settings
    private final Setting<Boolean> spawnInWorld;
    private final Setting<String> ambientParticleType;
    private final Setting<Float> ambientIntensity;
    private final Setting<Integer> ambientRadius;

    private final List<WorldParticle3D> particles = new ArrayList<>();
    private final Random random = new Random();
    private int ambientTicks;

    public CustomParticlesModule() {
        super("custom_particles", "Custom Particles", "Beautiful 3D particles with physics", Category.RENDER);

        // Hit - ULTRA MINIMAL defaults for PvP (won't distract at all)
        particleType = addSetting(Setting.ofOptions("particle_type", "Hit Shape", "Shape of hit particles", "Heart",
            "Heart", "Star", "Crit", "Flame", "Snowflake", "Note", "Lightning", "Circle"));
        particleColor = addSetting(Setting.ofColor("particle_color", "Hit Color", "Color of hit particles", 0xFFFF0000));
        particleCount = addSetting(Setting.ofInt("count", "Count", "Particles per hit (1-2 recommended for PvP)", 1, 1, 5));
        particleScale = addSetting(Setting.ofFloat("scale", "Scale", "Size of particles", 0.3f, 0.1f, 1.5f));
        onlyOnCrit = addSetting(Setting.ofBoolean("only_crit", "Only Crit", "Spawn only on critical hits", false));

        // Ambient
        spawnInWorld = addSetting(Setting.ofBoolean("spawn_in_world", "Ambient Spawn", "Enable falling ambient particles", true));
        ambientParticleType = addSetting(Setting.ofOptions("ambient_type", "Ambient Shape", "Shape of ambient particles", "Snowflake",
            "Heart", "Star", "Crit", "Flame", "Snowflake", "Note", "Lightning", "Circle"));
        ambientIntensity = addSetting(Setting.ofFloat("ambient_intensity", "Intensity", "Spawn rate", 0.5f, 0.1f, 2.0f));
        ambientRadius = addSetting(Setting.ofInt("ambient_radius", "Radius", "Spawn radius around player", 15, 5, 50));
    }

    public void onPlayerAttack(LivingEntity target) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isCrit = mc.player.fallDistance > 0.0F && !mc.player.isOnGround() && !mc.player.isSubmergedInWater();

        if (onlyOnCrit.getValue() && !isCrit) return;

        // Use exact count from settings, NO multiplier for crits (PvP friendly)
        int count = particleCount.getValue();

        Vec3d center = target.getPos().add(0, target.getHeight() / 2.0, 0);
        int color = particleColor.getValue();
        float scale = particleScale.getValue();
        ParticleShape shape = getShape(particleType.getValue());

        for (int i = 0; i < count; i++) {
            // Smaller, calmer explosion - less distracting in PvP
            double vx = (random.nextDouble() - 0.5) * 0.3;
            double vy = 0.1 + random.nextDouble() * 0.2;
            double vz = (random.nextDouble() - 0.5) * 0.3;

            particles.add(new WorldParticle3D(center.x, center.y, center.z, vx, vy, vz)
                .setShape(shape)
                .setColor(color)
                .setScale(scale));
        }
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;

        // Update physics
        Iterator<WorldParticle3D> it = particles.iterator();
        while (it.hasNext()) {
            if (it.next().tick()) it.remove();
        }

        // Spawn ambient
        if (spawnInWorld.getValue()) {
            spawnAmbient();
        }
    }

    private void spawnAmbient() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float intensity = ambientIntensity.getValue();
        if (intensity <= 0) return;

        ambientTicks++;
        int period = Math.max(1, (int)(10 / intensity));

        if (ambientTicks >= period) {
            ambientTicks = 0;

            int radius = ambientRadius.getValue();
            double x = mc.player.getX() + (random.nextDouble() - 0.5) * radius * 2;
            double y = mc.player.getY() + 5 + random.nextDouble() * 5;
            double z = mc.player.getZ() + (random.nextDouble() - 0.5) * radius * 2;

            ParticleShape shape = getShape(ambientParticleType.getValue());
            float scale = particleScale.getValue(); // Use same scale setting or add separate? Let's use same for now or default.
            // Actually ambient particles usually look better small.

            particles.add(new WorldParticle3D(x, y, z, 0, -0.05 - random.nextDouble() * 0.05, 0)
                .setShape(shape)
                .setColor(0xFFFFFFFF) // White for ambient usually
                .setScale(scale)
                .setGravity(0.005f)); // Low gravity for floating feel
        }
    }

    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!isEnabled() || particles.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        // Prepare rendering
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

        for (WorldParticle3D p : particles) {
            matrices.push();

            // Interpolate position
            double ix = p.prevX + (p.x - p.prevX) * tickDelta;
            double iy = p.prevY + (p.y - p.prevY) * tickDelta;
            double iz = p.prevZ + (p.z - p.prevZ) * tickDelta;

            matrices.translate(ix, iy, iz);

            // Billboard rotation (face camera)
            matrices.multiply(camera.getRotation());

            // Scale (inverted Y because text renders down) - ULTRA small for PvP
            float s = 0.012f * p.scale * (0.2f + 0.8f * p.getAlpha());
            matrices.scale(-s, -s, s);

            String text = getSymbol(p.shape);
            int color = (p.color & 0x00FFFFFF) | ((int)(p.alpha * 255) << 24);

            // Center text
            float width = mc.textRenderer.getWidth(text);
            float height = mc.textRenderer.fontHeight;

            mc.textRenderer.draw(text,
                -width / 2f,
                -height / 2f,
                color,
                false, // Shadow
                matrices.peek().getModel(),
                consumers,
                false, // SeeThrough
                0,
                0xF000F0); // Light

            matrices.pop();
        }

        consumers.draw();
        matrices.pop();
    }

    private ParticleShape getShape(String name) {
        if (name == null) return ParticleShape.HEART;
        switch (name.toUpperCase()) {
            case "HEART": return ParticleShape.HEART;
            case "STAR": return ParticleShape.STAR;
            case "CRIT": return ParticleShape.CRIT;
            case "FLAME": return ParticleShape.FLAME;
            case "SNOWFLAKE": return ParticleShape.SNOWFLAKE;
            case "NOTE": return ParticleShape.NOTE;
            case "LIGHTNING": return ParticleShape.LIGHTNING;
            case "CIRCLE": return ParticleShape.CIRCLE;
            default: return ParticleShape.HEART;
        }
    }

    private String getSymbol(ParticleShape shape) {
        switch (shape) {
            case HEART: return "❤";
            case STAR: return "★";
            case CRIT: return "✷";
            case FLAME: return "🔥"; // Standard font might miss this, fallback "§6x"? Let's try Unicode.
            case SNOWFLAKE: return "❄";
            case NOTE: return "♪";
            case LIGHTNING: return "⚡";
            case CIRCLE: return "●";
            default: return "•";
        }
    }
}
