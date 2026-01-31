package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

/**
 * Weather Control Module - client-side weather override
 */
public class WeatherControlModule extends Module {

    private Setting<String> weatherType;
    private Setting<Float> rainStrength;
    private Setting<Float> thunderStrength;

    public WeatherControlModule() {
        super("weather_control", "Weather Control", "Управление погодой", Category.RENDER);

        weatherType = addSetting(Setting.ofOptions("weather_type", "Тип погоды", "Выберите погоду", "clear", "clear", "rain", "thunder"));
        rainStrength = addSetting(Setting.ofFloat("rain_strength", "Сила дождя", "Интенсивность дождя", 1.0f, 0.0f, 1.0f));
        thunderStrength = addSetting(Setting.ofFloat("thunder_strength", "Сила грозы", "Интенсивность грозы", 1.0f, 0.0f, 1.0f));
    }

    public String getWeatherType() { return weatherType.getValue(); }
    public float getRainStrength() { return rainStrength.getValue(); }
    public float getThunderStrength() { return thunderStrength.getValue(); }

    public boolean shouldClearWeather() {
        return isEnabled() && "clear".equals(weatherType.getValue());
    }

    public boolean shouldRain() {
        return isEnabled() && "rain".equals(weatherType.getValue());
    }

    public boolean shouldThunder() {
        return isEnabled() && "thunder".equals(weatherType.getValue());
    }

    public float getRainFactor() {
        if (!isEnabled()) return 1f;
        if (shouldClearWeather()) return 0f;
        return getRainStrength();
    }

    public float getThunderFactor() {
        if (!isEnabled()) return 1f;
        if (shouldClearWeather()) return 0f;
        if (!shouldThunder()) return 0f;
        return getThunderStrength();
    }
}
