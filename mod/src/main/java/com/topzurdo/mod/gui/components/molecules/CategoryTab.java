package com.topzurdo.mod.gui.components.molecules;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import com.topzurdo.mod.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Consumer;

/**
 * Category tab for module menu
 */
public class CategoryTab implements com.topzurdo.mod.gui.UIComponent {

    private int x, y, width, height;
    private Module.Category category;
    private String label, icon;
    private int onCount, totalCount;
    private boolean selected;
    private Consumer<Module.Category> onClick;
    private float hoverProgress = 0f;

    public CategoryTab(int x, int y, int width, int height, Module.Category category, Consumer<Module.Category> onClick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.category = category;
        this.label = category.getName();
        this.icon = getCategoryIcon();
        this.onClick = onClick;
    }

    public CategoryTab(int x, int y, int width, int height, String label, String icon, int onCount, int totalCount, boolean selected) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.icon = icon;
        this.onCount = onCount;
        this.totalCount = totalCount;
        this.selected = selected;
        this.category = null;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY) {
        render(ms, mouseX, mouseY, 0f);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        float targetHover = hovered ? 1f : 0f;
        hoverProgress += (targetHover - hoverProgress) * 0.2f;

        // Карточка меню: фон, border-radius 8px
        int bgColor = selected ? OceanTheme.BG_ELEVATED : UIRenderHelper.lerpColor(OceanTheme.BG_CARD, OceanTheme.BG_ELEVATED, hoverProgress * 0.5f);
        UIRenderHelper.drawRoundRect(ms, x, y, width, height, OceanTheme.RADIUS_MED, bgColor);

        // Активный элемент: левый бордер 3px + лёгкое свечение
        if (selected) {
            UIRenderHelper.drawRect(ms, x, y + OceanTheme.RADIUS_MED, x + OceanTheme.BORDER_ACTIVE_LEFT, y + height - OceanTheme.RADIUS_MED, OceanTheme.ACCENT);
            UIRenderHelper.drawRect(ms, x + OceanTheme.BORDER_ACTIVE_LEFT, y, x + OceanTheme.BORDER_ACTIVE_LEFT + 1, y + OceanTheme.RADIUS_MED, OceanTheme.ACCENT);
            UIRenderHelper.drawRect(ms, x + OceanTheme.BORDER_ACTIVE_LEFT, y + height - OceanTheme.RADIUS_MED, x + OceanTheme.BORDER_ACTIVE_LEFT + 1, y + height, OceanTheme.ACCENT);
        } else if (hoverProgress > 0.01f) {
            int glowColor = UIRenderHelper.withAlpha(OceanTheme.ACCENT, 0.15f * hoverProgress);
            UIRenderHelper.drawRoundBorder(ms, x, y, width, height, OceanTheme.RADIUS_MED, 1, glowColor);
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        String iconStr = category != null ? getCategoryIcon() : icon;
        String nameStr = category != null ? category.getName() : label;
        int textColor = selected ? com.topzurdo.mod.gui.theme.DesignTokens.fgPrimary() : com.topzurdo.mod.gui.theme.DesignTokens.fgSecondary();
        int iconColor = selected ? OceanTheme.ACCENT_SECONDARY : textColor;

        // Padding 12px 16px, иконка слева
        tr.draw(ms, iconStr, x + OceanTheme.SPACE_12, y + (height - 8) / 2, iconColor);
        tr.draw(ms, nameStr, x + OceanTheme.SPACE_12 + 16, y + (height - 8) / 2, textColor);
    }

    private String getCategoryIcon() {
        if (category == null) return icon != null ? icon : "";
        switch (category) {
            case RENDER: return "\u25C6";   // ◆
            case HUD: return "\u25CF";     // ●
            case UTILITY: return "\u25C7";  // ◇
            case PERFORMANCE: return "\u26A1"; // ⚡
            default: return "\u25A0";      // ■
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY) && category != null) {
            if (onClick != null) onClick.accept(category);
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isSelected() { return selected; }
    public Module.Category getCategory() { return category; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
