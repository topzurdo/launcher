package com.topzurdo.mod.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.hud.*;
import com.topzurdo.mod.modules.performance.*;
import com.topzurdo.mod.modules.render.*;
import com.topzurdo.mod.modules.utility.*;

import net.minecraft.client.MinecraftClient;

/**
 * Manages all TopZurdo modules
 */
public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();
    private final Map<Module.Category, List<Module>> modulesByCategory = new HashMap<>();
    private volatile List<Module> enabledModulesCache = Collections.emptyList();

    public void init() {
        TopZurdoMod.getLogger().info("[TopZurdo] Initializing modules...");

        // RENDER
        register(new CustomParticlesModule());
        register(new NoHurtCamModule());
        register(new LavaClearWaterModule());
        register(new CustomFOVModule());
        register(new ChunksFadeInModule());
        register(new ViewModelChangerModule());
        register(new TimeChangerModule());
        register(new CustomCrosshairModule());
        register(new SkyComfortModule());
        register(new ZoomModule());
        register(new HitMarkersModule());
        register(new DamageOverlayModule());
        register(new DamageNumbersModule());
        register(new TrailsModule());
        register(new ChinaHatModule());
        register(new WeatherControlModule());
        register(new FullBrightModule());

        // HUD
        register(new ArmorHUDModule());
        register(new DurabilityViewerModule());
        register(new CoordinatesModule());
        register(new FPSDisplayModule());
        register(new ServerInfoModule());
        register(new CompassHUDModule());
        register(new TargetInfoModule());

        // UTILITY
        register(new AntiGhostModule());
        register(new ElytraSwapModule());
        register(new ItemLocksModule());
        register(new ContainerSearcherModule());
        register(new InventoryCleanerModule());
        register(new AutoTotemModule());

        // PERFORMANCE
        register(new FPSBoostModule());

        for (Module module : modules) {
            module.init();
        }

        updateEnabledCache();
        TopZurdoMod.getLogger().info("[TopZurdo] Registered {} modules ({} enabled)", modules.size(), enabledModulesCache.size());
    }

    private void register(Module module) {
        modules.add(module);
        modulesByCategory.computeIfAbsent(module.getCategory(), k -> new ArrayList<>()).add(module);
        module.setOnStateChangeCallback(this::updateEnabledCache);
    }

    public void updateEnabledCache() {
        List<Module> enabled = new ArrayList<>();
        for (Module module : modules) {
            if (module.isEnabled()) enabled.add(module);
        }
        enabledModulesCache = enabled;
    }

    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        for (Module module : enabledModulesCache) {
            try { module.onTick(); }
            catch (Exception e) { TopZurdoMod.getLogger().error("Error in module {} tick", module.getName(), e); }
        }
    }

    public void onRender(float partialTicks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        for (Module module : enabledModulesCache) {
            try { module.onRender(partialTicks); }
            catch (Exception e) { TopZurdoMod.getLogger().error("Error in module {} render", module.getName(), e); }
        }
    }

    public List<Module> getModules() { return modules; }
    public List<Module> getAllModules() { return modules; }
    public List<Module> getModulesByCategory(Module.Category category) {
        return modulesByCategory.getOrDefault(category, new ArrayList<>());
    }
    public Module getModule(String id) {
        for (Module module : modules) {
            if (module.getId().equals(id)) return module;
        }
        return null;
    }
    public int getModuleCount() { return modules.size(); }
    public int getEnabledCount() { return enabledModulesCache.size(); }
}
