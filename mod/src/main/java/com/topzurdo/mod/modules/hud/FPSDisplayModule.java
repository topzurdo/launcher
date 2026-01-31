package com.topzurdo.mod.modules.hud;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * FPS Display Module - shows current FPS
 */
public class FPSDisplayModule extends Module {

    private Setting<Integer> posX;
    private Setting<Integer> posY;
    private Setting<Boolean> colorByFps;

    /**
     * Constructs the FPS display module and registers its HUD configuration settings.
     *
     * <p>Registers these settings with their defaults and constraints:
     * <ul>
     *   <li>posX — horizontal position, default 900, range 0–2000</li>
     *   <li>posY — vertical position, default 10, range 0–2000</li>
     *   <li>colorByFps — whether to change text color based on FPS, default `true`</li>
     * </ul>
     */
    public FPSDisplayModule() {
        super("fps_display", "FPS Display", "Отображение FPS", Category.HUD);

        posX = addSetting(Setting.ofInt("pos_x", "Позиция X", "Горизонтальная позиция", 900, 0, 2000));
        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 10, 0, 2000));
        colorByFps = addSetting(Setting.ofBoolean("color_by_fps", "Цвет по FPS", "Менять цвет в зависимости от FPS", true));
    }

    /**
     * Provides the HUD element's bounding rectangle.
     *
     * @return an int array [x, y, width, height] representing the HUD bounds in screen pixels
     */
    @Override
    public int[] getHudBounds() {
        return new int[] { posX.getValue(), posY.getValue(), 60, 12 };
    }

    /**
     * Renders the FPS counter on the HUD at the configured position using an optional color scheme.
     *
     * The method does nothing if the module is disabled or there is no player instance. It reads the
     * client's FPS string and falls back to 0 when empty, then draws "FPS: <value>" at (posX, posY).
     * When the color-by-FPS setting is enabled, the text color is green for FPS >= 60, yellow for FPS
     * >= 30, and red otherwise.
     *
     * @param partialTicks interpolation tick value (not used for FPS computation)
     */
    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int fps = mc.fpsDebugString.isEmpty() ? 0 : Integer.parseInt(mc.fpsDebugString.split(" ")[0]);
        int color = 0xFFFFFF;
        if (colorByFps.getValue()) {
            if (fps >= 60) color = 0x55FF55;
            else if (fps >= 30) color = 0xFFFF55;
            else color = 0xFF5555;
        }

        MatrixStack ms = new MatrixStack();
        TextRenderer tr = mc.textRenderer;
        tr.draw(ms, "FPS: " + fps, posX.getValue(), posY.getValue(), color);
    }
}