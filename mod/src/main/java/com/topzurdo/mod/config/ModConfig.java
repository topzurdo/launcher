package com.topzurdo.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.topzurdo.mod.TopZurdoMod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configuration manager for TopZurdo mod
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long SAVE_DELAY_MS = 1000;

    private Path configDir;
    private Path configFile;

    private final Map<String, Boolean> moduleStates = new HashMap<>();
    private final Map<String, Map<String, Object>> moduleSettings = new HashMap<>();

    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TopZurdo-ConfigSaver");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> pendingSave;
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final Object saveLock = new Object();

    private String menuOpenMethod = "auto";

    public ModConfig() {
        configDir = FabricLoader.getInstance().getGameDir()
            .resolve("config").resolve("topzurdo");
        configFile = configDir.resolve("modules.json");
    }

    public void load() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            if (Files.exists(configFile)) {
                String json = Files.readString(configFile);
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> data = GSON.fromJson(json, type);

                if (data != null) {
                    Object states = data.get("moduleStates");
                    if (states instanceof Map) {
                        synchronized (moduleStates) {
                            moduleStates.clear();
                            ((Map<?, ?>) states).forEach((k, v) -> {
                                if (k instanceof String && v instanceof Boolean) {
                                    moduleStates.put((String) k, (Boolean) v);
                                }
                            });
                        }
                    }

                    Object settings = data.get("moduleSettings");
                    if (settings instanceof Map) {
                        synchronized (moduleSettings) {
                            moduleSettings.clear();
                            ((Map<?, ?>) settings).forEach((k, v) -> {
                                if (k instanceof String && v instanceof Map) {
                                    Map<String, Object> s = new HashMap<>();
                                    ((Map<?, ?>) v).forEach((sk, sv) -> {
                                        if (sk instanceof String) s.put((String) sk, sv);
                                    });
                                    moduleSettings.put((String) k, s);
                                }
                            });
                        }
                    }

                    Object method = data.get("menuOpenMethod");
                    if (method instanceof String) {
                        menuOpenMethod = (String) method;
                    }

                    Object cat = data.get("lastCategory");
                    if (cat instanceof String) lastCategory = (String) cat;
                    Object mid = data.get("lastModuleId");
                    if (mid instanceof String) lastModuleId = (String) mid;
                    Object mso = data.get("moduleScrollOffset");
                    if (mso instanceof Number) moduleScrollOffset = ((Number) mso).intValue();
                    Object sso = data.get("settingsScrollOffset");
                    if (sso instanceof Number) settingsScrollOffset = ((Number) sso).intValue();
                    Object dbg = data.get("debugLogging");
                    if (dbg instanceof Boolean) debugLogging = (Boolean) dbg;
                    Object mka = data.get("menuKeyAlternative");
                    if (mka instanceof Number) menuKeyAlternative = ((Number) mka).intValue();
                }
                TopZurdoMod.getLogger().info("[TopZurdo] Configuration loaded");
            }
        } catch (Exception e) {
            TopZurdoMod.getLogger().error("[TopZurdo] Failed to load config: {}", e.getMessage());
        }
    }

    public void save() {
        dirty.set(true);
        synchronized (saveLock) {
            if (pendingSave != null) pendingSave.cancel(false);
            pendingSave = saveExecutor.schedule(this::doSave, SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    /** Write config to disk immediately (e.g. when closing menu so state is not lost). */
    public void saveImmediate() {
        synchronized (saveLock) {
            if (pendingSave != null) {
                pendingSave.cancel(false);
                pendingSave = null;
            }
        }
        dirty.set(true);
        doSave();
    }

    private void doSave() {
        if (!dirty.getAndSet(false)) return;
        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);

            Map<String, Object> data = new HashMap<>();
            synchronized (moduleStates) { data.put("moduleStates", new HashMap<>(moduleStates)); }
            synchronized (moduleSettings) { data.put("moduleSettings", new HashMap<>(moduleSettings)); }
            data.put("menuOpenMethod", menuOpenMethod);
            data.put("lastCategory", lastCategory);
            data.put("lastModuleId", lastModuleId);
            data.put("moduleScrollOffset", Integer.valueOf(moduleScrollOffset));
            data.put("settingsScrollOffset", Integer.valueOf(settingsScrollOffset));
            data.put("debugLogging", Boolean.valueOf(debugLogging));
            data.put("menuKeyAlternative", Integer.valueOf(menuKeyAlternative));

            Files.writeString(configFile, GSON.toJson(data));
        } catch (IOException e) {
            TopZurdoMod.getLogger().error("[TopZurdo] Failed to save config: {}", e.getMessage());
        }
    }

    public synchronized boolean isModuleEnabled(String moduleId) {
        return moduleStates.getOrDefault(moduleId, false);
    }

    public void setModuleEnabled(String moduleId, boolean enabled) {
        synchronized (moduleStates) { moduleStates.put(moduleId, enabled); }
        save();
    }

    public Object getModuleSetting(String moduleId, String settingId) {
        synchronized (moduleSettings) {
            Map<String, Object> s = moduleSettings.get(moduleId);
            return s != null ? s.get(settingId) : null;
        }
    }

    public Object getModuleSetting(String moduleId, String settingId, Object defaultValue) {
        Object val = getModuleSetting(moduleId, settingId);
        return val != null ? val : defaultValue;
    }

    public void setModuleSetting(String moduleId, String settingId, Object value) {
        synchronized (moduleSettings) {
            moduleSettings.computeIfAbsent(moduleId, k -> new HashMap<>()).put(settingId, value);
        }
        save();
    }

    public String getMenuOpenMethod() { return menuOpenMethod; }
    public void setMenuOpenMethod(String method) { this.menuOpenMethod = method; save(); }

    // UI state persistence
    private String lastCategory = "RENDER";
    private String lastModuleId = "";
    private int moduleScrollOffset = 0;
    private int settingsScrollOffset = 0;
    private boolean debugLogging = false;

    public String getLastCategory() { return lastCategory; }
    public void setLastCategory(String cat) { this.lastCategory = cat; save(); }
    public String getLastModuleId() { return lastModuleId; }
    public void setLastModuleId(String id) { this.lastModuleId = id; save(); }
    public int getModuleScrollOffset() { return moduleScrollOffset; }
    public void setModuleScrollOffset(int off) { this.moduleScrollOffset = off; save(); }
    public int getSettingsScrollOffset() { return settingsScrollOffset; }
    public void setSettingsScrollOffset(int off) { this.settingsScrollOffset = off; save(); }
    public boolean isDebugLogging() { return debugLogging; }
    public void setDebugLogging(boolean on) { this.debugLogging = on; save(); }

    private int menuKeyAlternative = 0; // 0 = none, GLFW key code otherwise
    public int getMenuKeyAlternative() { return menuKeyAlternative; }
    public void setMenuKeyAlternative(int key) { this.menuKeyAlternative = key; save(); }
}
