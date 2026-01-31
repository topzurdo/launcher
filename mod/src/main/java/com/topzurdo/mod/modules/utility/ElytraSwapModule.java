package com.topzurdo.mod.modules.utility;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Elytra Swap Module - quick swap between elytra and chestplate
 */
public class ElytraSwapModule extends Module {

    private Setting<Boolean> autoSwap;

    public ElytraSwapModule() {
        super("elytra_swap", "Elytra Swap", "Быстрая смена элитр/нагрудника", Category.UTILITY);

        autoSwap = addSetting(Setting.ofBoolean("auto_swap", "Автосвап", "Автоматически менять при падении", false));
    }

    public void swap() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        boolean hasElytra = chest.getItem() instanceof ElytraItem;

        // Find swap item in inventory
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStack(i);
            if (hasElytra) {
                // Looking for chestplate
                if (stack.getItem() == Items.DIAMOND_CHESTPLATE || 
                    stack.getItem() == Items.NETHERITE_CHESTPLATE ||
                    stack.getItem() == Items.IRON_CHESTPLATE) {
                    performSwap(mc, i);
                    return;
                }
            } else {
                // Looking for elytra
                if (stack.getItem() instanceof ElytraItem) {
                    performSwap(mc, i);
                    return;
                }
            }
        }
    }

    private void performSwap(MinecraftClient mc, int slot) {
        int chestSlot = 6; // Chest armor slot in player inventory
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot < 9 ? slot + 36 : slot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, chestSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot < 9 ? slot + 36 : slot, 0, SlotActionType.PICKUP, mc.player);
    }
}
