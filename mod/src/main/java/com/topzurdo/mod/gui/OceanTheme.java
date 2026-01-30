package com.topzurdo.mod.gui;

/**
 * Палитра TopZurdo UI — Pro Max Gold: премиальное золото, глубокие темные тона.
 * Ultimate visual experience.
 */
public final class OceanTheme {

    private OceanTheme() {}

    /** Рисовать рамки для отладки. */
    public static final boolean DEBUG_BOUNDS = false;

    // ——— ULTIMATE GOLD PALETTE ———

    /** Глубокий темный фон (Stone 950) #0C0A09 */
    public static final int BG_DEEP = 0xFF0C0A09;

    /** Полупрозрачный фон оверлея (затемнение мира) */
    public static final int BG_OVERLAY = 0xCC000000;

    /** Фон стеклянной панели (Stone 900 с прозрачностью) */
    public static final int BG_PANEL = 0xE61C1917; // 90% opacity

    /** Фон заголовка/категорий (Stone 800) */
    public static final int BG_HEADER = 0xFF292524;

    /** Внутренний фон элементов (Stone 950) */
    public static final int BG_INNER = 0xFF0C0A09;

    // ——— NEON ACCENTS (Premium Gold) ———

    /** Golden Sun #F59E0B (Amber 500) - Основной акцент */
    public static final int NEON_CYAN = 0xFFF59E0B; // Was Cyan, now Gold
    public static final int NEON_CYAN_GLOW = 0x60F59E0B;

    /** Deep Amber #D97706 (Amber 600) - Вторичный акцент */
    public static final int NEON_PURPLE = 0xFFD97706; // Was Purple, now Deep Amber
    public static final int NEON_PURPLE_GLOW = 0x60D97706;

    /** Royal Red #EF4444 (Red 500) - Акцент для важных действий */
    public static final int NEON_PINK = 0xFFEF4444; // Was Pink, now Red
    public static final int NEON_PINK_GLOW = 0x60EF4444;

    /** Bright Gold #FCD34D (Amber 300) - Хайлайты */
    public static final int NEON_GOLD = 0xFFFCD34D;
    public static final int NEON_GOLD_GLOW = 0x60FCD34D;

    // ——— TEXT COLORS ———

    /** Теплый белый текст (Stone 50) */
    public static final int TEXT = 0xFFFAFAF9;
    /** Второстепенный текст (Stone 400) */
    public static final int TEXT_DIM = 0xFFA8A29E;
    /** Неактивный текст (Stone 600) */
    public static final int TEXT_MUTED = 0xFF57534E;
    /** Акцентный текст (Gold) */
    public static final int FOAM = 0xFFF59E0B;

    // ——— COMPATIBILITY ALIASES ———
    public static final int ACCENT = NEON_CYAN;
    public static final int ACCENT_DARK = 0xFFB45309; // Amber 700
    public static final int ACCENT_SECONDARY = NEON_PURPLE;
    public static final int ACCENT_SECONDARY_DARK = 0xFF92400E; // Amber 800

    public static final int SUCCESS = 0xFF10B981; // Emerald 500
    public static final int DISABLED = 0xFF44403C; // Stone 700

    public static final int BORDER = 0x40F59E0B; // Gold border
    public static final int BORDER_SUBTLE = 0x20A8A29E; // Subtle divider
    public static final int BORDER_HOVER = 0x80F59E0B; // Active border

    public static final int BORDER_WIDTH = 1;

    // ——— UX METRICS ———

    /** Радиус скругления кнопок (большой) */
    public static final int RADIUS_SMALL = 4;
    public static final int RADIUS_MED = 8;
    public static final int RADIUS_LARGE = 12;

    // ——— SPACING ———
    public static final int SPACE_4 = 4;
    public static final int SPACE_8 = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_24 = 24;
    public static final int SPACE_32 = 32;

    // ——— LAYOUT CONSTANTS ———
    public static final int BOTTOM_MARGIN = 70;
    public static final int HOVER_DESC_ZONE = 26;
    public static final int FOOTER_H = 38;
    public static final int CONTENT_BOTTOM_OFFSET = HOVER_DESC_ZONE + FOOTER_H;
    public static final int SETTINGS_HEADER_H = 48;
    public static final int ROW_TOGGLE_H = 40;
    public static final int ROW_SLIDER_H = 54;
    public static final int ROW_SELECTOR_H = 40;
    public static final int ROW_TOGGLE_STEP = 44;
    public static final int ROW_SLIDER_STEP = 58;
    public static final int ROW_SELECTOR_STEP = 44;
    public static final int SETTINGS_PAD_X = 16; // Wider padding
    public static final int HOVER_DESC_H = 12;
    public static final int SCROLLBAR_WIDTH = 4;
    public static final int SCROLLBAR_THUMB_RADIUS = 2;
}
