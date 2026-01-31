package com.topzurdo.mod.gui;

/**
 * Тема «Твой путь к победе» — геймерский/киберспортивный стиль.
 * Высокий контраст, неоновые акценты, современная палитра.
 */
public final class OceanTheme {

    private OceanTheme() {}

    public static final boolean DEBUG_BOUNDS = false;

    // ——— ФОН ———
    /** Градиент фона: верх #0a0e1a */
    public static final int BG_GRADIENT_TOP = 0xFF0a0e1a;
    /** Градиент фона: низ #1a1f2e */
    public static final int BG_GRADIENT_BOTTOM = 0xFF1a1f2e;
    /** Глубокий тёмный фон (alias) */
    public static final int BG_DEEP = BG_GRADIENT_TOP;
    /** Оверлей затемнения */
    public static final int BG_OVERLAY = 0xCC000000;
    /** Панель основная */
    public static final int BG_PANEL = 0xFF1a1f2e;
    /** Заголовок / категории */
    public static final int BG_HEADER = 0xFF1a1f2e;
    /** Внутренний фон полей */
    public static final int BG_INNER = 0xFF0a0e1a;
    /** Карточки модулей #1e293b */
    public static final int BG_CARD = 0xFF1e293b;
    /** Трек слайдера, неактивный toggle #334155 */
    public static final int BG_TRACK = 0xFF334155;
    /** Приподнятый фон (alias) */
    public static final int BG_ELEVATED = 0xFF252b3b;

    // ——— АКЦЕНТЫ ———
    /** Primary: энергичный оранжевый #FF6B35 */
    public static final int ACCENT = 0xFFFF6B35;
    public static final int ACCENT_GRADIENT_END = 0xFFFF8F35;
    /** Secondary: неоновый cyan #00D9FF */
    public static final int ACCENT_SECONDARY = 0xFF00D9FF;
    public static final int NEON_CYAN = ACCENT_SECONDARY;
    public static final int NEON_CYAN_GLOW = 0x6000D9FF;
    public static final int NEON_PURPLE = ACCENT;
    public static final int NEON_PURPLE_GLOW = 0x60FF6B35;
    public static final int NEON_PINK = 0xFFEF4444;
    public static final int NEON_PINK_GLOW = 0x60EF4444;
    public static final int NEON_GOLD = ACCENT_GRADIENT_END;
    public static final int NEON_GOLD_GLOW = 0x60FF8F35;
    public static final int ACCENT_DARK = 0xFFE55A2B;
    public static final int ACCENT_SECONDARY_DARK = 0xFF00B8D9;

    // ——— ТЕКСТ (контраст ≥4.5:1) ———
    /** Основной текст #FFFFFF */
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_PRIMARY = TEXT;
    /** Второстепенный #B4C3D8 — повышенный контраст на тёмном фоне */
    public static final int TEXT_DIM = 0xFFB4C3D8;
    public static final int TEXT_SECONDARY = TEXT_DIM;
    /** Описания, неактивный #A0B0C0 — больше контраста */
    public static final int TEXT_MUTED = 0xFFA0B0C0;
    /** Акцентный текст (cyan/orange) */
    public static final int FOAM = ACCENT_SECONDARY;

    // ——— ГРАНИЦЫ ———
    /** Граница карточек rgba(255,107,53,0.2) */
    public static final int BORDER = 0x33FF6B35;
    public static final int BORDER_SUBTLE = 0x20475569;
    public static final int BORDER_HOVER = 0x80FF6B35;
    public static final int BORDER_WIDTH = 1;
    /** Левый бордер активного пункта навигации 3px */
    public static final int BORDER_ACTIVE_LEFT = 3;

    // ——— Семантика ———
    public static final int SUCCESS = 0xFF10B981;
    public static final int DISABLED = 0xFF475569;

    // ——— РАДИУСЫ (8–12px) ———
    public static final int RADIUS_SMALL = 6;
    public static final int RADIUS_MED = 8;
    public static final int RADIUS_LARGE = 12;

    // ——— SPACING (8px grid) ———
    public static final int SPACE_4 = 4;
    public static final int SPACE_8 = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_24 = 24;
    public static final int SPACE_32 = 32;

    // ——— LAYOUT ———
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
    public static final int SETTINGS_PAD_X = 16;
    public static final int HOVER_DESC_H = 12;
    public static final int SCROLLBAR_WIDTH = 6;
    public static final int SCROLLBAR_THUMB_RADIUS = 3;
    public static final int SCROLLBAR_GUTTER = 10;
    /** Ширина левой панели навигации */
    public static final int SIDEBAR_WIDTH = 280;
    /** Ширина центральной панели модулей */
    public static final int MODULE_PANEL_DEFAULT_WIDTH = 280;
    /** Ширина правой панели настроек */
    public static final int SETTINGS_PANEL_DEFAULT_WIDTH = 400;
    public static final int SETTINGS_PANEL_MIN_WIDTH = 360;
    public static final int SETTINGS_ROW_MIN_WIDTH = 280;
}
