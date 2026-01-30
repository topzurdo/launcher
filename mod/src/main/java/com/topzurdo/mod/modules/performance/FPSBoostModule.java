package com.topzurdo.mod.modules.performance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.topzurdo.mod.ModConstants;
import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.render.CustomParticlesModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.AoMode;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * FPS Boost — единый модуль оптимизации.
 * Без настроек: при включении автоматически применяет оптимальные параметры.
 */
@SuppressWarnings("rawtypes")
public class FPSBoostModule extends Module {

    @SuppressWarnings("unchecked")
    private static final Set AMBIENT_PARTICLE_TYPES = new HashSet<>(Arrays.asList(
        ParticleTypes.PORTAL, ParticleTypes.DRAGON_BREATH,
        ParticleTypes.CAMPFIRE_COSY_SMOKE, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
        ParticleTypes.CLOUD, ParticleTypes.LARGE_SMOKE, ParticleTypes.SMOKE,
        ParticleTypes.ASH, ParticleTypes.SQUID_INK, ParticleTypes.FALLING_DUST,
        ParticleTypes.WITCH, ParticleTypes.END_ROD
    ));

    @SuppressWarnings("unchecked")
    private static final Set CUSTOM_PARTICLE_TYPES = new HashSet<>(Arrays.asList(
        ParticleTypes.HEART, ParticleTypes.END_ROD, ParticleTypes.CRIT,
        ParticleTypes.ENCHANT, ParticleTypes.FLAME, ParticleTypes.ITEM_SNOWBALL
    ));

    private static volatile int particlesThisTick = 0;
    private static long lastResetTick = -1;

    private static final int MAX_PARTICLES = 1000;
    private static final int CULL_DISTANCE = 32;
    private static final int MAX_ENTITY_DISTANCE = 64;

    private GraphicsMode originalGraphics;
    private ParticlesMode originalParticles;
    private CloudRenderMode originalClouds;
    private Integer originalMipmap;
    private AoMode originalAmbientOcclusion;
    private Integer originalRenderDistance;

    private final Map<Entity, CullResult> cullCache = new WeakHashMap<>();
    private long lastCacheClearTick = 0;

    private static class CullResult {
        final boolean shouldCull;
        final long tick;
        CullResult(boolean shouldCull, long tick) { this.shouldCull = shouldCull; this.tick = tick; }
    }

    public FPSBoostModule() {
        super("fps_boost", "FPS Boost", "Оптимизация FPS (авто)", Category.PERFORMANCE);
    }

    public static boolean isCustomParticlesEnabled() {
        TopZurdoMod mod = TopZurdoMod.getInstance();
        if (mod == null || mod.getModuleManager() == null) return false;
        Module m = mod.getModuleManager().getModule("custom_particles");
        return m != null && m.isEnabled();
    }

    public static boolean isCustomParticlesType(ParticleEffect d) {
        return CUSTOM_PARTICLE_TYPES.contains(d.getType());
    }

    public static boolean shouldExemptFromLimit(ParticleEffect d) {
        return isCustomParticlesEnabled() && isCustomParticlesType(d);
    }

    public static void tickParticleCount() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            long t = mc.world.getTime();
            if (t != lastResetTick) {
                particlesThisTick = 0;
                lastResetTick = t;
            }
        }
    }

    public static int getParticleCountThisTick() { return particlesThisTick; }
    public static void incrementParticleCount() { particlesThisTick++; }

    public boolean isLimitParticlesEnabled() { return isEnabled(); }
    public int getMaxParticles() { return isEnabled() ? MAX_PARTICLES : Integer.MAX_VALUE; }
    public int getCullDistance() { return CULL_DISTANCE; }

    public boolean shouldSpawnParticle(ParticleEffect data, double x, double y, double z) {
        if (!isLimitParticlesEnabled()) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return true;

        if (isCustomParticlesEnabled() && isCustomParticlesType(data)) return true;

        double dx = x - mc.player.getX();
        double dy = y - mc.player.getY();
        double dz = z - mc.player.getZ();
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) > CULL_DISTANCE) return false;

        if (AMBIENT_PARTICLE_TYPES.contains(data.getType())) return Math.random() > 0.3;
        if (data.getType() == ParticleTypes.EXPLOSION || data.getType() == ParticleTypes.EXPLOSION_EMITTER)
            return Math.random() > 0.5;
        if (data.getType() == ParticleTypes.ENCHANT) return Math.random() > 0.7;
        return true;
    }

    public boolean isEntityCullingEnabled() { return isEnabled(); }

    public boolean shouldCullEntity(Entity entity) {
        if (!isEntityCullingEnabled()) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getCameraEntity() == null) return false;
        if (entity == mc.player) return false;

        long tick = mc.world != null ? mc.world.getTime() : 0;
        if (tick != lastCacheClearTick) {
            cullCache.clear();
            lastCacheClearTick = tick;
        }
        CullResult cached = cullCache.get(entity);
        if (cached != null && cached.tick == tick) return cached.shouldCull;

        Entity cam = mc.getCameraEntity();
        Vec3d camPos = cam.getCameraPosVec(1f);
        Vec3d entPos = entity.getPos();
        double dist = camPos.distanceTo(entPos);

        boolean cull = false;
        if (dist > MAX_ENTITY_DISTANCE) cull = true;
        else {
            Box bb = entity.getBoundingBox();
            double size = Math.max(bb.getXLength(), Math.max(bb.getYLength(), bb.getZLength()));
            if (size < 1 && dist > ModConstants.Performance.SMALL_ENTITY_DISTANCE) {
                if ((entity instanceof ItemFrameEntity) && dist > ModConstants.Performance.ITEM_FRAME_PAINTING_CULL_DISTANCE)
                    cull = true;
            }
            if (!cull) {
                double dot = cam.getRotationVec(1f).dotProduct(entPos.subtract(camPos).normalize());
                if (dot < ModConstants.Performance.BEHIND_CULL_THRESHOLD && dist > ModConstants.Performance.BEHIND_CULL_MIN_DISTANCE)
                    cull = true;
            }
        }
        cullCache.put(entity, new CullResult(cull, tick));
        return cull;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            originalGraphics = mc.options.graphicsMode;
            originalParticles = mc.options.particles;
            originalClouds = mc.options.cloudRenderMode;
            originalMipmap = mc.options.mipmapLevels;
            originalAmbientOcclusion = mc.options.ao;
            originalRenderDistance = mc.options.viewDistance;
            apply();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;
        if (originalGraphics != null) mc.options.graphicsMode = originalGraphics;
        if (originalParticles != null) mc.options.particles = originalParticles;
        if (originalClouds != null) mc.options.cloudRenderMode = originalClouds;
        if (originalMipmap != null) mc.options.mipmapLevels = originalMipmap;
        if (originalAmbientOcclusion != null) mc.options.ao = originalAmbientOcclusion;
        if (originalRenderDistance != null) mc.options.viewDistance = originalRenderDistance;
    }

    private void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;
        boolean noCustomParts = !isCustomParticlesEnabled();

        if (mc.options.viewDistance > 8) mc.options.viewDistance = 8;
        if (noCustomParts) mc.options.particles = ParticlesMode.DECREASED;
        mc.options.cloudRenderMode = CloudRenderMode.OFF;
        if (mc.options.mipmapLevels > 2) mc.options.mipmapLevels = 2;
        mc.options.ao = AoMode.OFF;
    }
}
