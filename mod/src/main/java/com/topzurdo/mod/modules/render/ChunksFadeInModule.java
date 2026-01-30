package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

/**
 * Chunks Fade In Module - smooth chunk loading animation
 */
public class ChunksFadeInModule extends Module {

    private Setting<Float> fadeSpeed;
    private Setting<Boolean> fadeFromBottom;

    public ChunksFadeInModule() {
        super("chunks_fade_in", "Chunks Fade In", "Плавное появление чанков", Category.RENDER);

        fadeSpeed = addSetting(Setting.ofFloat("fade_speed", "Скорость", "Скорость появления", 0.5f, 0.1f, 2.0f));
        fadeFromBottom = addSetting(Setting.ofBoolean("fade_from_bottom", "Снизу вверх", "Чанки появляются снизу", true));
    }

    public float getFadeSpeed() { return fadeSpeed.getValue(); }
    public boolean fadeFromBottom() { return fadeFromBottom.getValue(); }
}
