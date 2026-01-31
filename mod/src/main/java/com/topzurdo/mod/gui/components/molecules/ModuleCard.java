package com.topzurdo.mod.gui.components.molecules;

import com.topzurdo.mod.gui.GuiUtil;
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
public class ModuleCard implements com.topzurdo.mod.gui.UIComponent {

    private int x, y, width, height;
    private Module module;
    private String title, description;
    private Toggle toggle;
    private Consumer<Module> onSelect;
    private boolean selected;
    private boolean enabled;
    private float hoverProgress = 0f;
    private float alphaMultiplier = 1f;
    private float partialTicks = 0f;

    public ModuleCard(int x, int y, int width, int height, Module module, Consumer<Module> onSelect) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.module = module;
        this.title = module.getName();
        this.description = module.getDescription();
        this.enabled = module.isEnabled();
        this.onSelect = onSelect;
        this.toggle = new Toggle(x + width - 46, y + (height - 18) / 2, module.isEnabled(), en -> {
            module.setEnabled(en);
        });
    }

    public ModuleCard(int x, int y, int width, int height, String title, String description, boolean enabled, boolean selected) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.module = null;
        this.title = title;
        this.description = description;
        this.enabled = enabled;
        this.selected = selected;
        this.toggle = new Toggle(x + width - 46, y + (height - 18) / 2, enabled, en -> {
            this.enabled = en;
            if (module != null) module.setEnabled(en);
        });
    }

    public void render(MatrixStack ms, int mouseX, int mouseY) {
        render(ms, mouseX, mouseY, 0f);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        float targetHover = hovered ? 1f : 0f;
        hoverProgress += (targetHover - hoverProgress) * (1f - (float) Math.pow(0.8f, Math.min(delta * 60f, 2f)));

        // Hover: лёгкий подъём (translateY -2)
        int renderOffsetY = (int) (-2 * hoverProgress);
        int drawY = y + renderOffsetY;

        int bgColor = selected ? OceanTheme.BG_ELEVATED : OceanTheme.BG_CARD;
        if (hovered && !selected) {
            bgColor = UIRenderHelper.lerpColor(OceanTheme.BG_CARD, OceanTheme.BG_ELEVATED, 0.3f);
        }
        int borderColor = selected ? OceanTheme.ACCENT : OceanTheme.BORDER;
        if (hovered) {
            borderColor = UIRenderHelper.lerpColor(borderColor, OceanTheme.BORDER_HOVER, hoverProgress * 0.5f);
        }

        if (alphaMultiplier < 1f) {
            bgColor = UIRenderHelper.withAlpha(bgColor, alphaMultiplier);
            borderColor = UIRenderHelper.withAlpha(borderColor, alphaMultiplier);
        }

        // Тень под карточкой при hover (динамический offset)
        if (hoverProgress > 0.01f) {
            int shadowOffset = 2 + (int) (hoverProgress * 4);
            int shadowAlpha = (int) (48 * hoverProgress * alphaMultiplier);
            UIRenderHelper.fillRoundRect(ms, x + shadowOffset, drawY + shadowOffset, width, height, OceanTheme.RADIUS_LARGE + 2, (shadowAlpha << 24));
        }

        UIRenderHelper.drawRoundRect(ms, x, drawY, width, height, OceanTheme.RADIUS_LARGE, bgColor);
        UIRenderHelper.drawRoundBorder(ms, x, drawY, width, height, OceanTheme.RADIUS_LARGE, 1, borderColor);

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        int textColor = UIRenderHelper.withAlpha(com.topzurdo.mod.gui.theme.DesignTokens.fgPrimary(), alphaMultiplier);
        int mutedColor = UIRenderHelper.withAlpha(com.topzurdo.mod.gui.theme.DesignTokens.fgMuted(), alphaMultiplier);

        // Место под toggle справа (46px), отступ 8px от края контента
        final int toggleZone = 46 + OceanTheme.SPACE_8;
        int pad = OceanTheme.SPACE_16;
        int textMaxW = Math.max(60, width - pad * 2 - toggleZone);
        String titleStr = title != null ? title : "";
        if (tr.getWidth(titleStr) > textMaxW) titleStr = GuiUtil.truncate(tr, titleStr, textMaxW);
        tr.draw(ms, titleStr, x + pad, drawY + pad, textColor);

        // Описание: полная ширина минус toggle и отступы, перенос по словам (до 2 строк)
        String desc = description != null ? description : "";
        int descMaxW = Math.max(80, width - pad * 2 - toggleZone);
        java.util.List<String> descLines = GuiUtil.wrapHint(tr, desc, descMaxW);
        int lineHeight = tr.fontHeight + 2;
        int maxDescLines = 2;
        for (int i = 0; i < Math.min(descLines.size(), maxDescLines); i++) {
            tr.draw(ms, descLines.get(i), x + pad, drawY + pad + 12 + i * lineHeight, mutedColor);
        }

        // Toggle по вертикали по центру карточки, справа с отступом
        int toggleY = y + (height - 18) / 2;
        toggle.setPosition(x + width - toggleZone + OceanTheme.SPACE_8, toggleY);
        toggle.setValue(enabled);
        // Рисуем toggle со смещением по Y для визуального подъёма
        ms.push();
        ms.translate(0, renderOffsetY, 0);
        toggle.render(ms, mouseX, mouseY, delta);
        ms.pop();
    }

    public void tick() {
        // Animation handled in render
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (toggle.mouseClicked(mouseX, mouseY, button)) {
            if (module != null) module.setEnabled(toggle.getValue());
            enabled = toggle.getValue();
            return true;
        }
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            if (onSelect != null && module != null) onSelect.accept(module);
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isSelected() { return selected; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; toggle.setValue(enabled); }
    public boolean isEnabled() { return enabled; }
    public void setAlphaMultiplier(float alpha) { this.alphaMultiplier = alpha; }
    public void setPartialTicks(float pt) { this.partialTicks = pt; }
    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
