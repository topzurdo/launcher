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

    public DurabilityViewerModule() {
        super("durability_viewer", "Durability Viewer", "Прочность предмета в руке", Category.HUD);

        posX = addSetting(Setting.ofInt("pos_x", "Позиция X", "Горизонтальная позиция", 8, 0, 2000));
        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 218, 0, 2000));
        showPercent = addSetting(Setting.ofBoolean("show_percent", "Проценты", "Показывать в процентах", true));
        warnLow = addSetting(Setting.ofBoolean("warn_low", "Предупреждение", "Предупреждать о низкой прочности", true));
        warnThreshold = addSetting(Setting.ofInt("warn_threshold", "Порог", "Порог предупреждения (%)", 10, 1, 50));
    }

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
