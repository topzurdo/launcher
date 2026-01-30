package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

/**
 * Lava Clear Water Module - transparent lava/water visibility
 */
public class LavaClearWaterModule extends Module {

    private Setting<Float> lavaTransparency;
    private Setting<Float> waterTransparency;
    private Setting<Boolean> clearFog;

    public LavaClearWaterModule() {
        super("lava_clear_water", "LavaClearWater", "Прозрачная лава и вода", Category.RENDER);

        lavaTransparency = addSetting(Setting.ofFloat("lava_transparency", "Прозрачность лавы",
            "Насколько прозрачна лава", 0.8f, 0.1f, 1.0f));
        waterTransparency = addSetting(Setting.ofFloat("water_transparency", "Прозрачность воды",
            "Насколько прозрачна вода", 0.9f, 0.1f, 1.0f));
        clearFog = addSetting(Setting.ofBoolean("clear_fog", "Убрать туман",
            "Убирает туман в воде/лаве", true));
    }

    public float getLavaTransparency() { return lavaTransparency.getValue(); }
    public float getWaterTransparency() { return waterTransparency.getValue(); }
    public boolean shouldClearFog() { return clearFog.getValue(); }
}
