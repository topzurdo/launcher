package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

/**
 * Zoom Module - optical zoom
 */
public class ZoomModule extends Module {

    private Setting<Float> zoomFactor;
    private Setting<Boolean> smoothZoom;
    private Setting<Float> zoomSpeed;
    private Setting<Boolean> scrollZoom;

    private float currentZoom = 1.0f;
    private boolean zooming = false;

    public ZoomModule() {
        super("zoom", "Zoom", "Приближение", Category.RENDER);

        zoomFactor = addSetting(Setting.ofFloat("zoom_factor", "Множитель", "Сила приближения", 4.0f, 2.0f, 10.0f));
        smoothZoom = addSetting(Setting.ofBoolean("smooth_zoom", "Плавный зум", "Плавное приближение", true));
        zoomSpeed = addSetting(Setting.ofFloat("zoom_speed", "Скорость", "Скорость приближения", 0.1f, 0.01f, 0.5f));
        scrollZoom = addSetting(Setting.ofBoolean("scroll_zoom", "Скролл", "Регулировка колёсиком", true));
    }

    public float getZoomFactor() { return zoomFactor.getValue(); }
    public boolean isSmoothZoom() { return smoothZoom.getValue(); }
    public float getZoomSpeed() { return zoomSpeed.getValue(); }
    public boolean isScrollZoom() { return scrollZoom.getValue(); }

    public void setZooming(boolean z) { zooming = z; }
    public boolean isZooming() { return zooming; }
    public float getCurrentZoom() { return currentZoom; }
    public void setCurrentZoom(float z) { currentZoom = z; }
}
