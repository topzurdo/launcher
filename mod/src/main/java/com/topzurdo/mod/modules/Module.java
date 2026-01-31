package com.topzurdo.mod.modules;

import java.util.ArrayList;
import java.util.List;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.config.ModConfig;

/**
 * Base class for all TopZurdo modules
 * Each module represents a toggleable feature
 */
public abstract class Module {

    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;

    protected final List<Setting<?>> settings = new ArrayList<>();
    private Runnable onStateChangeCallback;

    public Module(String id, String name, String description, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false;
    }

    public void init() {
        ModConfig config = TopZurdoMod.getInstance() != null ? TopZurdoMod.getInstance().getConfig() : null;
        if (config != null) {
            this.enabled = config.isModuleEnabled(id);
            for (Setting<?> setting : settings) {
                setting.load(id);
            }
        }
        if (enabled) {
            onEnable();
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public List<Setting<?>> getSettings() { return settings; }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) onEnable();
            else onDisable();

            ModConfig config = TopZurdoMod.getInstance() != null ? TopZurdoMod.getInstance().getConfig() : null;
            if (config != null) {
                config.setModuleEnabled(id, enabled);
            }
            if (onStateChangeCallback != null) {
                onStateChangeCallback.run();
            }
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setOnStateChangeCallback(Runnable callback) {
        this.onStateChangeCallback = callback;
    }

    protected <T> Setting<T> addSetting(Setting<T> setting) {
        settings.add(setting);
        return setting;
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    public void onRender(float partialTicks) {}

    /**
     * For HUD modules with pos_x/pos_y: return [x, y, width, height] for hit-test and drag.
     * Default returns null (not draggable).
     */
    public int[] getHudBounds() {
        return null;
    }

    public enum Category {
        RENDER("Render", "Visual modifications"),
        HUD("HUD", "Heads-up display"),
        UTILITY("Utility", "Helpful tools"),
        PERFORMANCE("Performance", "FPS optimization");

        private final String name;
        private final String description;

        Category(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
    }
}
