package com.topzurdo.mod.modules.utility;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Auto Totem Module - automatically equips totem to offhand
 */
public class AutoTotemModule extends Module {

    private Setting<Boolean> onlyWhenLow;
    private Setting<Integer> healthThreshold;

    public AutoTotemModule() {
        super("auto_totem", "Auto Totem", "Автоматически надевает тотем", Category.UTILITY);

        onlyWhenLow = addSetting(Setting.ofBoolean("only_when_low", "Только при низком HP", "Надевать только при низком здоровье", false));
        healthThreshold = addSetting(Setting.ofInt("health_threshold", "Порог здоровья", "Здоровье для активации", 10, 1, 20));
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        // Check if already have totem in offhand
        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) return;

        // Check health threshold
        if (onlyWhenLow.getValue() && mc.player.getHealth() > healthThreshold.getValue()) return;

        // Find totem in inventory
        int totemSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }

        if (totemSlot == -1) return;

        // Swap totem to offhand
        int slot = totemSlot < 9 ? totemSlot + 36 : totemSlot;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
    }
}
