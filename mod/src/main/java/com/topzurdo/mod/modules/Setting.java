package com.topzurdo.mod.modules;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.config.ModConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A configurable setting for a module
 * Supports various types: boolean, int, float, String, enum, options (choice by name)
 *
 * Client-side only
 */
public class Setting<T> {

    private final String key;
    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;
    private T minValue;
    private T maxValue;
    private List<String> options;
    private Consumer<T> onChange;

    public Setting(String key, String name, String description, T defaultValue) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    /**
     * Create a boolean setting
     */
    public static Setting<Boolean> ofBoolean(String key, String name, String description, boolean defaultValue) {
        return new Setting<>(key, name, description, defaultValue);
    }

    /**
     * Create an integer setting with range
     */
    public static Setting<Integer> ofInt(String key, String name, String description,
                                          int defaultValue, int min, int max) {
        Setting<Integer> setting = new Setting<>(key, name, description, defaultValue);
        setting.minValue = min;
        setting.maxValue = max;
        return setting;
    }

    /**
     * Create a float setting with range
     */
    public static Setting<Float> ofFloat(String key, String name, String description,
                                          float defaultValue, float min, float max) {
        Setting<Float> setting = new Setting<>(key, name, description, defaultValue);
        setting.minValue = min;
        setting.maxValue = max;
        return setting;
    }

    /**
     * Create a double setting with range
     */
    public static Setting<Double> ofDouble(String key, String name, String description,
                                            double defaultValue, double min, double max) {
        Setting<Double> setting = new Setting<>(key, name, description, defaultValue);
        setting.minValue = min;
        setting.maxValue = max;
        return setting;
    }

    /**
     * Create a color setting (RGB integer).
     * Marked with special range for UI detection.
     */
    public static Setting<Integer> ofColor(String key, String name, String description, int defaultColor) {
        Setting<Integer> setting = new Setting<>(key, name, description, defaultColor);
        setting.minValue = 0x000000;  // Special marker: color min
        setting.maxValue = 0xFFFFFF;  // Special marker: color max
        return setting;
    }

    /**
     * Create a string setting
     */
    public static Setting<String> ofString(String key, String name, String description, String defaultValue) {
        return new Setting<>(key, name, description, defaultValue);
    }

    /**
     * Create an options setting (choice by display name).
     * Value is one of the options; load validates and resets to default if invalid.
     */
    public static Setting<String> ofOptions(String key, String name, String description,
                                            String defaultValue, List<String> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("options must be non-null and non-empty");
        }
        Setting<String> setting = new Setting<>(key, name, description, defaultValue);
        setting.options = new java.util.ArrayList<>(options);
        if (!options.contains(defaultValue)) {
            setting.value = options.get(0);
        }
        return setting;
    }

    /**
     * Create an options setting with varargs.
     */
    public static Setting<String> ofOptions(String key, String name, String description,
                                            String defaultValue, String... options) {
        return ofOptions(key, name, description, defaultValue, Arrays.asList(options));
    }

    /**
     * Set change callback
     */
    public Setting<T> onChange(Consumer<T> callback) {
        this.onChange = callback;
        return this;
    }

    /**
     * Load value from config
     */
    @SuppressWarnings("unchecked")
    public void load(String moduleId) {
        ModConfig config = TopZurdoMod.getConfig();
        if (config == null) {
            return; // Config not yet initialized
        }
        Object loaded = config.getModuleSetting(moduleId, key, defaultValue);

        if (loaded != null) {
            try {
                // ofOptions: в JSON числа приходят как Double/Integer — не кастуем в String, сбрасываем в default
                if (options != null && !options.isEmpty() && loaded instanceof Number) {
                    value = (T) (options.contains(defaultValue) ? defaultValue : options.get(0));
                } else if (defaultValue instanceof Integer && loaded instanceof Number) {
                    value = (T) Integer.valueOf(((Number) loaded).intValue());
                } else if (defaultValue instanceof Float && loaded instanceof Number) {
                    value = (T) Float.valueOf(((Number) loaded).floatValue());
                } else if (defaultValue instanceof Double && loaded instanceof Number) {
                    value = (T) Double.valueOf(((Number) loaded).doubleValue());
                } else {
                    value = (T) loaded;
                }
            } catch (ClassCastException e) {
                if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().warn("[TopZurdo] Setting.load cast failed key={} moduleId={}: {}; using default", key, moduleId, e.getMessage());
                value = defaultValue;
            }
        }
        // For options: ensure loaded value is in the list
        if (options != null && !options.isEmpty() && value instanceof String) {
            String s = (String) value;
            if (!options.contains(s)) {
                value = (T) (options.contains(defaultValue) ? defaultValue : options.get(0));
            }
        }
    }

    /**
     * Save value to config
     */
    public void save(String moduleId) {
        ModConfig config = TopZurdoMod.getConfig();
        if (config != null) {
            config.setModuleSetting(moduleId, key, value);
        }
    }

    /**
     * Get current value
     */
    public T getValue() {
        return value;
    }

    /**
     * Set value
     */
    public void setValue(T value) {
        // Clamp numeric values
        if (minValue != null && maxValue != null) {
            if (value instanceof Integer) {
                int v = (Integer) value;
                int min = (Integer) minValue;
                int max = (Integer) maxValue;
                value = (T) Integer.valueOf(Math.max(min, Math.min(max, v)));
            } else if (value instanceof Float) {
                float v = (Float) value;
                float min = (Float) minValue;
                float max = (Float) maxValue;
                value = (T) Float.valueOf(Math.max(min, Math.min(max, v)));
            } else if (value instanceof Double) {
                double v = (Double) value;
                double min = (Double) minValue;
                double max = (Double) maxValue;
                value = (T) Double.valueOf(Math.max(min, Math.min(max, v)));
            }
        }

        this.value = value;

        if (onChange != null) {
            onChange.accept(value);
        }
    }

    // ==================== Getters ====================

    public Consumer<T> getOnChange() {
        return onChange;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public T getMinValue() {
        return minValue;
    }

    public T getMaxValue() {
        return maxValue;
    }

    public boolean hasRange() {
        return minValue != null && maxValue != null;
    }

    public boolean isBoolean() {
        return defaultValue instanceof Boolean;
    }

    public boolean isNumber() {
        return defaultValue instanceof Number;
    }

    public boolean isInteger() {
        return defaultValue instanceof Integer;
    }

    public boolean isFloat() {
        return defaultValue instanceof Float;
    }

    public boolean isDouble() {
        return defaultValue instanceof Double;
    }

    public boolean isString() {
        return defaultValue instanceof String;
    }

    /**
     * Check if this is a color setting (created via ofColor).
     * Detected by Integer type with range 0x000000 to 0xFFFFFF.
     */
    public boolean isColor() {
        if (!(defaultValue instanceof Integer)) return false;
        if (minValue == null || maxValue == null) return false;
        Integer min = (Integer) minValue;
        Integer max = (Integer) maxValue;
        return min == 0x000000 && max == 0xFFFFFF;
    }

    /**
     * Whether this is an options (choice by name) setting.
     */
    public boolean isOptions() {
        return options != null && !options.isEmpty();
    }

    /**
     * Get the list of options for ofOptions() settings. Returns empty list if not options.
     */
    public List<String> getOptions() {
        return options != null ? Collections.unmodifiableList(options) : Collections.emptyList();
    }

    /**
     * Get min value as Number (for sliders)
     */
    public Number getMin() {
        return minValue instanceof Number ? (Number) minValue : null;
    }

    /**
     * Get max value as Number (for sliders)
     */
    public Number getMax() {
        return maxValue instanceof Number ? (Number) maxValue : null;
    }

    /**
     * Constructor with range (convenience)
     */
    public Setting(String key, String name, String description, T defaultValue, T min, T max) {
        this(key, name, description, defaultValue);
        this.minValue = min;
        this.maxValue = max;
    }
}
