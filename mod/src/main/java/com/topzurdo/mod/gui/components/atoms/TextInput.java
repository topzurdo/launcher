package com.topzurdo.mod.gui.components.atoms;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import com.topzurdo.mod.gui.theme.DesignTokens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Text input field component
 */
public class TextInput implements com.topzurdo.mod.gui.UIComponent {

    private int x, y, width, height;
    private String text;
    private String placeholder;
    private boolean focused;
    private int cursorPos;
    private int maxLength;
    private Consumer<String> onChange;
    private long lastBlinkTime = 0;
    private boolean cursorVisible = true;

    public TextInput(int x, int y, int width, int height, String placeholder) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = "";
        this.placeholder = placeholder;
        this.focused = false;
        this.cursorPos = 0;
        this.maxLength = 32;
    }

    /**
     * Extended constructor with initial value, max length and onChange callback
     */
    public TextInput(int x, int y, int width, int height, String placeholder, String initialValue, int maxLength, java.util.function.Consumer<String> onChange) {
        this(x, y, width, height, placeholder);
        this.text = initialValue != null ? initialValue : "";
        this.maxLength = maxLength;
        this.onChange = onChange;
        this.cursorPos = this.text.length();
    }

    /**
     * Render without delta (for compatibility)
     */
    public void render(MatrixStack ms, int mouseX, int mouseY) {
        render(ms, mouseX, mouseY, 0f);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        boolean hover = !focused && isMouseOver(mouseX, mouseY);
        int bgColor = focused ? OceanTheme.BG_ELEVATED : (hover ? UIRenderHelper.withAlpha(OceanTheme.ACCENT, 0.08f) : OceanTheme.BG_PANEL);
        int borderColor = focused ? OceanTheme.ACCENT : (hover ? OceanTheme.BORDER_HOVER : OceanTheme.BORDER);

        UIRenderHelper.fillRoundRect(ms, x, y, width, height, DesignTokens.RADIUS, bgColor);
        UIRenderHelper.drawRoundBorder(ms, x, y, width, height, DesignTokens.RADIUS, 1, borderColor);

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        String displayText = text.isEmpty() && !focused ? placeholder : text;
        int textColor = text.isEmpty() && !focused ? DesignTokens.fgMuted() : DesignTokens.fgPrimary();

        int textY = y + (height - 8) / 2;
        tr.draw(ms, displayText, x + 6, textY, textColor);

        // Cursor (blink every 530ms)
        long now = System.currentTimeMillis();
        if (focused) {
            if (lastBlinkTime == 0) lastBlinkTime = now;
            if (now - lastBlinkTime > 530) {
                cursorVisible = !cursorVisible;
                lastBlinkTime = now;
            }
        } else {
            cursorVisible = true;
            lastBlinkTime = 0;
        }
        if (focused && cursorVisible) {
            int cursorX = x + 6 + tr.getWidth(text.substring(0, Math.min(cursorPos, text.length())));
            UIRenderHelper.drawRect(ms, cursorX, textY - 1, 1, 10, DesignTokens.fgPrimary());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean wasInside = isMouseOver(mouseX, mouseY);
        focused = wasInside;
        return wasInside;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && cursorPos > 0) {
            text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
            cursorPos--;
            notifyChange();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE && cursorPos < text.length()) {
            text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
            notifyChange();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT && cursorPos > 0) {
            cursorPos--;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT && cursorPos < text.length()) {
            cursorPos++;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorPos = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            cursorPos = text.length();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;
        if (text.length() >= maxLength) return false;
        if (Character.isISOControl(chr)) return false;

        text = text.substring(0, cursorPos) + chr + text.substring(cursorPos);
        cursorPos++;
        notifyChange();
        return true;
    }

    private void notifyChange() {
        if (onChange != null) onChange.accept(text);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
        this.cursorPos = this.text.length();
    }
    public String getText() { return text; }
    public void setFocused(boolean focused) { this.focused = focused; }
    public boolean isFocused() { return focused; }
    public void setOnChange(Consumer<String> onChange) { this.onChange = onChange; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
