package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Damage Overlay Module - screen effect on damage
 */
public class DamageOverlayModule extends Module {

    private Setting<Integer> color;
    private Setting<Float> intensity;
    private Setting<Float> duration;

    private long lastDamageTime = 0;
    private float lastHealth = 20f;

    public DamageOverlayModule() {
        super("damage_overlay", "Damage Overlay", "Эффект при получении урона", Category.RENDER);

        color = addSetting(Setting.ofInt("color", "Цвет", "Цвет оверлея", 0xFF0000, 0, 0xFFFFFF));
        intensity = addSetting(Setting.ofFloat("intensity", "Интенсивность", "Яркость эффекта", 0.3f, 0.1f, 1.0f));
        duration = addSetting(Setting.ofFloat("duration", "Длительность", "Время эффекта (сек)", 0.5f, 0.1f, 2.0f));
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float health = mc.player.getHealth();
        if (health < lastHealth) {
            lastDamageTime = System.currentTimeMillis();
        }
        lastHealth = health;
    }

    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getWindow() == null) return;

        long now = System.currentTimeMillis();
        long durationMs = (long) (duration.getValue() * 1000);
        if (now - lastDamageTime > durationMs) return;

        float alpha = (1f - (float)(now - lastDamageTime) / durationMs) * intensity.getValue();
        int c = color.getValue();
        int a = (int) (alpha * 255);
        int col = (a << 24) | c;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        MatrixStack ms = new MatrixStack();
        DrawableHelper.fill(ms, 0, 0, w, h, col);
    }
}
