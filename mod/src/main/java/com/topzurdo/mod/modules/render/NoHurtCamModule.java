package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

/**
 * No Hurt Cam Module - removes camera shake on damage
 * Uses GameRendererMixin for actual implementation
 */
public class NoHurtCamModule extends Module {

    private Setting<Boolean> removeRedTint;
    private Setting<Float> shakeReduction;

    public NoHurtCamModule() {
        super("no_hurt_cam", "NoHurtCam", "Убирает тряску камеры при уроне", Category.RENDER);

        removeRedTint = addSetting(Setting.ofBoolean("remove_red_tint", "Убрать красный оттенок",
            "Убирает красный эффект при получении урона", false));
        shakeReduction = addSetting(Setting.ofFloat("shake_reduction", "Интенсивность",
            "0 = без эффекта, 1 = полностью убрать тряску", 1.0f, 0.0f, 1.0f));
    }

    public float getShakeReduction() {
        return isEnabled() ? shakeReduction.getValue() : 0f;
    }
}
