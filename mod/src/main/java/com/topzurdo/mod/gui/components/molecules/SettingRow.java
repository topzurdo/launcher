package com.topzurdo.mod.gui.components.molecules;

import com.topzurdo.mod.gui.GuiUtil;
import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.components.atoms.ColorPicker;
import com.topzurdo.mod.gui.components.atoms.Selector;
import com.topzurdo.mod.gui.components.atoms.Slider;
import com.topzurdo.mod.gui.components.atoms.TextInput;
import com.topzurdo.mod.gui.components.atoms.Toggle;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;

/**
 * Row for displaying a module setting.
 * Creates the appropriate control (Toggle, Slider, ColorPicker, Selector, TextInput) from the Setting.
 */
public class SettingRow implements com.topzurdo.mod.gui.UIComponent {

    private int x, y, width, height;
    private static final int DEFAULT_HEIGHT = 36;
    private static final int MIN_ROW_WIDTH = 250;
    private static final int LABEL_ZONE_WIDTH = 130;
    private static final int CONTROL_GAP = 12;
    private static final int MIN_CONTROL_WIDTH = 118;
    /** Vertical layout: label on first line, control full width on second. */
    private static final int VERTICAL_CONTROL_OFFSET = 28;
    private static final int VERTICAL_ROW_HEIGHT = 56;

    private final Setting<?> setting;
    private final Runnable onChanged;
    /** When true, label and desc are on top, control uses full width below. */
    private boolean verticalLayout;
    /** Height taken by description lines (wrap). */
    private int descHeightPx;

    private Toggle toggle;
    private Slider slider;
    private ColorPicker colorPicker;
    private Selector selector;
    private TextInput textInput;

    public SettingRow(int x, int y, int width, Setting<?> setting) {
        this(x, y, width, setting, null);
    }

    @SuppressWarnings("unchecked")
    public SettingRow(int x, int y, int width, Setting<?> setting, Runnable onChanged) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = DEFAULT_HEIGHT;
        this.setting = setting;
        this.onChanged = onChanged;
        initControl();
    }

    @SuppressWarnings("unchecked")
    private void initControl() {
        if (setting == null) return;
        verticalLayout = width < MIN_ROW_WIDTH || (width - LABEL_ZONE_WIDTH - CONTROL_GAP) < MIN_CONTROL_WIDTH;
        int descWidth = verticalLayout ? (width - 10) : (LABEL_ZONE_WIDTH - 10);
        int descLines = 0;
        if (setting.getDescription() != null && !setting.getDescription().isEmpty()) {
            net.minecraft.client.font.TextRenderer tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            descLines = GuiUtil.wrapHint(tr, setting.getDescription(), descWidth).size();
        }
        descHeightPx = descLines * (net.minecraft.client.MinecraftClient.getInstance().textRenderer.fontHeight + 2);

        int controlX = verticalLayout ? x : x + LABEL_ZONE_WIDTH;
        int controlW = verticalLayout ? width : Math.max(MIN_CONTROL_WIDTH, width - LABEL_ZONE_WIDTH - CONTROL_GAP);
        int controlY = verticalLayout ? y + 18 + descHeightPx : y + 6;

        if (setting.isBoolean()) {
            toggle = new Toggle(controlX + controlW - 40, controlY, (Boolean) setting.getValue(), v -> {
                ((Setting<Boolean>) setting).setValue(v);
                if (onChanged != null) onChanged.run();
            });
        } else if (setting.isColor()) {
            int colorVal = setting.getValue() instanceof Integer ? (Integer) setting.getValue() : 0xFFFFFFFF;
            colorPicker = new ColorPicker(controlX, controlY, controlW, 24, setting.getName(), colorVal, v -> {
                ((Setting<Integer>) setting).setValue(v);
                if (onChanged != null) onChanged.run();
            });
        } else if (setting.isFloat() && setting.hasRange()) {
            Setting<Float> fs = (Setting<Float>) setting;
            Number min = fs.getMin();
            Number max = fs.getMax();
            slider = new Slider(controlX, controlY, controlW, fs.getValue(),
                min != null ? min.floatValue() : 0f,
                max != null ? max.floatValue() : 1f,
                v -> { fs.setValue(v); if (onChanged != null) onChanged.run(); });
        } else if (setting.isInteger() && setting.hasRange()) {
            Setting<Integer> is = (Setting<Integer>) setting;
            Number min = is.getMin();
            Number max = is.getMax();
            slider = new Slider(controlX, controlY, controlW, is.getValue().floatValue(),
                min != null ? min.floatValue() : 0f,
                max != null ? max.floatValue() : 100f,
                v -> { is.setValue(v.intValue()); if (onChanged != null) onChanged.run(); });
        } else if (setting.isOptions()) {
            Setting<String> ss = (Setting<String>) setting;
            List<String> opts = ss.getOptions();
            String val = ss.getValue();
            if (val == null || !opts.contains(val)) val = opts.isEmpty() ? "" : opts.get(0);
            selector = new Selector(controlX, controlY, controlW, 24, "", opts, val, v -> {
                ss.setValue(v);
                if (onChanged != null) onChanged.run();
            });
        } else if (setting.isString()) {
            String val = (String) setting.getValue();
            if (val == null) val = "";
            textInput = new TextInput(controlX, controlY, controlW, 24, "", val, 256, v -> {
                ((Setting<String>) setting).setValue(v);
                if (onChanged != null) onChanged.run();
            });
        }

        if (verticalLayout) {
            height = VERTICAL_ROW_HEIGHT + descHeightPx;
        } else {
            if (toggle != null) height = Math.max(height, 28);
            if (slider != null) height = Math.max(height, 36);
            if (colorPicker != null) height = Math.max(height, 32);
            if (selector != null) height = Math.max(height, 32);
            if (textInput != null) height = Math.max(height, 32);
            height = Math.max(height, 18 + descHeightPx + (toggle != null ? 28 : slider != null ? 36 : 32));
        }
    }

    public void render(MatrixStack ms, int mouseX, int mouseY) {
        render(ms, mouseX, mouseY, 0f);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        if (setting == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        int labelMaxW = verticalLayout ? (width - 10) : (LABEL_ZONE_WIDTH - 10);
        String name = setting.getName();
        if (tr.getWidth(name) > labelMaxW) name = GuiUtil.truncate(tr, name, labelMaxW);
        tr.draw(ms, name, x + 5, y + 6, com.topzurdo.mod.gui.theme.DesignTokens.fgPrimary());

        // Описания: перенос по словам, цвет #94A3B8 (TEXT_MUTED), без обрезки
        String desc = setting.getDescription();
        if (desc != null && !desc.isEmpty()) {
            int descWrapW = verticalLayout ? (width - 10) : (LABEL_ZONE_WIDTH - 10);
            java.util.List<String> descLines = GuiUtil.wrapHint(tr, desc, descWrapW);
            int lineHeight = tr.fontHeight + 2;
            for (int i = 0; i < descLines.size(); i++) {
                tr.draw(ms, descLines.get(i), x + 5, y + 18 + i * lineHeight, com.topzurdo.mod.gui.theme.DesignTokens.fgMuted());
            }
        }

        if (toggle != null) toggle.render(ms, mouseX, mouseY, delta);
        if (slider != null) slider.render(ms, mouseX, mouseY, delta);
        if (colorPicker != null) colorPicker.render(ms, mouseX, mouseY, delta);
        if (selector != null) selector.render(ms, mouseX, mouseY, delta);
        if (textInput != null) textInput.render(ms, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (toggle != null && toggle.mouseClicked(mouseX, mouseY, button)) return true;
        if (slider != null && slider.mouseClicked(mouseX, mouseY, button)) return true;
        if (colorPicker != null && colorPicker.mouseClicked(mouseX, mouseY, button)) return true;
        if (selector != null && selector.mouseClicked(mouseX, mouseY, button)) return true;
        if (textInput != null && textInput.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (slider != null && slider.mouseReleased(mouseX, mouseY, button)) return true;
        if (colorPicker != null && colorPicker.mouseReleased(mouseX, mouseY, button)) return true;
        if (selector != null && selector.mouseReleased(mouseX, mouseY, button)) return true;
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (slider != null && slider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        if (colorPicker != null && colorPicker.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (selector != null && selector.mouseScrolled(mouseX, mouseY, amount)) return true;
        if (slider != null && slider.mouseScrolled(mouseX, mouseY, amount)) return true;
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (textInput != null && textInput.charTyped(chr, modifiers)) return true;
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textInput != null && textInput.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        verticalLayout = width < MIN_ROW_WIDTH || (width - LABEL_ZONE_WIDTH - CONTROL_GAP) < MIN_CONTROL_WIDTH;
        int controlX = verticalLayout ? x : x + LABEL_ZONE_WIDTH;
        int controlW = verticalLayout ? width : Math.max(MIN_CONTROL_WIDTH, width - LABEL_ZONE_WIDTH - CONTROL_GAP);
        int controlY = verticalLayout ? y + 18 + descHeightPx : y + 6;
        if (toggle != null) toggle.setPosition(controlX + controlW - 40, controlY);
        if (slider != null) slider.setPosition(controlX, controlY);
        if (colorPicker != null) colorPicker.setPosition(controlX, controlY);
        if (selector != null) selector.setPosition(controlX, controlY);
        if (textInput != null) textInput.setPosition(controlX, controlY);
    }

    public void tick() {
        if (toggle != null) {
            toggle.setValue((Boolean) setting.getValue());
            toggle.tick();
        }
        if (slider != null && setting.isFloat() && setting.hasRange()) {
            slider.setValue(((Setting<Float>) setting).getValue());
        }
        if (slider != null && setting.isInteger() && setting.hasRange()) {
            slider.setValue(((Setting<Integer>) setting).getValue().floatValue());
        }
        if (selector != null && setting.isOptions()) {
            String v = ((Setting<String>) setting).getValue();
            if (v != null && !v.equals(selector.getValue())) selector.setValue(v);
        }
        if (colorPicker != null && setting.isColor()) {
            int cv = setting.getValue() instanceof Integer ? (Integer) setting.getValue() : 0xFFFFFFFF;
            if (colorPicker.getValue() != cv) colorPicker.setValue(cv);
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Setting<?> getSetting() { return setting; }

    /** For hover/focus logic in screen. Returns first non-null control. */
    public com.topzurdo.mod.gui.UIComponent getControl() {
        if (toggle != null) return toggle;
        if (slider != null) return slider;
        if (colorPicker != null) return colorPicker;
        if (selector != null) return selector;
        if (textInput != null) return textInput;
        return null;
    }
}
