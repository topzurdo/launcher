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

    /**
     * Creates the Coordinates HUD module and registers its configurable settings.
     *
     * <p>Initializes and registers the following settings:
     * <ul>
     *   <li>{@code posX} — horizontal HUD position (default 10, range 0 to 2000)</li>
     *   <li>{@code posY} — vertical HUD position (default 380, range 0 to 2000)</li>
     *   <li>{@code showNether} — toggle Nether/Overworld coordinate display (default true)</li>
     *   <li>{@code showBiome} — toggle current biome display (default false)</li>
     *   <li>{@code showDirection} — toggle facing direction display (default true)</li>
     * </ul>
     */
    public CoordinatesModule() {
        super("coordinates", "Coordinates", "Координаты игрока", Category.HUD);

        posX = addSetting(Setting.ofInt("pos_x", "Позиция X", "Горизонтальная позиция", 10, 0, 2000));
        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 380, 0, 2000));
        showNether = addSetting(Setting.ofBoolean("show_nether", "Координаты Незера", "Показывать координаты для Незера", true));
        showBiome = addSetting(Setting.ofBoolean("show_biome", "Биом", "Показывать текущий биом", false));
        showDirection = addSetting(Setting.ofBoolean("show_direction", "Направление", "Показывать направление", true));
    }

    /**
     * Provide the HUD bounds rectangle based on the current X/Y settings.
     *
     * @return an int array [x, y, width, height] representing the HUD position and fixed size (180, 40)
     */
    @Override
    public int[] getHudBounds() {
        int x = posX.getValue();
        int y = posY.getValue();
        return new int[] { x, y, 180, 40 };
    }

    /**
     * Renders the coordinates HUD including player XYZ, optional Nether/Overworld coordinates, and facing direction.
     *
     * Displays the player's current X/Y/Z at the module's configured HUD position and, when enabled, the corresponding
     * Nether or Overworld coordinates (transformed by factor 8) and the cardinal facing direction derived from player yaw.
     * Rendering is skipped if the module is disabled or if the Minecraft player or world is unavailable. Position and
     * visibility are controlled by the module settings (posX, posY, showNether, showDirection).
     *
     * @param partialTicks the frame interpolation fraction used for rendering
     */
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
            float yaw = mc.player.yaw % 360;
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