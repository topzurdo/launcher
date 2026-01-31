package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Hit Markers Module - visual hit indicators
 */
public class HitMarkersModule extends Module {

    private Setting<Integer> color;
    private Setting<Float> duration;
    private Setting<Integer> size;
    private Setting<Boolean> sound;

    private final List<HitMarker> markers = new ArrayList<>();

    public HitMarkersModule() {
        super("hit_markers", "Hit Markers", "Индикатор попаданий", Category.RENDER);

        color = addSetting(Setting.ofInt("color", "Цвет", "Цвет маркера", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE));
        duration = addSetting(Setting.ofFloat("duration", "Длительность", "Время показа (сек)", 0.5f, 0.1f, 2.0f));
        size = addSetting(Setting.ofInt("size", "Размер", "Размер маркера", 8, 4, 20));
        sound = addSetting(Setting.ofBoolean("sound", "Звук", "Звук при попадании", true));
    }

    public void addMarker() {
        markers.add(new HitMarker(System.currentTimeMillis()));
    }

    public void onPlayerAttack() {
        if (isEnabled()) {
            addMarker();
        }
    }

    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getWindow() == null) return;

        long now = System.currentTimeMillis();
        long durationMs = (long) (duration.getValue() * 1000);

        // Remove expired markers
        Iterator<HitMarker> it = markers.iterator();
        while (it.hasNext()) {
            if (now - it.next().time > durationMs) it.remove();
        }

        if (markers.isEmpty()) return;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int cx = w / 2;
        int cy = h / 2;
        int s = size.getValue();

        MatrixStack ms = new MatrixStack();
        for (HitMarker m : markers) {
            float alpha = 1f - (float)(now - m.time) / durationMs;
            int c = color.getValue();
            int a = (int) (((c >> 24) & 0xFF) * alpha);
            int col = (a << 24) | (c & 0x00FFFFFF);

            // X shape
            for (int i = 0; i < s; i++) {
                DrawableHelper.fill(ms, cx - s + i, cy - s + i, cx - s + i + 2, cy - s + i + 2, col);
                DrawableHelper.fill(ms, cx + s - i - 2, cy - s + i, cx + s - i, cy - s + i + 2, col);
                DrawableHelper.fill(ms, cx - s + i, cy + s - i - 2, cx - s + i + 2, cy + s - i, col);
                DrawableHelper.fill(ms, cx + s - i - 2, cy + s - i - 2, cx + s - i, cy + s - i, col);
            }
        }
    }

    private static class HitMarker {
        final long time;
        HitMarker(long time) { this.time = time; }
    }
}
