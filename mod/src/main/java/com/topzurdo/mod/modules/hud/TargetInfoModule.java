package com.topzurdo.mod.modules.hud;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Target Info Module - displays information about targeted entity
 */
public class TargetInfoModule extends Module {

    private Setting<Integer> posX;
    private Setting<Integer> posY;
    private Setting<Boolean> showHealth;
    private Setting<Boolean> showDistance;

    /**
     * Creates a TargetInfoModule HUD element and registers its configurable settings.
     *
     * Initializes position and display settings used to render target information on screen:
     * - pos_x: horizontal HUD position, default 450, range 0–2000
     * - pos_y: vertical HUD position, default 200, range 0–2000
     * - show_health: whether to display target health, default true
     * - show_distance: whether to display target distance, default true
     */
    public TargetInfoModule() {
        super("target_info", "Target Info", "Информация о цели", Category.HUD);

        posX = addSetting(Setting.ofInt("pos_x", "Позиция X", "Горизонтальная позиция", 450, 0, 2000));
        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 200, 0, 2000));
        showHealth = addSetting(Setting.ofBoolean("show_health", "Здоровье", "Показывать здоровье", true));
        showDistance = addSetting(Setting.ofBoolean("show_distance", "Дистанция", "Показывать расстояние", true));
    }

    /**
     * Provides the HUD element's on-screen bounds as [x, y, width, height].
     *
     * @return an int array with four elements: x position, y position, width (120), and height (40)
     */
    @Override
    public int[] getHudBounds() {
        return new int[] { posX.getValue(), posY.getValue(), 120, 40 };
    }

    /**
     * Renders the on-screen HUD for the currently targeted entity, showing its name and optional health and distance lines.
     *
     * This method performs no rendering if the module is disabled, there is no crosshair target, the hit result is not an entity,
     * or the targeted entity is not a living entity.
     *
     * @param partialTicks the partial tick time used for rendering interpolation
     */
    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.crosshairTarget == null) return;

        HitResult hit = mc.crosshairTarget;
        if (hit.getType() != HitResult.Type.ENTITY) return;
        if (!(hit instanceof EntityHitResult)) return;

        EntityHitResult entityHit = (EntityHitResult) hit;
        if (!(entityHit.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) entityHit.getEntity();

        int x = posX.getValue();
        int y = posY.getValue();
        MatrixStack ms = new MatrixStack();
        TextRenderer tr = mc.textRenderer;

        tr.draw(ms, target.getName().getString(), x, y, 0xFFFFFF);
        y += 10;

        if (showHealth.getValue()) {
            float hp = target.getHealth();
            float maxHp = target.getMaxHealth();
            int pct = (int) (100f * hp / maxHp);
            int color = pct > 50 ? 0x55FF55 : pct > 25 ? 0xFFFF55 : 0xFF5555;
            tr.draw(ms, String.format("HP: %.1f / %.1f", hp, maxHp), x, y, color);
            y += 10;
        }

        if (showDistance.getValue()) {
            double dist = mc.player.distanceTo(target);
            tr.draw(ms, String.format("Distance: %.1fm", dist), x, y, 0xAAAAAA);
        }
    }
}