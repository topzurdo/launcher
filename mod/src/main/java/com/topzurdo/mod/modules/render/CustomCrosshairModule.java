package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Custom Crosshair Module - customizable crosshair
 * Works with InGameHudMixin to hide vanilla crosshair
 */
public class CustomCrosshairModule extends Module {

    private Setting<String> style;
    private Setting<Integer> size;
    private Setting<Integer> gap;
    private Setting<Integer> thickness;
    private Setting<Integer> color;
    private Setting<Boolean> dot;
    private Setting<Boolean> outline;

    public CustomCrosshairModule() {
        super("custom_crosshair", "Custom Crosshair", "Кастомный прицел", Category.RENDER);

        style = addSetting(Setting.ofOptions("style", "Стиль", "Форма прицела", "cross", "cross", "circle", "dot"));
        size = addSetting(Setting.ofInt("size", "Размер", "Размер прицела", 6, 1, 20));
        gap = addSetting(Setting.ofInt("gap", "Промежуток", "Расстояние от центра", 2, 0, 10));
        thickness = addSetting(Setting.ofInt("thickness", "Толщина", "Толщина линий", 1, 1, 5));
        color = addSetting(Setting.ofInt("color", "Цвет", "Цвет прицела (ARGB)", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE));
        dot = addSetting(Setting.ofBoolean("dot", "Точка", "Показывать точку в центре", false));
        outline = addSetting(Setting.ofBoolean("outline", "Обводка", "Чёрная обводка", true));
    }

    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getWindow() == null) return;
        if (mc.options.hudHidden) return;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        MatrixStack ms = new MatrixStack();
        int c = color.getValue();
        int s = size.getValue();
        int g = gap.getValue();
        int t = thickness.getValue();

        String st = style.getValue();
        if ("cross".equals(st)) {
            // Draw cross
            if (outline.getValue()) {
                int black = 0xFF000000;
                // Horizontal outline
                DrawableHelper.fill(ms, cx - s - 1, cy - t / 2 - 1, cx - g + 1, cy + t / 2 + 2, black);
                DrawableHelper.fill(ms, cx + g - 1, cy - t / 2 - 1, cx + s + 1, cy + t / 2 + 2, black);
                // Vertical outline
                DrawableHelper.fill(ms, cx - t / 2 - 1, cy - s - 1, cx + t / 2 + 2, cy - g + 1, black);
                DrawableHelper.fill(ms, cx - t / 2 - 1, cy + g - 1, cx + t / 2 + 2, cy + s + 1, black);
            }
            // Horizontal lines
            DrawableHelper.fill(ms, cx - s, cy - t / 2, cx - g, cy + t / 2 + 1, c);
            DrawableHelper.fill(ms, cx + g, cy - t / 2, cx + s, cy + t / 2 + 1, c);
            // Vertical lines
            DrawableHelper.fill(ms, cx - t / 2, cy - s, cx + t / 2 + 1, cy - g, c);
            DrawableHelper.fill(ms, cx - t / 2, cy + g, cx + t / 2 + 1, cy + s, c);
        } else if ("dot".equals(st)) {
            if (outline.getValue()) {
                DrawableHelper.fill(ms, cx - t - 1, cy - t - 1, cx + t + 2, cy + t + 2, 0xFF000000);
            }
            DrawableHelper.fill(ms, cx - t, cy - t, cx + t + 1, cy + t + 1, c);
        }

        if (dot.getValue() && !"dot".equals(st)) {
            DrawableHelper.fill(ms, cx, cy, cx + 1, cy + 1, c);
        }
    }

    public void onPlayerAttack() {
        // Could add animation/effect here
    }
}
