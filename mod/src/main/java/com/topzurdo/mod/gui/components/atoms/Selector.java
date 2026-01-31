package com.topzurdo.mod.gui.components.atoms;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.topzurdo.mod.gui.GuiUtil;
import com.topzurdo.mod.gui.UIComponent;
import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import com.topzurdo.mod.gui.theme.DesignTokens;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * Выпадающий список. Controlled: {@link #setValue(String)} перед render.
 * Родитель делегирует mouseClicked, mouseScrolled. При выборе — {@code onChange.accept(option)}.
 */
public final class Selector implements UIComponent {

    private static final int ROW_H = 20;
    private static final int ARROW_W = 12;
    /** Reserved right zone for value + arrow so label and value do not overlap. */
    private static final int VALUE_ZONE = 80;

    private int x, y;
    private final int width, height;
    private final String label;
    private final List<String> options;
    private final Consumer<String> onChange;

    private String value;
    private boolean expanded;

    public Selector(int x, int y, int width, int height, String label, List<String> options, String value, Consumer<String> onChange) {
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.label = label != null ? label : "";
        this.options = options != null ? options : Collections.emptyList();
        this.onChange = onChange;
        this.value = value != null ? value : (this.options.isEmpty() ? "" : this.options.get(0));
        this.expanded = false;
    }

    public void setValue(String v) {
        this.value = v != null ? v : "";
    }

    public String getValue() {
        if (options.isEmpty()) return "";
        int i = options.indexOf(value);
        return options.get(MathHelper.clamp(i >= 0 ? i : 0, 0, options.size() - 1));
    }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    /** Высота с учётом раскрытого списка. */
    public int getTotalHeight() {
        return expanded && !options.isEmpty() ? height + options.size() * ROW_H : height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (!isMouseOver(mx, my)) return false; // uses getHeight()=getTotalHeight()

        if (my >= y && my < y + height) {
            expanded = !expanded;
            return true;
        }

        if (expanded && !options.isEmpty()) {
            int listY = y + height;
            for (int i = 0; i < options.size(); i++) {
                int rowY = listY + i * ROW_H;
                if (my >= rowY && my < rowY + ROW_H) {
                    this.value = options.get(i);
                    if (onChange != null) onChange.accept(options.get(i));
                    expanded = false;
                    return true;
                }
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Only consume scroll when EXPANDED - otherwise let parent scroll
        if (!expanded) return false;
        if (!isMouseOver((int) mouseX, (int) mouseY) || options.isEmpty()) return false;
        int idx = MathHelper.clamp(options.indexOf(value), 0, options.size() - 1);
        if (delta > 0) idx = Math.max(0, idx - 1);
        else if (delta < 0) idx = Math.min(options.size() - 1, idx + 1);
        this.value = options.get(idx);
        if (onChange != null) onChange.accept(options.get(idx));
        return true;
    }

    private static String resolve(String s) {
        if (s == null) return "";
        return GuiUtil.resolveKey(s);
    }

    public void render(MatrixStack ms, int mouseX, int mouseY) {
        TextRenderer fr = MinecraftClient.getInstance().textRenderer;
        if (fr == null) return;

        if (options.isEmpty()) {
            UIRenderHelper.fill(ms, x, y, x + width, y + height, 0x08ffffff);
            UIRenderHelper.drawBorder1px(ms, x, y, width, height, DesignTokens.borderSubtle());
            int labelMaxW = Math.max(20, width - VALUE_ZONE - 10);
            fr.draw(ms, GuiUtil.truncate(fr, label, labelMaxW), (float) (x + 10), (float) (y + (height - 8) / 2), DesignTokens.fgDisabled());
            return;
        }

        boolean hover = isMouseOver(mouseX, mouseY);
        int bg = hover || expanded ? (DesignTokens.bgInner() & 0x99FFFFFF) : 0x08ffffff;
        UIRenderHelper.fill(ms, x, y, x + width, y + height, bg);
        int brd = hover || expanded ? (DesignTokens.accentBase() & 0x99FFFFFF) : DesignTokens.borderSubtle();
        UIRenderHelper.drawBorder1px(ms, x, y, width, height, brd);
        if (expanded)
            UIRenderHelper.fill(ms, x, y, x + 3, y + height, DesignTokens.accentBase() & 0x99FFFFFF);

        int labelMaxW = Math.max(20, width - VALUE_ZONE - 10);
        String lbl = GuiUtil.truncate(fr, label, labelMaxW);
        int txt = hover || expanded ? DesignTokens.fgPrimary() : DesignTokens.fgSecondary();
        fr.draw(ms, lbl, (float) (x + 10), (float) (y + (height - 8) / 2), txt);

        String cur = resolve(getValue());
        int vw = fr.getWidth(cur);
        int valueZoneStart = x + width - VALUE_ZONE;
        int valueX = valueZoneStart + Math.max(4, (VALUE_ZONE - ARROW_W - vw - 4) / 2);
        fr.draw(ms, cur, (float) valueX, (float) (y + (height - 8) / 2), DesignTokens.accentBase());
        fr.draw(ms, expanded ? "▲" : "▼", (float) (x + width - ARROW_W - 4), (float) (y + (height - 8) / 2), DesignTokens.accentActive());

        // Expanded list is rendered by parent in second pass (renderExpandedPart) so it draws on top
    }

    /**
     * Compute dropdown list Y position. Flips above the button if it would overflow visible bottom.
     */
    private int getDropdownListY(int visibleBottomY, int scrollOffset) {
        int listH = Math.min(options.size() * ROW_H, 5 * ROW_H);
        int dropdownBottomScreen = (y + height) + listH - scrollOffset;
        return (dropdownBottomScreen > visibleBottomY) ? (y - listH) : (y + height);
    }

    /**
     * Render expanded dropdown list with smooth animation.
     * Rendered above other content with higher z-index.
     */
    private void renderExpandedList(MatrixStack ms, TextRenderer fr, int mouseX, int mouseY, int visibleBottomY, int scrollOffset) {
        int listH = Math.min(options.size() * ROW_H, 5 * ROW_H); // Max 5 visible rows
        int listY = getDropdownListY(visibleBottomY, scrollOffset);
        int totalH = options.size() * ROW_H;
        boolean needsScroll = totalH > listH;
        int radius = DesignTokens.RADIUS;

        // Push matrix for z-ordering (render above other elements)
        ms.push();
        ms.translate(0, 0, 100); // Higher z-index

        // Shadow below dropdown
        UIRenderHelper.drawShadow(ms, x, listY, width, listH, 4);
        // Background: rounded panel
        UIRenderHelper.fillRoundRect(ms, x, listY, width, listH, radius, OceanTheme.BG_INNER);
        UIRenderHelper.drawRoundBorder(ms, x, listY, width, listH, radius, 1, DesignTokens.accentBase() & 0xAAFFFFFF);

        // Render visible options
        int maxVisible = Math.min(options.size(), 5);
        for (int i = 0; i < maxVisible; i++) {
            int optIndex = i; // For scrolling, would add scroll offset here
            if (optIndex >= options.size()) break;

            int rowY = listY + i * ROW_H;
            boolean rh = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_H;

            // Hover effect with smooth gradient
            if (rh) {
                UIRenderHelper.fill(ms, x + 1, rowY + 1, x + width - 1, rowY + ROW_H - 1,
                    DesignTokens.accentBase() & 0x30FFFFFF);
            }

            boolean sel = options.get(optIndex).equals(getValue());

            // Selection indicator - softer accent bar
            if (sel) {
                UIRenderHelper.fill(ms, x, rowY, x + 3, rowY + ROW_H, DesignTokens.success());
            }

            // Text color: always high contrast so options are readable
            int tc = sel ? DesignTokens.success() : (rh ? DesignTokens.fgPrimary() : DesignTokens.fgPrimary());
            fr.draw(ms, resolve(options.get(optIndex)), (float) (x + 10), (float) (rowY + (ROW_H - 8) / 2), tc);
        }

        // Scroll indicator if needed
        if (needsScroll && options.size() > 5) {
            int indicatorY = listY + listH - 8;
            fr.draw(ms, "...", (float)(x + width / 2 - 6), (float)indicatorY, DesignTokens.fgMuted());
        }

        ms.pop();
    }

    /** Renders only the expanded dropdown; call after scissor is disabled so it draws on top of everything. */
    public void renderExpandedPart(MatrixStack ms, int mouseX, int mouseY, int visibleBottomY, int scrollOffset) {
        if (!expanded || options.isEmpty()) return;
        TextRenderer fr = MinecraftClient.getInstance().textRenderer;
        if (fr == null) return;
        ms.push();
        ms.translate(0, 0, 400); // Above ColorPicker expanded (200)
        renderExpandedList(ms, fr, mouseX, mouseY, visibleBottomY, scrollOffset);
        ms.pop();
    }

    public boolean isExpanded() { return expanded; }

    /** Toggle dropdown for keyboard activation. */
    public void toggleExpanded() {
        if (!options.isEmpty()) expanded = !expanded;
    }

    @Override
    public int getX() { return x; }
    @Override
    public int getY() { return y; }
    @Override
    public int getWidth() { return width; }
    @Override
    public int getHeight() { return getTotalHeight(); }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + getTotalHeight();
    }

    /** True when mouse is over the dropdown list only (not the button). Used so overlay click priority doesn't steal clicks from rows below. */
    public boolean isMouseOverDropdown(int mx, int my, int visibleBottomY, int scrollOffset) {
        if (!expanded || options.isEmpty()) return false;
        int listH = Math.min(options.size() * ROW_H, 5 * ROW_H);
        int listY = getDropdownListY(visibleBottomY, scrollOffset);
        return mx >= x && mx < x + width && my >= listY && my < listY + listH;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        render(ms, mouseX, mouseY); // delegate to existing render
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
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
