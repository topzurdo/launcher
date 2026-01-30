package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

/**
 * Damage Numbers Module - floating damage numbers
 */
public class DamageNumbersModule extends Module {

    private Setting<Integer> color;
    private Setting<Float> scale;
    private Setting<Float> duration;
    private Setting<Boolean> showHeal;

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
}
