package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;

/**
 * FullBright — повышенная яркость (гамма) для игры в темноте.
 * Управляет options.gamma при включении и восстанавливает при выключении.
 */
public class FullBrightModule extends Module {

    private Setting<Float> brightness;
    private double originalGamma = 1.0;

    public FullBrightModule() {
        super("fullbright", "FullBright", "Максимальная яркость в темноте", Category.RENDER);
        brightness = addSetting(Setting.ofFloat("brightness", "Яркость", "Уровень гаммы (1.0 = норма)", 1.0f, 0.5f, 2.0f));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null) {
            originalGamma = mc.options.gamma;
            applyGamma();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.gamma = originalGamma;
        }
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null && isEnabled()) {
            applyGamma();
        }
    }

    private void applyGamma() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        float mult = brightness != null ? brightness.getValue() : 1.0f;
        mc.options.gamma = Math.min(16.0, 1.0 * (10.0 + 6.0 * mult));
    }

    public float getBrightness() {
        return brightness != null ? brightness.getValue() : 1.0f;
    }
}
