package com.topzurdo.mod.modules.hud;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Armor HUD Module - displays armor durability
 */
public class ArmorHUDModule extends Module {

    private Setting<Integer> posX;
    private Setting<Integer> posY;
    private Setting<Boolean> showDurability;

    /**
     * Creates the Armor HUD module and registers its configurable settings.
     *
     * Registers:
     * - posX: horizontal position (default 10, range 0–2000)
     * - posY: vertical position (default 10, range 0–2000)
     * - showDurability: whether to display item durability (default true)
     */
    public ArmorHUDModule() {
        super("armor_hud", "Armor HUD", "Отображение брони", Category.HUD);

        posX = addSetting(Setting.ofInt("pos_x", "Позиция X", "Горизонтальная позиция", 10, 0, 2000));
        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 10, 0, 2000));
        showDurability = addSetting(Setting.ofBoolean("show_durability", "Прочность", "Показывать прочность", true));
    }

    /**
     * Provide the HUD bounding rectangle based on the module's current position settings.
     *
     * @return an int[] containing [x, y, width, height] where x and y are the current posX and posY values, width is 80 and height is 90
     */
    @Override
    public int[] getHudBounds() {
        return new int[] { posX.getValue(), posY.getValue(), 80, 90 };
    }

    /**
     * Renders the player's equipped armor items at the module's configured position and, if enabled,
     * overlays each item's durability as a "current/max" string colored by remaining percentage.
     *
     * @param partialTicks interpolation value for the current render frame
     */
    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int x = posX.getValue();
        int y = posY.getValue();
        MatrixStack ms = new MatrixStack();
        TextRenderer tr = mc.textRenderer;

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : slots) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                mc.getItemRenderer().renderInGuiWithOverrides(stack, x, y);
                if (showDurability.getValue() && stack.isDamageable()) {
                    int dur = stack.getMaxDamage() - stack.getDamage();
                    int max = stack.getMaxDamage();
                    int pct = (int) (100f * dur / max);
                    int color = pct > 50 ? 0x55FF55 : pct > 25 ? 0xFFFF55 : 0xFF5555;
                    tr.draw(ms, dur + "/" + max, x + 20, y + 4, color);
                }
            }
            y += 20;
        }
    }
}