package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

/**
 * Sky Comfort Module - custom sky colors
 */
public class SkyComfortModule extends Module {

    private Setting<Integer> skyColor;
    private Setting<Integer> fogColor;
    private Setting<Boolean> disableSunMoon;
    private Setting<Boolean> disableStars;
    private Setting<Float> brightness;

    public SkyComfortModule() {
        super("sky_comfort", "Sky Comfort", "Настройка неба", Category.RENDER);

        skyColor = addSetting(Setting.ofInt("sky_color", "Цвет неба", "Цвет неба (RGB)", 0x87CEEB, 0, 0xFFFFFF));
        fogColor = addSetting(Setting.ofInt("fog_color", "Цвет тумана", "Цвет тумана (RGB)", 0xC0D8E4, 0, 0xFFFFFF));
        disableSunMoon = addSetting(Setting.ofBoolean("disable_sun_moon", "Убрать солнце/луну", "Скрыть небесные светила", false));
        disableStars = addSetting(Setting.ofBoolean("disable_stars", "Убрать звёзды", "Скрыть звёзды", false));
        brightness = addSetting(Setting.ofFloat("brightness", "Яркость", "Яркость неба", 1.0f, 0.5f, 2.0f));
    }

    public int getSkyColor() { return skyColor.getValue(); }
    public int getFogColor() { return fogColor.getValue(); }
    public boolean shouldDisableSunMoon() { return disableSunMoon.getValue(); }
    public boolean shouldDisableStars() { return disableStars.getValue(); }
    public float getBrightness() { return brightness.getValue(); }
}
