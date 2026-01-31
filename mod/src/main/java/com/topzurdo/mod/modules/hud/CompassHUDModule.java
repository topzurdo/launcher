package com.topzurdo.mod.modules.hud;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Compass HUD Module - directional compass
 */
public class CompassHUDModule extends Module {

    private Setting<Integer> posY;
    private Setting<Boolean> showDegrees;

    /**
     * Constructs the Compass HUD module and registers its configurable settings.
     *
     * Initializes the module metadata (id, display name, tooltip, category) and registers:
     * - an integer setting "pos_y" (vertical position) with default 5 and range 0–500,
     * - a boolean setting "show_degrees" that controls rendering of yaw degrees (default true).
     */
    public CompassHUDModule() {
        super("compass_hud", "Compass HUD", "Компас", Category.HUD);

        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 5, 0, 500));
        showDegrees = addSetting(Setting.ofBoolean("show_degrees", "Градусы", "Показывать градусы", true));
    }

    /**
     * Compute the HUD bounding rectangle for the compass, centered horizontally and positioned using the module's vertical setting.
     *
     * @return an int array in the form {@code {x, y, width, height}} describing the bounds to render the HUD, or {@code null} if the game window is unavailable.
     */
    @Override
    public int[] getHudBounds() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return null;
        int w = mc.getWindow().getScaledWidth();
        int cx = w / 2;
        int y = posY.getValue();
        return new int[] { cx - 40, y, 80, 30 };
    }

    /**
     * Render the on-screen compass HUD centered horizontally at the configured vertical position.
     *
     * Draws visible cardinal and intercardinal labels relative to the player's current yaw and,
     * when enabled, renders the yaw as an integer degree string below the compass.
     *
     * @param partialTicks Fractional tick time for the current render frame.
     */
    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getWindow() == null) return;

        float yaw = mc.player.yaw % 360;
        if (yaw < 0) yaw += 360;

        int w = mc.getWindow().getScaledWidth();
        int cx = w / 2;
        int y = posY.getValue();

        MatrixStack ms = new MatrixStack();
        TextRenderer tr = mc.textRenderer;

        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        for (int i = 0; i < dirs.length; i++) {
            float dirAngle = i * 45;
            float diff = yaw - dirAngle;
            if (diff > 180) diff -= 360;
            if (diff < -180) diff += 360;

            if (Math.abs(diff) < 60) {
                int x = cx - (int)(diff * 2);
                int color = dirs[i].length() == 1 ? 0xFFFFFF : 0xAAAAAA;
                tr.draw(ms, dirs[i], x - tr.getWidth(dirs[i]) / 2, y, color);
            }
        }

        if (showDegrees.getValue()) {
            tr.draw(ms, String.format("%.0f°", yaw), cx - 10, y + 12, 0xFFFFFF);
        }
    }
}