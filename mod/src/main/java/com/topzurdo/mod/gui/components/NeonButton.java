package com.topzurdo.mod.gui.components;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import com.topzurdo.mod.gui.theme.DesignTokens;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Refined Neon Button: мягкие переходы, плавные анимации, современный стиль.
 */
public class NeonButton extends ButtonWidget {

    private final int neonColor;
    private float hoverProgress = 0f;
    private float pressProgress = 0f;
    private boolean wasHovered = false;
    private long lastClickTime = 0;

    public NeonButton(int x, int y, int width, int height, Text text, int neonColor, PressAction onPress) {
        super(x, y, width, height, text, onPress);
        this.neonColor = neonColor;
    }

    public NeonButton(int x, int y, int width, int height, Text text, PressAction onPress) {
        this(x, y, width, height, text, OceanTheme.NEON_CYAN, onPress);
    }

    @Override
    public void renderButton(MatrixStack m, int mouseX, int mouseY, float partial) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        boolean hover = isHovered();

        // Плавная анимация hover (ease-out)
        float targetHover = hover ? 1f : 0f;
        float hoverSpeed = hover ? 0.12f : 0.08f;
        hoverProgress += (targetHover - hoverProgress) * hoverSpeed;

        // Анимация нажатия
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 150) {
            pressProgress = 1f - (now - lastClickTime) / 150f;
        } else {
            pressProgress = 0f;
        }

        wasHovered = hover;

        int radius = 8;

        // Мягкая тень
        if (hoverProgress > 0.01f) {
            for (int i = 6; i > 0; i--) {
                float alpha = hoverProgress * (float)i / 6 * 0.05f;
                int shadowColor = UIRenderHelper.withAlpha(neonColor, alpha);
                UIRenderHelper.fillRoundRect(m, x - i + 2, y - i + 2, width + i * 2 - 4, height + i * 2 - 4, radius + i, shadowColor);
            }
        }

        // Фон кнопки
        int baseBg = 0xFF1C1917; // Stone 900
        int hoverBg = UIRenderHelper.blendColors(baseBg, neonColor, 0.08f);
        int bgColor = UIRenderHelper.lerpColor(baseBg, hoverBg, hoverProgress);
        bgColor = UIRenderHelper.withAlpha(bgColor, 0.85f + hoverProgress * 0.1f);

        // Эффект нажатия
        if (pressProgress > 0) {
            bgColor = UIRenderHelper.blendColors(bgColor, neonColor, pressProgress * 0.15f);
        }

        UIRenderHelper.fillRoundRect(m, x, y, width, height, radius, bgColor);

        // Верхний блик (стекло)
        int highlightAlpha = (int)(12 + hoverProgress * 8);
        int highlight = (highlightAlpha << 24) | 0x00FFFFFF;
        DrawableHelper.fill(m, x + 2, y + 1, x + width - 2, y + 2, highlight);

        // Рамка с градиентом
        float borderAlpha = 0.25f + hoverProgress * 0.35f;
        int borderColor = UIRenderHelper.withAlpha(neonColor, borderAlpha);
        UIRenderHelper.drawRoundBorder(m, x, y, width, height, radius, borderColor);

        // Левый акцент при hover
        if (hoverProgress > 0.1f) {
            int accentH = (int)(height * 0.6f * hoverProgress);
            int accentY = y + (height - accentH) / 2;
            int accentColor = UIRenderHelper.withAlpha(neonColor, 0.7f * hoverProgress);
            DrawableHelper.fill(m, x, accentY, x + 3, accentY + accentH, accentColor);
        }

        // Текст
        String textStr = getMessage().getString();
        int tw = mc.textRenderer.getWidth(textStr);
        float tx = x + (width - tw) / 2f;
        float ty = y + (height - 8) / 2f;

        // Сдвиг текста при hover
        float textOffset = hoverProgress * 2f;
        tx += textOffset;

        // Тень текста
        mc.textRenderer.draw(m, textStr, tx + 1, ty + 1, UIRenderHelper.withAlpha(0xFF000000, 0.3f));

        // Основной текст
        int textColor = UIRenderHelper.lerpColor(0xFFA8A29E, 0xFFFAFAF9, hoverProgress); // Stone 400 -> Stone 50
        if (hoverProgress > 0.5f) {
            textColor = UIRenderHelper.lerpColor(textColor, neonColor, (hoverProgress - 0.5f) * 0.4f);
        }
        mc.textRenderer.draw(m, textStr, tx, ty, textColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        lastClickTime = System.currentTimeMillis();
        super.onClick(mouseX, mouseY);
    }
}
