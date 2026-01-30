package com.topzurdo.mod.modules.utility;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Inventory Cleaner Module - auto-drop trash items
 */
public class InventoryCleanerModule extends Module {

    private Setting<Boolean> dropDirt;
    private Setting<Boolean> dropCobble;
    private Setting<Boolean> dropGravel;
    private Setting<Boolean> dropSand;
    private Setting<Integer> delay;

    private long lastDrop = 0;

    public InventoryCleanerModule() {
        super("inventory_cleaner", "Inventory Cleaner", "Автоматическая очистка инвентаря", Category.UTILITY);

        dropDirt = addSetting(Setting.ofBoolean("drop_dirt", "Выбрасывать землю", "Автоматически выбрасывать землю", true));
        dropCobble = addSetting(Setting.ofBoolean("drop_cobble", "Выбрасывать булыжник", "Автоматически выбрасывать булыжник", true));
        dropGravel = addSetting(Setting.ofBoolean("drop_gravel", "Выбрасывать гравий", "Автоматически выбрасывать гравий", true));
        dropSand = addSetting(Setting.ofBoolean("drop_sand", "Выбрасывать песок", "Автоматически выбрасывать песок", false));
        delay = addSetting(Setting.ofInt("delay", "Задержка (мс)", "Задержка между выбрасываниями", 100, 50, 500));
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        long now = System.currentTimeMillis();
        if (now - lastDrop < delay.getValue()) return;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (shouldDrop(stack)) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 1, SlotActionType.THROW, mc.player);
                lastDrop = now;
                return;
            }
        }
    }

    private boolean shouldDrop(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getItem().toString().toLowerCase();
        if (dropDirt.getValue() && name.contains("dirt")) return true;
        if (dropCobble.getValue() && name.contains("cobblestone")) return true;
        if (dropGravel.getValue() && name.contains("gravel")) return true;
        if (dropSand.getValue() && name.contains("sand")) return true;
        return false;
    }
}
