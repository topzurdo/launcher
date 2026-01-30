package com.topzurdo.mod.modules.hud;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Coordinates Module - displays player coordinates
 */
public class CoordinatesModule extends Module {

    private Setting<Integer> posX;
    private Setting<Integer> posY;
    private Setting<Boolean> showNether;
    private Setting<Boolean> showBiome;
    private Setting<Boolean> showDirection;

    public CoordinatesModule() {
        super("coordinates", "Coordinates", "Координаты игрока", Category.HUD);

        posX = addSetting(Setting.ofInt("pos_x", "Позиция X", "Горизонтальная позиция", 10, 0, 500));
        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 10, 0, 500));
        showNether = addSetting(Setting.ofBoolean("show_nether", "Координаты Незера", "Показывать координаты для Незера", true));
        showBiome = addSetting(Setting.ofBoolean("show_biome", "Биом", "Показывать текущий биом", false));
        showDirection = addSetting(Setting.ofBoolean("show_direction", "Направление", "Показывать направление", true));
    }

    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        int x = posX.getValue();
        int y = posY.getValue();
        MatrixStack ms = new MatrixStack();
        TextRenderer tr = mc.textRenderer;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        tr.draw(ms, String.format("XYZ: %.1f / %.1f / %.1f", px, py, pz), x, y, 0xFFFFFF);
        y += 10;

        if (showNether.getValue()) {
            boolean inNether = mc.world.getRegistryKey().getValue().getPath().contains("nether");
            double nx = inNether ? px * 8 : px / 8;
            double nz = inNether ? pz * 8 : pz / 8;
            String label = inNether ? "Overworld" : "Nether";
            tr.draw(ms, String.format("%s: %.1f / %.1f", label, nx, nz), x, y, 0xAAAAAA);
            y += 10;
        }

        if (showDirection.getValue()) {
            float yaw = mc.player.getYaw() % 360;
            if (yaw < 0) yaw += 360;
            String dir;
            if (yaw >= 315 || yaw < 45) dir = "South (+Z)";
            else if (yaw >= 45 && yaw < 135) dir = "West (-X)";
            else if (yaw >= 135 && yaw < 225) dir = "North (-Z)";
            else dir = "East (+X)";
            tr.draw(ms, "Facing: " + dir, x, y, 0xFFFFFF);
        }
    }
}
