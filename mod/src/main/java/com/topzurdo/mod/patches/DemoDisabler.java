package com.topzurdo.mod.patches;

import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Disables Minecraft demo mode via reflection
 */
public class DemoDisabler {

    private static final Logger LOG = LogManager.getLogger("DemoDisabler");

    public static boolean disable() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) return false;

            // Check if already not in demo mode
            if (!mc.isDemo()) {
                LOG.info("Game is not in demo mode, no patch needed");
                return true;
            }

            // Try to disable demo mode via reflection
            for (Field field : MinecraftClient.class.getDeclaredFields()) {
                if (field.getType() == boolean.class) {
                    field.setAccessible(true);
                    // Look for the demo field (usually named 'demo' or 'isDemo')
                    String name = field.getName().toLowerCase();
                    if (name.contains("demo")) {
                        field.setBoolean(mc, false);
                        LOG.info("Demo mode disabled via field: {}", field.getName());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to disable demo mode: {}", e.getMessage());
        }
        return false;
    }
}
