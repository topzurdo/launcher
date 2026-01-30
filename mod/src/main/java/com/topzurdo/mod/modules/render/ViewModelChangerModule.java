package com.topzurdo.mod.modules.render;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

/**
 * ViewModel Changer Module - customize held item position
 */
public class ViewModelChangerModule extends Module {

    private Setting<Float> posX;
    private Setting<Float> posY;
    private Setting<Float> posZ;
    private Setting<Float> scale;
    private Setting<Float> rotationX;
    private Setting<Float> rotationY;
    private Setting<Float> rotationZ;
    private Setting<Float> swingSpeed;

    public ViewModelChangerModule() {
        super("viewmodel_changer", "ViewModel Changer", "Настройка отображения предмета в руке", Category.RENDER);

        posX = addSetting(Setting.ofFloat("pos_x", "Позиция X", "Горизонтальное смещение", 0f, -1f, 1f));
        posY = addSetting(Setting.ofFloat("pos_y", "Позиция Y", "Вертикальное смещение", 0f, -1f, 1f));
        posZ = addSetting(Setting.ofFloat("pos_z", "Позиция Z", "Смещение вглубь", 0f, -1f, 1f));
        scale = addSetting(Setting.ofFloat("scale", "Масштаб", "Размер предмета", 1f, 0.5f, 2f));
        rotationX = addSetting(Setting.ofFloat("rotation_x", "Поворот X", "Поворот по X", 0f, -180f, 180f));
        rotationY = addSetting(Setting.ofFloat("rotation_y", "Поворот Y", "Поворот по Y", 0f, -180f, 180f));
        rotationZ = addSetting(Setting.ofFloat("rotation_z", "Поворот Z", "Поворот по Z", 0f, -180f, 180f));
        swingSpeed = addSetting(Setting.ofFloat("swing_speed", "Скорость взмаха", "Множитель скорости", 1f, 0.5f, 2f));
    }

    public float getPosX() { return posX.getValue(); }
    public float getPosY() { return posY.getValue(); }
    public float getPosZ() { return posZ.getValue(); }
    public float getScale() { return scale.getValue(); }
    public float getRotationX() { return rotationX.getValue(); }
    public float getRotationY() { return rotationY.getValue(); }
    public float getRotationZ() { return rotationZ.getValue(); }
    public float getSwingSpeed() { return swingSpeed.getValue(); }
}
