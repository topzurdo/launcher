package com.topzurdo.mod.gui.components.molecules;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.components.atoms.Selector;
import com.topzurdo.mod.gui.components.atoms.Slider;
import com.topzurdo.mod.gui.components.atoms.Toggle;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Row for displaying a module setting
 */
public class SettingRow {

    private int x, y, width;
    private static final int HEIGHT = 32;
    private Setting<?> setting;

    private Toggle toggle;
    private Slider slider;
    private Selector selector;

    @SuppressWarnings("unchecked")
    public SettingRow(int x, int y, int width, Setting<?> setting) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.setting = setting;

        initControl();
    }

    @SuppressWarnings("unchecked")
    private void initControl() {
        int controlX = x + width - 100;
        int controlY = y + 7;

        if (setting.getType() == Setting.Type.BOOLEAN) {
            toggle = new Toggle(controlX + 64, controlY, (Boolean) setting.getValue(), v -> {
                ((Setting<Boolean>) setting).setValue(v);
            });
        } else if (setting.getType() == Setting.Type.FLOAT) {
            Setting<Float> fs = (Setting<Float>) setting;
            slider = new Slider(controlX - 30, controlY, 90, fs.getValue(), fs.getMin(), fs.getMax(), v -> {
                fs.setValue(v);
            });
        } else if (setting.getType() == Setting.Type.INT) {
            Setting<Integer> is = (Setting<Integer>) setting;
            slider = new Slider(controlX - 30, controlY, 90, is.getValue().floatValue(), is.getMin().floatValue(), is.getMax().floatValue(), v -> {
                is.setValue(v.intValue());
            });
        } else if (setting.getType() == Setting.Type.OPTIONS) {
            Setting<String> ss = (Setting<String>) setting;
            String[] opts = ss.getOptions();
            selector = new Selector(controlX - 30, controlY, 130, ss.getValue(), opts, v -> {
                ss.setValue(v);
            });
        }
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        tr.draw(ms, setting.getName(), x + 5, y + 8, OceanTheme.TEXT_PRIMARY);
        tr.draw(ms, setting.getDescription(), x + 5, y + 20, OceanTheme.TEXT_MUTED);

        if (toggle != null) toggle.render(ms, mouseX, mouseY, delta);
        if (slider != null) slider.render(ms, mouseX, mouseY, delta);
        if (selector != null) selector.render(ms, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (toggle != null && toggle.mouseClicked(mouseX, mouseY, button)) return true;
        if (slider != null && slider.mouseClicked(mouseX, mouseY, button)) return true;
        if (selector != null && selector.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (slider != null && slider.mouseReleased(mouseX, mouseY, button)) return true;
        if (selector != null && selector.mouseReleased(mouseX, mouseY, button)) return true;
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (slider != null && slider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (selector != null && selector.mouseScrolled(mouseX, mouseY, amount)) return true;
        return false;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        initControl();
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return HEIGHT; }
    public Setting<?> getSetting() { return setting; }
}
