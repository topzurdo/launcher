package com.topzurdo.mod.gui.components.molecules;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import com.topzurdo.mod.gui.components.atoms.Toggle;
import com.topzurdo.mod.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Consumer;

/**
 * Module card component for module list
 */
public class ModuleCard {

    private int x, y, width, height;
    private Module module;
    private Toggle toggle;
    private Consumer<Module> onSelect;
    private boolean selected;
    private float hoverProgress = 0f;

    public ModuleCard(int x, int y, int width, int height, Module module, Consumer<Module> onSelect) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.module = module;
        this.onSelect = onSelect;
        this.toggle = new Toggle(x + width - 46, y + (height - 18) / 2, module.isEnabled(), enabled -> {
            module.setEnabled(enabled);
        });
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        float targetHover = hovered ? 1f : 0f;
        hoverProgress += (targetHover - hoverProgress) * 0.3f;

        int bgColor = selected ? OceanTheme.BG_ELEVATED : UIRenderHelper.lerpColor(OceanTheme.BG_PANEL, OceanTheme.BG_ELEVATED, hoverProgress * 0.5f);
        int borderColor = selected ? OceanTheme.ACCENT : OceanTheme.BORDER;

        UIRenderHelper.drawRoundRect(ms, x, y, width, height, 6, bgColor);
        if (selected) {
            UIRenderHelper.drawRoundBorder(ms, x, y, width, height, 6, 2, borderColor);
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        tr.draw(ms, module.getName(), x + 10, y + 8, OceanTheme.TEXT_PRIMARY);
        
        // Description with wrapping
        String desc = module.getDescription();
        if (desc.length() > 40) desc = desc.substring(0, 37) + "...";
        tr.draw(ms, desc, x + 10, y + 20, OceanTheme.TEXT_MUTED);

        // Update toggle position and render
        toggle.setPosition(x + width - 46, y + (height - 18) / 2);
        toggle.setValue(module.isEnabled());
        toggle.render(ms, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (toggle.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            if (onSelect != null) onSelect.accept(module);
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isSelected() { return selected; }
    public Module getModule() { return module; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
