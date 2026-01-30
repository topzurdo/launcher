package com.topzurdo.mod.gui;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.gui.components.NeonButton;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

/**
 * Главное меню в стиле Premium Gold: премиальные тона, золото, темный фон.
 */
public class CustomMainMenuScreen extends Screen {

    // Более компактная панель (увеличена высота для новой кнопки)
    private static final int PANEL_W = 400;
    private static final int PANEL_H = 440;
    private static final int BTN_W = 320;
    private static final int BTN_H = 48;
    private static final int BTN_GAP = 16;
    private static final int CARD_PAD = 40;

    // Анимация
    private float openAnimation = 0f;
    private float titleGlow = 0f;
    private boolean titleGlowUp = true;

    public CustomMainMenuScreen() {
        super(new LiteralText("TopZurdo"));
        TopZurdoMod.getLogger().info("[TopZurdo] Refined Neon menu initialized");
    }

    @Override
    protected void init() {
        super.init();
        openAnimation = 0f;

        int cx = width / 2;
        int py = height / 2 - PANEL_H / 2;
        int startY = py + 120;

        // Кнопки с иконками и золотым стилем
        addButton(new NeonButton(cx - BTN_W / 2, startY, BTN_W, BTN_H,
            new LiteralText("▶  Одиночная игра"), OceanTheme.NEON_CYAN, // Gold (mapped to CYAN constant)
            b -> { if (client != null) client.openScreen(new SelectWorldScreen(this)); }));

        addButton(new NeonButton(cx - BTN_W / 2, startY + BTN_H + BTN_GAP, BTN_W, BTN_H,
            new LiteralText("◈  Сетевая игра"), OceanTheme.NEON_PURPLE, // Amber (mapped to PURPLE constant)
            b -> { if (client != null) client.openScreen(new MultiplayerScreen(this)); }));

        addButton(new NeonButton(cx - BTN_W / 2, startY + (BTN_H + BTN_GAP) * 2, BTN_W, BTN_H,
            new LiteralText("⚙  Настройки"), OceanTheme.NEON_GOLD, // Bright Gold
            b -> { if (client != null) client.openScreen(new OptionsScreen(this, client.options)); }));

        addButton(new NeonButton(cx - BTN_W / 2, startY + (BTN_H + BTN_GAP) * 3, BTN_W, BTN_H,
            new LiteralText("👤  Смена ника"), OceanTheme.NEON_PURPLE, // Account switcher
            b -> { if (client != null) client.openScreen(new AccountSwitcherScreen(this)); }));

        addButton(new NeonButton(cx - BTN_W / 2, startY + (BTN_H + BTN_GAP) * 4, BTN_W, BTN_H,
            new LiteralText("✕  Выйти"), 0xFF57534E, // Stone 600 для выхода
            b -> { if (client != null) client.stop(); }));
    }

    @Override
    public void tick() {
        super.tick();
        UIRenderHelper.tickAnimation();

        // Плавная анимация открытия
        if (openAnimation < 1f) {
            openAnimation = Math.min(1f, openAnimation + 0.06f);
        }

        // Пульсация заголовка
        if (titleGlowUp) {
            titleGlow += 0.015f;
            if (titleGlow >= 1f) titleGlowUp = false;
        } else {
            titleGlow -= 0.015f;
            if (titleGlow <= 0f) titleGlowUp = true;
        }
    }

    @Override
    public void render(MatrixStack m, int mouseX, int mouseY, float partial) {
        UIRenderHelper.setPartialTicks(partial);

        int cx = width / 2;
        int px = cx - PANEL_W / 2;
        int py = height / 2 - PANEL_H / 2;

        // Ease-out анимация
        float eased = easeOutCubic(openAnimation);

        // Фон
        drawModernBackground(m);

        // Частицы (меньше и мягче)
        UIRenderHelper.drawFloatingParticles(m, 0, 0, width, height,
            UIRenderHelper.withAlpha(OceanTheme.NEON_CYAN, 0.6f), 6);

        // Виньетка (мягче)
        UIRenderHelper.drawVignette(m, 0, 0, width, height, 0.12f);

        // Панель с анимацией
        m.push();
        float scale = 0.9f + 0.1f * eased;
        m.translate(cx, py + PANEL_H / 2, 0);
        m.scale(scale, scale, 1f);
        m.translate(-cx, -(py + PANEL_H / 2), 0);

        drawGlassPanel(m, px, py, eased);

        m.pop();

        // Заголовок
        drawAnimatedTitle(m, cx, py + CARD_PAD);

        // Подзаголовок
        String sub = "Твой путь к победе";
        int subAlpha = (int)(180 * eased);
        int subColor = (subAlpha << 24) | (OceanTheme.TEXT_DIM & 0x00FFFFFF);
        textRenderer.draw(m, sub, cx - textRenderer.getWidth(sub) / 2f, py + CARD_PAD + 50, subColor);

        // Декоративная линия
        int lineY = py + CARD_PAD + 68;
        int lineW = (int)(100 * eased);
        drawGradientLine(m, cx - lineW, lineY, lineW * 2);

        // Версия внизу
        String version = "TopZurdo v1.0.0 • Minecraft 1.16.5";
        textRenderer.draw(m, version, cx - textRenderer.getWidth(version) / 2f,
            height - 20, UIRenderHelper.withAlpha(OceanTheme.TEXT_MUTED, 0.5f));

        super.render(m, mouseX, mouseY, partial);
    }

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1 - x, 3);
    }

    private void drawModernBackground(MatrixStack m) {
        // Градиент фона (теплее)
        UIRenderHelper.fillVerticalGradient(m, 0, 0, width, height, 0xFF0E1117, 0xFF161B22);

        // Тонкая сетка
        int gridColor = UIRenderHelper.withAlpha(OceanTheme.NEON_CYAN, 0.015f);
        int gridSize = 48;
        for (int gx = 0; gx < width + gridSize; gx += gridSize) {
            DrawableHelper.fill(m, gx, 0, gx + 1, height, gridColor);
        }
        for (int gy = 0; gy < height + gridSize; gy += gridSize) {
            DrawableHelper.fill(m, 0, gy, width, gy + 1, gridColor);
        }

        // Градиентное свечение сверху
        int glowHeight = height / 3;
        for (int i = 0; i < glowHeight; i++) {
            float alpha = (1f - (float)i / glowHeight) * 0.03f;
            int c = UIRenderHelper.withAlpha(OceanTheme.NEON_PURPLE, alpha);
            DrawableHelper.fill(m, 0, i, width, i + 1, c);
        }
    }

    private void drawGlassPanel(MatrixStack m, int x, int y, float anim) {
        int alpha = (int)(220 * anim);

        // Мягкая тень
        for (int i = 8; i > 0; i--) {
            float shadowAlpha = (float)i / 8 * 0.04f * anim;
            int shadowColor = UIRenderHelper.withAlpha(0xFF000000, shadowAlpha);
            DrawableHelper.fill(m, x - i + 4, y - i + 4, x + PANEL_W + i + 4, y + PANEL_H + i + 4, shadowColor);
        }

        // Основной фон (стекло)
        int bgColor = (alpha << 24) | (OceanTheme.BG_PANEL & 0x00FFFFFF);
        UIRenderHelper.fillRoundRect(m, x, y, PANEL_W, PANEL_H, 12, bgColor);

        // Верхний градиент (блик)
        int highlightH = 60;
        for (int i = 0; i < highlightH; i++) {
            float t = 1f - (float)i / highlightH;
            int highlightAlpha = (int)(t * t * 8 * anim);
            int c = (highlightAlpha << 24) | 0x00FFFFFF;
            DrawableHelper.fill(m, x + 1, y + 1 + i, x + PANEL_W - 1, y + 2 + i, c);
        }

        // Рамка с градиентом
        int borderAlpha = (int)(100 * anim);
        int borderTop = (borderAlpha << 24) | (OceanTheme.NEON_CYAN & 0x00FFFFFF);
        int borderBottom = (borderAlpha / 2 << 24) | (OceanTheme.NEON_PURPLE & 0x00FFFFFF);

        // Верхняя граница (ярче)
        DrawableHelper.fill(m, x, y, x + PANEL_W, y + 1, borderTop);
        // Боковые границы (градиент)
        for (int i = 0; i < PANEL_H; i++) {
            float t = (float)i / PANEL_H;
            int c = UIRenderHelper.lerpColor(borderTop, borderBottom, t);
            DrawableHelper.fill(m, x, y + i, x + 1, y + i + 1, c);
            DrawableHelper.fill(m, x + PANEL_W - 1, y + i, x + PANEL_W, y + i + 1, c);
        }
        // Нижняя граница
        DrawableHelper.fill(m, x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, borderBottom);

        // Угловые акценты
        int cornerLen = 20;
        int cornerColor = UIRenderHelper.withAlpha(OceanTheme.NEON_CYAN, 0.8f * anim);
        // Верхний левый
        DrawableHelper.fill(m, x, y, x + cornerLen, y + 2, cornerColor);
        DrawableHelper.fill(m, x, y, x + 2, y + cornerLen, cornerColor);
        // Верхний правый
        DrawableHelper.fill(m, x + PANEL_W - cornerLen, y, x + PANEL_W, y + 2, cornerColor);
        DrawableHelper.fill(m, x + PANEL_W - 2, y, x + PANEL_W, y + cornerLen, cornerColor);
        // Нижний левый
        DrawableHelper.fill(m, x, y + PANEL_H - 2, x + cornerLen, y + PANEL_H, cornerColor);
        DrawableHelper.fill(m, x, y + PANEL_H - cornerLen, x + 2, y + PANEL_H, cornerColor);
        // Нижний правый
        DrawableHelper.fill(m, x + PANEL_W - cornerLen, y + PANEL_H - 2, x + PANEL_W, y + PANEL_H, cornerColor);
        DrawableHelper.fill(m, x + PANEL_W - 2, y + PANEL_H - cornerLen, x + PANEL_W, y + PANEL_H, cornerColor);
    }

    private void drawAnimatedTitle(MatrixStack m, int cx, int ty) {
        String title = "TOPZURDO";
        int tw = textRenderer.getWidth(title);
        float scale = 2.5f;

        // Пульсирующее свечение
        float glowIntensity = 0.15f + titleGlow * 0.1f;

        m.push();
        m.translate(cx, ty, 0);
        m.scale(scale, scale, 1f);

        float titleX = -tw / 2f;

        // Свечение за текстом
        int glowColor = UIRenderHelper.withAlpha(OceanTheme.NEON_CYAN, glowIntensity);
        for (int ox = -2; ox <= 2; ox++) {
            for (int oy = -2; oy <= 2; oy++) {
                if (ox != 0 || oy != 0) {
                    textRenderer.draw(m, title, titleX + ox * 0.5f, oy * 0.5f, glowColor);
                }
            }
        }

        // Тень
        textRenderer.draw(m, title, titleX + 1, 1, UIRenderHelper.withAlpha(0xFF000000, 0.4f));

        // Основной текст с градиентом (имитация)
        textRenderer.draw(m, title, titleX, 0, 0xFFFFFFFF);

        m.pop();

        // Декоративные линии по бокам
        int halfW = (int)(tw * scale / 2f);
        int lineY = ty + 12;
        int lineLen = 30;
        int lineColor = UIRenderHelper.withAlpha(OceanTheme.NEON_CYAN, 0.4f + titleGlow * 0.2f);

        // Левая линия с градиентом
        for (int i = 0; i < lineLen; i++) {
            float alpha = (float)i / lineLen * (0.4f + titleGlow * 0.2f);
            int c = UIRenderHelper.withAlpha(OceanTheme.NEON_CYAN, alpha);
            DrawableHelper.fill(m, cx - halfW - lineLen + i - 8, lineY, cx - halfW - lineLen + i + 1 - 8, lineY + 2, c);
        }
        // Правая линия с градиентом
        for (int i = 0; i < lineLen; i++) {
            float alpha = (1f - (float)i / lineLen) * (0.4f + titleGlow * 0.2f);
            int c = UIRenderHelper.withAlpha(OceanTheme.NEON_CYAN, alpha);
            DrawableHelper.fill(m, cx + halfW + i + 8, lineY, cx + halfW + i + 1 + 8, lineY + 2, c);
        }
    }

    private void drawGradientLine(MatrixStack m, int x, int y, int w) {
        int half = w / 2;
        int cx = x + half;

        for (int i = 0; i < half; i++) {
            float alpha = (float)i / half * 0.5f;
            int c = UIRenderHelper.withAlpha(OceanTheme.NEON_CYAN, alpha);
            DrawableHelper.fill(m, cx - half + i, y, cx - half + i + 1, y + 1, c);
            DrawableHelper.fill(m, cx + half - i - 1, y, cx + half - i, y + 1, c);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
