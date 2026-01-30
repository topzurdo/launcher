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

    public FPSDisplayModule() {
        super("fps_display", "FPS Display", "Отображение FPS", Category.HUD);

        posX = addSetting(Setting.ofInt("pos_x", "Позиция X", "Горизонтальная позиция", 10, 0, 500));
        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 30, 0, 500));
        colorByFps = addSetting(Setting.ofBoolean("color_by_fps", "Цвет по FPS", "Менять цвет в зависимости от FPS", true));
    }

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
