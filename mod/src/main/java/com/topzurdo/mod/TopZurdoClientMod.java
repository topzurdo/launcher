package com.topzurdo.mod;

import org.lwjgl.glfw.GLFW;

import com.topzurdo.mod.config.ModConfig;
import com.topzurdo.mod.gui.CustomMainMenuScreen;
import com.topzurdo.mod.gui.TopZurdoMenuScreen;
import com.topzurdo.mod.modules.ModuleManager;
import com.topzurdo.mod.patches.DemoDisabler;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/**
 * TopZurdo Client Mod - Fabric ClientModInitializer
 * Handles client setup, keybindings, module manager, menu open logic
 */
public class TopZurdoClientMod implements ClientModInitializer {

    public static KeyBinding menuKey; // unused, kept for compatibility
    private boolean lastMenuKeyDown = false;
    private boolean demoDisableRetried = false;

    private ModuleManager moduleManager;
    private ModConfig config;

    @Override
    public void onInitializeClient() {
        clientInstance = this;
        TopZurdoMod.getLogger().info("[TopZurdo] Client setup started");

        try {
            config = new ModConfig();
            config.load();
            TopZurdoMod.setConfig(config);
            String preferred = config.getMenuOpenMethod();
            if (preferred != null && !"auto".equals(preferred)) {
                TopZurdoMod.getLogger().info("[TopZurdo] Preferred menu open method: {} (from config)", preferred);
            }
        } catch (Exception e) {
            TopZurdoMod.getLogger().error("[TopZurdo] Failed to load config: {}", e.getMessage(), e);
            config = new ModConfig();
        }

        if (DemoDisabler.disable()) {
            TopZurdoMod.getLogger().info("[TopZurdo] Demo mode disabled via reflection");
        }

        menuKey = new KeyBinding(
            "key.topzurdo.menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "key.categories.topzurdo"
        );
        TopZurdoMod.getLogger().info("[TopZurdo] Menu key: Right Shift (polled via GLFW)");

        try {
            moduleManager = new ModuleManager();
            moduleManager.init();
            TopZurdoMod.setModuleManager(moduleManager);
            TopZurdoMod.getLogger().info("[TopZurdo] {} modules loaded; press Right Shift to open menu", moduleManager.getModuleCount());
        } catch (Exception e) {
            TopZurdoMod.getLogger().error("[TopZurdo] Module manager init failed: {}", e.getMessage(), e);
        }

        // Replace main menu with custom one
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen && !(screen instanceof CustomMainMenuScreen)) {
                client.openScreen(new CustomMainMenuScreen());
                TopZurdoMod.getLogger().info("[TopZurdo] Custom main menu enabled");
            }
        });

        // ContainerSearcherModule: draw highlights, clear on close
        // ItemLocksModule: Alt+click to toggle lock
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof HandledScreen)) return;
            final HandledScreen<?> handledScreen = (HandledScreen<?>) screen;

            ScreenEvents.afterRender(screen).register((Screen s, MatrixStack matrices, int mouseX, int mouseY, float tickDelta) -> {
                if (moduleManager == null) return;
                com.topzurdo.mod.modules.Module cs = moduleManager.getModule("container_searcher");
                if (cs != null && cs.isEnabled() && cs instanceof com.topzurdo.mod.modules.utility.ContainerSearcherModule) {
                    ((com.topzurdo.mod.modules.utility.ContainerSearcherModule) cs).drawHighlights(handledScreen, matrices);
                }
            });

            ScreenEvents.remove(screen).register((Screen removedScreen) -> {
                if (moduleManager == null) return;
                com.topzurdo.mod.modules.Module cs = moduleManager.getModule("container_searcher");
                if (cs != null && cs instanceof com.topzurdo.mod.modules.utility.ContainerSearcherModule) {
                    ((com.topzurdo.mod.modules.utility.ContainerSearcherModule) cs).onScreenClosed();
                }
            });

            ScreenMouseEvents.allowMouseClick(screen).register((Screen s, double mouseX, double mouseY, int button) -> {
                if (moduleManager == null) return true;
                com.topzurdo.mod.modules.Module il = moduleManager.getModule("item_locks");
                if (il == null || !il.isEnabled() || !(il instanceof com.topzurdo.mod.modules.utility.ItemLocksModule)) return true;
                com.topzurdo.mod.modules.utility.ItemLocksModule ilm = (com.topzurdo.mod.modules.utility.ItemLocksModule) il;
                Slot slot = getSlotAt(handledScreen, mouseX, mouseY);
                if (slot != null) {
                    if (ilm.shouldHandleMouseClick(button)) {
                        ilm.toggleSlotLock(slot.id);
                        return false;
                    }
                    if (ilm.shouldPreventMove(slot.id)) return false;
                }
                return true;
            });

            ScreenKeyboardEvents.allowKeyPress(screen).register((Screen s, int keyCode, int scanCode, int modifiers) -> {
                if (moduleManager == null) return true;
                com.topzurdo.mod.modules.Module il = moduleManager.getModule("item_locks");
                if (il == null || !il.isEnabled() || !(il instanceof com.topzurdo.mod.modules.utility.ItemLocksModule)) return true;
                com.topzurdo.mod.modules.utility.ItemLocksModule ilm = (com.topzurdo.mod.modules.utility.ItemLocksModule) il;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.options != null && mc.options.keyDrop.matchesKey(keyCode, scanCode)) {
                    int selectedSlot = mc.player != null ? mc.player.inventory.selectedSlot : 0;
                    if (ilm.shouldPreventDrop(selectedSlot)) return false;
                }
                return true;
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick(client));

        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            if (moduleManager != null) {
                moduleManager.onRender(tickDelta);
            }
        });

        // 3D World rendering for Trails, ChinaHat, etc.
        WorldRenderEvents.END.register(context -> {
            if (moduleManager != null) {
                MatrixStack matrices = context.matrixStack();
                float tickDelta = context.tickDelta();

                // Trails
                com.topzurdo.mod.modules.Module trails = moduleManager.getModule("trails");
                if (trails != null && trails.isEnabled() && trails instanceof com.topzurdo.mod.modules.render.TrailsModule) {
                    ((com.topzurdo.mod.modules.render.TrailsModule) trails).onWorldRender(matrices, tickDelta);
                }

                // ChinaHat
                com.topzurdo.mod.modules.Module chinaHat = moduleManager.getModule("china_hat");
                if (chinaHat != null && chinaHat.isEnabled() && chinaHat instanceof com.topzurdo.mod.modules.render.ChinaHatModule) {
                    ((com.topzurdo.mod.modules.render.ChinaHatModule) chinaHat).onWorldRender(matrices, tickDelta);
                }

                // Custom Particles (World 3D)
                com.topzurdo.mod.modules.Module particles = moduleManager.getModule("custom_particles");
                if (particles != null && particles.isEnabled() && particles instanceof com.topzurdo.mod.modules.render.CustomParticlesModule) {
                    ((com.topzurdo.mod.modules.render.CustomParticlesModule) particles).onWorldRender(matrices, tickDelta);
                }
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (moduleManager != null && entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                com.topzurdo.mod.modules.Module crosshair = moduleManager.getModule("custom_crosshair");
                if (crosshair != null && crosshair.isEnabled() && crosshair instanceof com.topzurdo.mod.modules.render.CustomCrosshairModule) {
                    ((com.topzurdo.mod.modules.render.CustomCrosshairModule) crosshair).onPlayerAttack();
                }
                com.topzurdo.mod.modules.Module hitMarkers = moduleManager.getModule("hit_markers");
                if (hitMarkers != null && hitMarkers.isEnabled() && hitMarkers instanceof com.topzurdo.mod.modules.render.HitMarkersModule) {
                    ((com.topzurdo.mod.modules.render.HitMarkersModule) hitMarkers).onPlayerAttack();
                }
                com.topzurdo.mod.modules.Module particles = moduleManager.getModule("custom_particles");
                if (particles != null && particles.isEnabled() && particles instanceof com.topzurdo.mod.modules.render.CustomParticlesModule) {
                    ((com.topzurdo.mod.modules.render.CustomParticlesModule) particles).onPlayerAttack(living);
                }
            }
            return ActionResult.PASS;
        });

        TopZurdoMod.getLogger().info("[TopZurdo] Client setup complete");
    }

    private void onClientTick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        Screen currentScreen = client.currentScreen;
        boolean canOpenFromHere = currentScreen == null
            || currentScreen instanceof TitleScreen
            || currentScreen instanceof GameMenuScreen
            || currentScreen instanceof CustomMainMenuScreen;

        int alt = config != null ? config.getMenuKeyAlternative() : 0;

        if (canOpenFromHere) {
            boolean opened = false;

            try {
                if (client.getWindow() != null) {
                    long win = client.getWindow().getHandle();
                    boolean rshift = (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS);
                    boolean altDown = (alt != 0 && GLFW.glfwGetKey(win, alt) == GLFW.GLFW_PRESS);
                    boolean nowDown = rshift || altDown;
                    if (!lastMenuKeyDown && nowDown) {
                        if (config != null) config.setMenuOpenMethod("glfw_poll");
                        lastMenuKeyDown = true;
                        opened = true;
                        TopZurdoMod.logEvent("GLFW: menu key edge, opening");
                        openModMenu(client);
                    } else {
                        lastMenuKeyDown = nowDown;
                    }
                } else {
                    lastMenuKeyDown = false;
                }
            } catch (Throwable t) {
                lastMenuKeyDown = false;
                TopZurdoMod.getLogger().warn("[TopZurdo] [Tick] GLFW poll error", t);
            }

            boolean rshift = false;
            boolean altDown = false;
            try {
                if (client.getWindow() != null) {
                    long win = client.getWindow().getHandle();
                    rshift = (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS);
                    altDown = (alt != 0 && GLFW.glfwGetKey(win, alt) == GLFW.GLFW_PRESS);
                }
            } catch (Throwable t) { }
            boolean keyDown = rshift || altDown;
            if (!opened && keyDown && !lastMenuKeyDown) {
                if (config != null) config.setMenuOpenMethod("keybinding");
                TopZurdoMod.logEvent("KeyBinding: opening");
                openModMenu(client);
            }
            lastMenuKeyDown = keyDown;
        } else {
            try {
                if (client.getWindow() != null) {
                    long win = client.getWindow().getHandle();
                    boolean rshift = (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS);
                    boolean altDown = (alt != 0 && GLFW.glfwGetKey(win, alt) == GLFW.GLFW_PRESS);
                    lastMenuKeyDown = rshift || altDown;
                } else {
                    lastMenuKeyDown = false;
                }
            } catch (Throwable t) {
                lastMenuKeyDown = false;
            }
        }

        if (!demoDisableRetried && !DemoDisabler.isDisabled()) {
            demoDisableRetried = true;
            if (DemoDisabler.disable()) {
                TopZurdoMod.getLogger().info("[TopZurdo] Demo mode disabled on retry");
            }
        }

        if (moduleManager != null) {
            moduleManager.onTick();
            // Zoom: hold C to zoom (key polled so it works in-game)
            com.topzurdo.mod.modules.Module zoomMod = moduleManager.getModule("zoom");
            if (zoomMod != null && zoomMod instanceof com.topzurdo.mod.modules.render.ZoomModule) {
                boolean zoomKey = false;
                if (zoomMod.isEnabled()) {
                    try {
                        if (client.getWindow() != null) {
                            long win = client.getWindow().getHandle();
                            zoomKey = (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_C) == GLFW.GLFW_PRESS);
                        }
                    } catch (Throwable t) { }
                }
                ((com.topzurdo.mod.modules.render.ZoomModule) zoomMod).setZooming(zoomKey);
            }
        }
    }

    private void openModMenu(MinecraftClient client) {
        Screen parent = (client.currentScreen instanceof TitleScreen
            || client.currentScreen instanceof GameMenuScreen
            || client.currentScreen instanceof CustomMainMenuScreen)
            ? client.currentScreen : null;
        client.openScreen(new TopZurdoMenuScreen(parent));
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ModConfig getConfig() {
        return config;
    }

    private static TopZurdoClientMod clientInstance;

    public static TopZurdoClientMod getClientInstance() {
        return clientInstance;
    }

    /** Find slot at screen coordinates for HandledScreen */
    private static Slot getSlotAt(HandledScreen<?> screen, double mouseX, double mouseY) {
        com.topzurdo.mod.patches.mixins.HandledScreenAccessor accessor = (com.topzurdo.mod.patches.mixins.HandledScreenAccessor) screen;
        for (Slot slot : screen.getScreenHandler().slots) {
            int x = accessor.getX() + slot.x;
            int y = accessor.getY() + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
    }
}
