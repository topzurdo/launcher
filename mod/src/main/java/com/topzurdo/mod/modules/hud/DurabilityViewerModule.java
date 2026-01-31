package com.topzurdo.mod.modules.hud;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

/**
 * Durability Viewer Module - shows held item durability
 */
public class DurabilityViewerModule extends Module {

    private Setting<Integer> posX;
    private Setting<Integer> posY;
    private Setting<Boolean> showPercent;
    private Setting<Boolean> warnLow;
    private Setting<Integer> warnThreshold;

    /**
     * Creates the Durability Viewer module and registers its configurable HUD settings.
     *
     * Initializes settings that control the module's screen position, display format,
     * and low-durability warning behavior:
     * - "pos_x": horizontal HUD position (default 10)
     * - "pos_y": vertical HUD position (default 100)
     * - "show_percent": display durability as a percentage (default true)
     * - "warn_low": enable low-durability warnings (default true)
     * - "warn_threshold": warning threshold in percent (default 10)
     */
    public DurabilityViewerModule() {
        super("durability_viewer", "Durability Viewer", "Прочность предмета в руке", Category.HUD);

        posX = addSetting(Setting.ofInt("pos_x", "Позиция X", "Горизонтальная позиция", 10, 0, 2000));
        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 100, 0, 2000));
        showPercent = addSetting(Setting.ofBoolean("show_percent", "Проценты", "Показывать в процентах", true));
        warnLow = addSetting(Setting.ofBoolean("warn_low", "Предупреждение", "Предупреждать о низкой прочности", true));
        warnThreshold = addSetting(Setting.ofInt("warn_threshold", "Порог", "Порог предупреждения (%)", 10, 1, 50));
    }

    /**
     * Provide the HUD element's bounding rectangle in screen coordinates.
     *
     * @return an int array [x, y, width, height] where x and y are the current HUD position and width and height are 120 and 12 respectively
     */
    @Override
    public int[] getHudBounds() {
        return new int[] { posX.getValue(), posY.getValue(), 120, 12 };
    }

    /**
     * Renders the durability HUD for the item held in the player's main hand.
     *
     * Draws the label "Прочность: " followed by either a percentage or "remaining/max" durability
     * at the configured HUD position and color. If the module is disabled, the player is null,
     * or the held item is empty or not damageable, nothing is rendered. When low-durability
     * warnings are enabled and the durability percent is at or below the configured threshold,
     * the text color changes to the warning color.
     *
     * @param partialTicks fractional tick time used for interpolation during rendering
     */
    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty() || !stack.isDamageable()) return;

        int dur = stack.getMaxDamage() - stack.getDamage();
        int max = stack.getMaxDamage();
        int pct = (int) (100f * dur / max);

        String text = showPercent.getValue() ? pct + "%" : dur + "/" + max;
        int color = 0xFFFFFF;
        if (warnLow.getValue() && pct <= warnThreshold.getValue()) {
            color = 0xFF5555;
        }

        MatrixStack ms = new MatrixStack();
        TextRenderer tr = mc.textRenderer;
        tr.draw(ms, "Прочность: " + text, posX.getValue(), posY.getValue(), color);
    }
}