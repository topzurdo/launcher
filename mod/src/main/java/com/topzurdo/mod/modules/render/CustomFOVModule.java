package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Custom FOV Module - field of view customization
 */
public class CustomFOVModule extends Module {

    private Setting<Integer> baseFov;
    private Setting<Boolean> staticFov;
    private Setting<Boolean> disableSprintFov;
    private Setting<Boolean> disableBowFov;
    private Setting<Boolean> fovCircle;
    private Setting<Float> vignetteRadius;
    private Setting<Float> vignetteIntensity;

    public CustomFOVModule() {
        super("custom_fov", "Custom FOV", "Настройка поля зрения", Category.RENDER);

        baseFov = addSetting(Setting.ofInt("base_fov", "Базовый FOV", "Базовое поле зрения", 90, 30, 140));
        staticFov = addSetting(Setting.ofBoolean("static_fov", "Статичный FOV", "FOV не меняется при эффектах", true));
        disableSprintFov = addSetting(Setting.ofBoolean("disable_sprint_fov", "Без FOV спринта", "FOV не увеличивается при беге", true));
        disableBowFov = addSetting(Setting.ofBoolean("disable_bow_fov", "Без FOV лука", "FOV не уменьшается при натяжении лука", false));
        fovCircle = addSetting(Setting.ofBoolean("fov_circle", "Виньетка", "Затемнение по краям экрана", false));
        vignetteRadius = addSetting(Setting.ofFloat("vignette_radius", "Радиус виньетки", "Ширина затемнения", 0.85f, 0.5f, 1.0f));
        vignetteIntensity = addSetting(Setting.ofFloat("vignette_intensity", "Интенсивность виньетки", "Яркость затемнения", 0.5f, 0.0f, 1.0f));
    }

    public int getBaseFov() { return baseFov.getValue(); }
    public boolean isStaticFov() { return staticFov.getValue(); }
    public boolean shouldDisableSprintFov() { return disableSprintFov.getValue(); }
    public boolean shouldDisableBowFov() { return disableBowFov.getValue(); }

    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled() || !fovCircle.getValue()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null || mc.player == null) return;

        float intensity = vignetteIntensity.getValue();
        if (intensity <= 0f) return;

        float r = vignetteRadius.getValue();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int stripW = (int) ((1f - r) * w / 2f);
        int stripH = (int) ((1f - r) * h / 2f);
        if (stripW <= 0 && stripH <= 0) return;

        int alpha = (int) (200 * intensity);
        int color = (Math.min(255, alpha) << 24);

        MatrixStack ms = new MatrixStack();
        if (stripW > 0) {
            DrawableHelper.fill(ms, 0, 0, stripW, h, color);
            DrawableHelper.fill(ms, w - stripW, 0, w, h, color);
        }
        if (stripH > 0) {
            DrawableHelper.fill(ms, 0, 0, w, stripH, color);
            DrawableHelper.fill(ms, 0, h - stripH, w, h, color);
        }
    }
}
