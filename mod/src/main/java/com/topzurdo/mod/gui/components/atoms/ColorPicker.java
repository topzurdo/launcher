package com.topzurdo.mod.gui.components.atoms;

import java.util.function.Consumer;

import com.topzurdo.mod.gui.GuiUtil;
import com.topzurdo.mod.gui.UIComponent;
import com.topzurdo.mod.gui.UIRenderHelper;
import com.topzurdo.mod.gui.theme.DesignTokens;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * Color Picker component with HSV gradient, preview swatch, and RGB sliders.
 * Compact design for module settings panel.
 */
public final class ColorPicker implements UIComponent {

    private int x, y;
    private final int width, height;
    private final String label;
    private final Consumer<Integer> onChange;

    // Current color (RGB)
    private int colorRGB;

    // HSV representation for gradient picking
    private float hue = 0f;
    private float saturation = 1f;
    private float brightness = 1f;

    // Dragging state
    private boolean draggingHue = false;
    private boolean draggingSV = false;

    // UI dimensions: explicit zones to avoid overlap
    /** Left zone for color swatch (px). */
    private static final int SWATCH_ZONE = 24;
    private static final int SWATCH_SIZE = 20;
    /** Right zone for hex text: full "#FFFFFF" + padding (px). */
    private static final int HEX_ZONE = 52;
    private static final int HUE_BAR_HEIGHT = 10;
    private static final int SV_BOX_SIZE = 50;
    private static final int SPACING = 4;

    // Expanded state
    private boolean expanded = false;

    public ColorPicker(int x, int y, int width, int height, String label, int initialColor, Consumer<Integer> onChange) {
        this.x = x;
        this.y = y;
        this.width = Math.max(120, width);
        this.height = Math.max(24, height);
        this.label = label != null ? label : "";
        this.colorRGB = initialColor;
        this.onChange = onChange;

        // Initialize HSV from RGB
        rgbToHsv(initialColor);
    }

    public void setValue(int rgb) {
        if (this.colorRGB != rgb) {
            this.colorRGB = rgb;
            rgbToHsv(rgb);
        }
    }

    public int getValue() {
        return colorRGB;
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private void rgbToHsv(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        // Brightness
        brightness = max;

        // Saturation
        saturation = max == 0 ? 0 : delta / max;

        // Hue
        if (delta == 0) {
            hue = 0;
        } else if (max == rf) {
            hue = ((gf - bf) / delta) % 6;
        } else if (max == gf) {
            hue = (bf - rf) / delta + 2;
        } else {
            hue = (rf - gf) / delta + 4;
        }
        hue /= 6f;
        if (hue < 0) hue += 1f;
    }

    private int hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }

        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    private void updateColorFromHSV() {
        colorRGB = hsvToRgb(hue, saturation, brightness);
        if (onChange != null) {
            onChange.accept(colorRGB);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Click on collapsed swatch to expand
        if (!expanded && isOverSwatch(mx, my)) {
            expanded = true;
            return true;
        }

        if (expanded) {
            // Click on hue bar
            if (isOverHueBar(mx, my)) {
                draggingHue = true;
                updateHueFromMouse(mx);
                return true;
            }

            // Click on SV box
            if (isOverSVBox(mx, my)) {
                draggingSV = true;
                updateSVFromMouse(mx, my);
                return true;
            }

            // Click outside to collapse
            if (!isMouseOver(mx, my)) {
                expanded = false;
                return true;
            }
        }

        return isMouseOver(mx, my);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (draggingHue || draggingSV)) {
            draggingHue = false;
            draggingSV = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (draggingHue) {
            updateHueFromMouse(mx);
            return true;
        }

        if (draggingSV) {
            updateSVFromMouse(mx, my);
            return true;
        }

        return false;
    }

    private void updateHueFromMouse(int mx) {
        int hueBarX = x + SWATCH_ZONE + SPACING;
        int hueBarWidth = width - SWATCH_ZONE - SPACING * 2;
        float progress = MathHelper.clamp((float)(mx - hueBarX) / hueBarWidth, 0f, 1f);
        hue = progress;
        updateColorFromHSV();
    }

    private void updateSVFromMouse(int mx, int my) {
        int svBoxX = x + SWATCH_ZONE + SPACING;
        int svBoxY = y + height + SPACING + HUE_BAR_HEIGHT + SPACING;

        saturation = MathHelper.clamp((float)(mx - svBoxX) / SV_BOX_SIZE, 0f, 1f);
        brightness = 1f - MathHelper.clamp((float)(my - svBoxY) / SV_BOX_SIZE, 0f, 1f);
        updateColorFromHSV();
    }

    private boolean isOverSwatch(int mx, int my) {
        int swatchX = x + (SWATCH_ZONE - SWATCH_SIZE) / 2;
        int swatchY = y + (height - SWATCH_SIZE) / 2;
        return mx >= swatchX && mx < swatchX + SWATCH_SIZE && my >= swatchY && my < swatchY + SWATCH_SIZE;
    }

    private boolean isOverHueBar(int mx, int my) {
        int hueBarX = x + SWATCH_ZONE + SPACING;
        int hueBarY = y + height + SPACING;
        int hueBarWidth = width - SWATCH_ZONE - SPACING * 2;
        return mx >= hueBarX && mx < hueBarX + hueBarWidth && my >= hueBarY && my < hueBarY + HUE_BAR_HEIGHT;
    }

    private boolean isOverSVBox(int mx, int my) {
        int svBoxX = x + SWATCH_ZONE + SPACING;
        int svBoxY = y + height + SPACING + HUE_BAR_HEIGHT + SPACING;
        return mx >= svBoxX && mx < svBoxX + SV_BOX_SIZE && my >= svBoxY && my < svBoxY + SV_BOX_SIZE;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY) {
        TextRenderer fr = MinecraftClient.getInstance().textRenderer;
        if (fr == null) return;

        boolean hover = isMouseOver(mouseX, mouseY);

        // Background
        int bg = hover ? (DesignTokens.bgInner() & 0x99FFFFFF) : 0x08ffffff;
        UIRenderHelper.fill(ms, x, y, x + width, y + height, bg);
        UIRenderHelper.drawBorder1px(ms, x, y, width, height, hover ? (DesignTokens.accentBase() & 0x99FFFFFF) : DesignTokens.borderSubtle());

        // Color swatch (left zone)
        int swatchX = x + (SWATCH_ZONE - SWATCH_SIZE) / 2;
        int swatchY = y + (height - SWATCH_SIZE) / 2;
        DrawableHelper.fill(ms, swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | colorRGB);
        UIRenderHelper.drawBorder1px(ms, swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, DesignTokens.borderSubtle());

        // Optional label (middle zone): only if width allows
        int middleStart = x + SWATCH_ZONE + SPACING;
        int middleEnd = x + width - HEX_ZONE - SPACING;
        if (middleEnd > middleStart + 20 && label != null && !label.isEmpty()) {
            String lbl = GuiUtil.truncate(fr, label, middleEnd - middleStart - 4);
            fr.draw(ms, lbl, (float) middleStart, (float) (y + (height - 8) / 2),
                hover ? DesignTokens.fgPrimary() : DesignTokens.fgSecondary());
        }

        // Hex value (right zone): full "#FFFFFF" or truncate only when necessary
        if (width >= SWATCH_ZONE + HEX_ZONE) {
            String hex = String.format("#%06X", colorRGB);
            int hexMaxW = HEX_ZONE - 8;
            if (fr.getWidth(hex) > hexMaxW) hex = GuiUtil.truncate(fr, hex, hexMaxW);
            int hexW = fr.getWidth(hex);
            int hexX = x + width - HEX_ZONE + (HEX_ZONE - hexW) / 2;
            fr.draw(ms, hex, (float) hexX, (float) (y + (height - 8) / 2), DesignTokens.accentBase());
        }
        // Expanded picker is rendered by parent after scissor is disabled (see TopZurdoMenuScreen)
    }

    private void renderExpandedPicker(MatrixStack ms, TextRenderer fr, int mouseX, int mouseY) {
        int expandedY = y + height;
        int expandedHeight = SPACING * 3 + HUE_BAR_HEIGHT + SV_BOX_SIZE;

        // Background for expanded area
        UIRenderHelper.fill(ms, x, expandedY, x + width, expandedY + expandedHeight, DesignTokens.bgPanel());
        UIRenderHelper.drawBorder1px(ms, x, expandedY, width, expandedHeight, DesignTokens.accentBase() & 0x80FFFFFF);

        // Hue bar
        int hueBarX = x + SPACING;
        int hueBarY = expandedY + SPACING;
        int hueBarWidth = width - SPACING * 2;

        // Draw rainbow gradient
        for (int i = 0; i < hueBarWidth; i++) {
            float h = (float) i / hueBarWidth;
            int c = hsvToRgb(h, 1f, 1f);
            DrawableHelper.fill(ms, hueBarX + i, hueBarY, hueBarX + i + 1, hueBarY + HUE_BAR_HEIGHT, 0xFF000000 | c);
        }

        // Hue indicator
        int hueIndicatorX = hueBarX + (int)(hue * hueBarWidth);
        DrawableHelper.fill(ms, hueIndicatorX - 1, hueBarY - 1, hueIndicatorX + 2, hueBarY + HUE_BAR_HEIGHT + 1, 0xFFFFFFFF);
        DrawableHelper.fill(ms, hueIndicatorX, hueBarY, hueIndicatorX + 1, hueBarY + HUE_BAR_HEIGHT, 0xFF000000);

        // SV box
        int svBoxX = x + SPACING;
        int svBoxY = hueBarY + HUE_BAR_HEIGHT + SPACING;

        // Draw SV gradient
        int baseHueColor = hsvToRgb(hue, 1f, 1f);
        for (int sx = 0; sx < SV_BOX_SIZE; sx++) {
            for (int sy = 0; sy < SV_BOX_SIZE; sy++) {
                float s = (float) sx / SV_BOX_SIZE;
                float v = 1f - (float) sy / SV_BOX_SIZE;
                int c = hsvToRgb(hue, s, v);
                DrawableHelper.fill(ms, svBoxX + sx, svBoxY + sy, svBoxX + sx + 1, svBoxY + sy + 1, 0xFF000000 | c);
            }
        }

        // SV indicator
        int svIndicatorX = svBoxX + (int)(saturation * SV_BOX_SIZE);
        int svIndicatorY = svBoxY + (int)((1f - brightness) * SV_BOX_SIZE);
        UIRenderHelper.drawCircle(ms, svIndicatorX, svIndicatorY, 4, 0xFFFFFFFF);
        UIRenderHelper.drawCircle(ms, svIndicatorX, svIndicatorY, 3, 0xFF000000 | colorRGB);

        // RGB values
        int r = (colorRGB >> 16) & 0xFF;
        int g = (colorRGB >> 8) & 0xFF;
        int b = colorRGB & 0xFF;

        int textX = svBoxX + SV_BOX_SIZE + SPACING * 2;
        int textY = svBoxY + 2;
        fr.draw(ms, "R: " + r, textX, textY, 0xFFFF6666);
        fr.draw(ms, "G: " + g, textX, textY + 12, 0xFF66FF66);
        fr.draw(ms, "B: " + b, textX, textY + 24, 0xFF6666FF);
    }

    @Override
    public int getX() { return x; }

    @Override
    public int getY() { return y; }

    @Override
    public int getWidth() { return width; }

    @Override
    public int getHeight() {
        if (expanded) {
            return height + SPACING * 3 + HUE_BAR_HEIGHT + SV_BOX_SIZE;
        }
        return height;
    }

    public boolean isExpanded() { return expanded; }

    public void collapse() { expanded = false; }

    /** Renders only the expanded palette; call after scissor is disabled so it is not clipped. */
    public void renderExpandedPart(MatrixStack ms, int mouseX, int mouseY) {
        if (!expanded) return;
        TextRenderer fr = MinecraftClient.getInstance().textRenderer;
        if (fr == null) return;
        ms.push();
        ms.translate(0, 0, 200);
        renderExpandedPicker(ms, fr, mouseX, mouseY);
        ms.pop();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + getHeight();
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        render(ms, mouseX, mouseY); // delegate to existing render
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return mouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return false;
    }
}
