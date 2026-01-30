package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

/**
 * Visually change the time of day (client-only).
 * Uses WorldMixin to override getTimeOfDay() — no server modification.
 * Instant change, no flicker.
 */
public class TimeChangerModule extends Module {

    private final Setting<String> timePreset;

    // Internal time value - not exposed as setting (controlled by preset only)
    private int currentTime = NOON;

    public static final int SUNRISE = 0;
    public static final int NOON = 6000;
    public static final int COMFORT = 8000;
    public static final int SUNSET = 12000;
    public static final int MIDNIGHT = 18000;

    public TimeChangerModule() {
        super("time_changer", "Time Changer", "Изменение времени суток (визуально)", Category.RENDER);

        this.timePreset = addSetting(Setting.ofOptions(
            "time_preset", "Пресет",
            "Выберите время суток", "noon",
            "sunrise", "noon", "comfort", "sunset", "midnight"
        ));

        // Apply preset immediately when changed
        timePreset.onChange(this::applyPreset);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        // Apply current preset when module is enabled
        applyPreset(timePreset.getValue());
    }

    /** Called by WorldMixin to get override time. */
    public int getTargetTime() {
        return currentTime;
    }

    private void applyPreset(String preset) {
        if (preset == null) {
            currentTime = NOON;
            return;
        }
        switch (preset.toLowerCase()) {
            case "sunrise":
            case "dawn":
                currentTime = SUNRISE;
                break;
            case "noon":
            case "day":
                currentTime = NOON;
                break;
            case "comfort":
                currentTime = COMFORT;
                break;
            case "sunset":
            case "dusk":
                currentTime = SUNSET;
                break;
            case "midnight":
            case "night":
                currentTime = MIDNIGHT;
                break;
            default:
                currentTime = NOON;
        }
    }
}
