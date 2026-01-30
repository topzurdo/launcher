package com.topzurdo.mod.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 3D Particle with physics simulation.
 * Features: gravity, ground collision, fade out after landing.
 */
public class WorldParticle3D {

    public enum ParticleShape {
        HEART, STAR, CRIT, FLAME, SNOWFLAKE, NOTE, LIGHTNING, CIRCLE, TEXT
    }

    // Position and velocity
    public double x, y, z;
    public double prevX, prevY, prevZ;
    public double vx, vy, vz;

    // Lifecycle
    public float life;
    public float maxLife;
    public float alpha = 1.0f;
    public float scale = 1.0f;

    // Physics
    public float gravity = 0.04f;
    public float friction = 0.98f;
    public float bounceFactor = 0.3f;
    public boolean onGround = false;
    public boolean hasCollision = true;

    // Appearance
    public int color;
    public ParticleShape shape;
    public float rotation = 0f;
    public float rotationSpeed = 0f;

    // Ground fade
    public float groundFadeSpeed = 0.08f;
    private boolean startedFading = false;

    /**
     * Create a particle at position with velocity.
     */
    public WorldParticle3D(double x, double y, double z, double vx, double vy, double vz) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.life = 1.0f;
        this.maxLife = 1.0f;
        this.color = 0xFFFFFFFF;
        this.shape = ParticleShape.CIRCLE;
    }

    /**
     * Update particle physics. Call once per tick.
     * @return true if particle should be removed
     */
    public boolean tick() {
        if (life <= 0) return true;

        // Save previous pos for interpolation
        prevX = x;
        prevY = y;
        prevZ = z;

        // Apply gravity
        vy -= gravity;

        // Apply friction
        vx *= friction;
        vz *= friction;

        // Update position
        x += vx;
        y += vy;
        z += vz;

        // Rotation
        rotation += rotationSpeed;

        // Ground collision
        if (hasCollision) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world != null) {
                int groundY = findGroundLevel(mc.world, x, y, z);
                // Simple collision: if we fall below ground + offset
                if (y <= groundY + 0.1) {
                    y = groundY + 0.1;
                    if (vy < -0.05) {
                        vy = -vy * bounceFactor;
                        vx *= 0.8;
                        vz *= 0.8;
                    } else {
                        vy = 0;
                        onGround = true;
                    }
                } else {
                    onGround = false;
                }
            }
        }

        // Fade out on ground
        if (onGround && !startedFading) {
            startedFading = true;
        }

        if (startedFading) {
            life -= groundFadeSpeed;
            alpha = Math.max(0, life / maxLife);
            // Optional: shrink when fading
            // scale = scale * 0.95f;
        }

        return life <= 0;
    }

    /**
     * Find ground level at position.
     */
    private int findGroundLevel(World world, double px, double py, double pz) {
        BlockPos pos = new BlockPos((int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));

        // Search downward for solid block
        for (int dy = 0; dy < 10; dy++) {
            BlockPos checkPos = pos.down(dy);
            if (!world.getBlockState(checkPos).isAir()) {
                return checkPos.getY() + 1;
            }
        }

        // No ground found, use current y - 10
        return (int) py - 10;
    }

    public WorldParticle3D setColor(int argb) {
        this.color = argb;
        return this;
    }

    public WorldParticle3D setShape(ParticleShape shape) {
        this.shape = shape;
        return this;
    }

    public WorldParticle3D setGravity(float g) {
        this.gravity = g;
        return this;
    }

    public WorldParticle3D setScale(float s) {
        this.scale = s;
        return this;
    }

    public float getAlpha() {
        return alpha;
    }
}
