package com.topzurdo.mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.topzurdo.mod.config.ModConfig;
import com.topzurdo.mod.modules.ModuleManager;

import net.fabricmc.api.ModInitializer;

/**
 * TopZurdo Mod - Main class
 * Fabric ModInitializer
 */
public class TopZurdoMod implements ModInitializer {

    public static final String MOD_ID = "topzurdo";
    public static final String MOD_NAME = "TopZurdo Client";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static TopZurdoMod instance;
    private static ModuleManager moduleManager;
    private static ModConfig config;

    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("[TopZurdo] Mod loaded, version {}", VERSION);
        LOGGER.info("[TopZurdo] MOD_ID={}, Java={}", MOD_ID, System.getProperty("java.version"));
    }

    public static TopZurdoMod getInstance() {
        return instance;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static void setModuleManager(ModuleManager mm) {
        moduleManager = mm;
    }

    public ModConfig getConfig() {
        return config;
    }

    public static void setConfig(ModConfig c) {
        config = c;
    }
}
