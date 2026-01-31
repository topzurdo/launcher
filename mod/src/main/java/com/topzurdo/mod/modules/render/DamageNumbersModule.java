package com.topzurdo.mod.modules.render;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Damage Numbers Module - floating damage/heal numbers above entities.
 * Records damage/heal from LivingEntityMixin and renders in world.
 */
public class DamageNumbersModule extends Module {

    private Setting<Integer> color;
    private Setting<Float> scale;
    private Setting<Float> duration;
    private Setting<Boolean> showHeal;

    private static final int HEAL_COLOR = 0x00FF00;
    private final CopyOnWriteArrayList<FloatingNumber> numbers = new CopyOnWriteArrayList<>();

    public DamageNumbersModule() {
        super("damage_numbers", "Damage Numbers", "Числа урона над мобами", Category.RENDER);

        color = addSetting(Setting.ofInt("color", "Цвет урона", "Цвет чисел урона", 0xFF0000, 0, 0xFFFFFF));
        scale = addSetting(Setting.ofFloat("scale", "Масштаб", "Размер чисел", 1.0f, 0.5f, 2.0f));
        duration = addSetting(Setting.ofFloat("duration", "Длительность", "Время показа", 1.0f, 0.5f, 3.0f));
        showHeal = addSetting(Setting.ofBoolean("show_heal", "Показывать исцеление", "Зелёные числа при хиле", true));
    }

    public int getColor() { return color.getValue(); }
    public float getScale() { return scale.getValue(); }
    public float getDuration() { return duration.getValue(); }
    public boolean showHeal() { return showHeal.getValue(); }

    /** Called from LivingEntityMixin when entity takes damage. */
    public void recordDamage(LivingEntity entity, float amount, boolean isHeal) {
        if (!isEnabled()) return;
        if (isHeal && !showHeal()) return;
        if (amount <= 0) return;
        Vec3d pos = entity.getPos();
        long endMs = System.currentTimeMillis() + (long) (getDuration() * 1000);
        numbers.add(new FloatingNumber(pos.x, pos.y + entity.getHeight() * 0.9, pos.z, amount, isHeal, endMs));
    }

    /** Called from LivingEntityMixin when entity is healed (setHealth increase). */
    public void recordHeal(LivingEntity entity, float amount) {
        recordDamage(entity, amount, true);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        numbers.clear();
    }

    /**
     * Called from WorldRenderEvents.END to draw numbers in world.
     */
    public void onWorldRender(MatrixStack matrices, float tickDelta) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.textRenderer == null) return;

        long now = System.currentTimeMillis();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        float scaleF = 0.02f * getScale();

        Iterator<FloatingNumber> it = numbers.iterator();
        while (it.hasNext()) {
            FloatingNumber n = it.next();
            if (now >= n.endTimeMs) {
                it.remove();
                continue;
            }
            float age = (n.endTimeMs - now) / 1000f / getDuration();
            float alpha = MathHelper.clamp(age, 0f, 1f);
            int a = (int) (alpha * 255) << 24;
            int c = n.isHeal ? HEAL_COLOR : getColor();
            int color = a | (c & 0xFFFFFF);

            matrices.push();
            matrices.translate(n.x - camPos.x, n.y - camPos.y, n.z - camPos.z);
            matrices.scale(scaleF, scaleF, scaleF);
            matrices.multiply(camera.getRotation());
            matrices.translate(0, 0, -0.1f);
            String text = n.isHeal ? "+" + formatNum(n.amount) : "-" + formatNum(n.amount);
            int w = mc.textRenderer.getWidth(text);
            mc.textRenderer.drawWithShadow(matrices, text, -w / 2f, 0, color);
            matrices.pop();
        }
    }

    private static String formatNum(float amount) {
        if (amount == (int) amount) return String.valueOf((int) amount);
        return String.format("%.1f", amount);
    }

    private static class FloatingNumber {
        final double x, y, z;
        final float amount;
        final boolean isHeal;
        final long endTimeMs;

        FloatingNumber(double x, double y, double z, float amount, boolean isHeal, long endTimeMs) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.amount = amount;
            this.isHeal = isHeal;
            this.endTimeMs = endTimeMs;
        }
    }
}
