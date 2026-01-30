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
public class CategoryTab {

    private int x, y, width, height;
    private Module.Category category;
    private boolean selected;
    private Consumer<Module.Category> onClick;
    private float hoverProgress = 0f;

    public CategoryTab(int x, int y, int width, int height, Module.Category category, Consumer<Module.Category> onClick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.category = category;
        this.onClick = onClick;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        float targetHover = hovered ? 1f : 0f;
        hoverProgress += (targetHover - hoverProgress) * 0.3f;

        int bgColor = selected ? OceanTheme.ACCENT : UIRenderHelper.lerpColor(OceanTheme.BG_PANEL, OceanTheme.BG_ELEVATED, hoverProgress);
        UIRenderHelper.drawRoundRect(ms, x, y, width, height, 4, bgColor);

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        String icon = getCategoryIcon();
        int textColor = selected ? OceanTheme.TEXT_PRIMARY : OceanTheme.TEXT_SECONDARY;

        tr.draw(ms, icon, x + 8, y + (height - 8) / 2, textColor);
        tr.draw(ms, category.getName(), x + 24, y + (height - 8) / 2, textColor);
    }

    private String getCategoryIcon() {
        switch (category) {
            case RENDER: return "🎨";
            case HUD: return "📊";
            case UTILITY: return "🔧";
            case PERFORMANCE: return "⚡";
            default: return "📦";
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
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
