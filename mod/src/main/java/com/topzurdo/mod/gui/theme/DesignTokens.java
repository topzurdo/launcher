package com.topzurdo.mod.gui.theme;

import com.topzurdo.mod.gui.OceanTheme;

/**
 * Design system для Tactical HUD: роли цветов, 8px-сетка, один accent.
 *
 * <p>Семантика по концепции редизайна:
 * <ul>
 *   <li>Background: base (основной фон), elevated (карточки/панели), overlay (модалки)</li>
 *   <li>Foreground: primary (основной текст), secondary (второстепенный), disabled</li>
 *   <li>Accent: золото для primary интерактива, циан для secondary</li>
 *   <li>Semantic: success, warning, danger — не зависят от темы</li>
 * </ul>
 * </p>
 *
 * <p>Реализация по умолчанию делегирует в {@link OceanTheme}. В будущем возможен
 * runtime theme provider (dark/light/custom).</p>
 */
public final class DesignTokens {

    private DesignTokens() {}

    // ─── Spacing (8px grid) ───
    public static final int XS = 4;
    public static final int SM = 8;
    public static final int MD = 12;
    public static final int LG = 16;
    public static final int XL = 24;
    public static final int XXL = 32;

    // ─── Radius: один 6px для всех элементов (кроме round-кнопок 50%) ───
    public static final int RADIUS = 6;
    /** Для круглых кнопок — интерпретировать как «половина меньшей стороны». */
    public static final int RADIUS_ROUND = -1;

    // ─── Background (делегация в OceanTheme) ───
    public static int bgBase()       { return OceanTheme.BG_PANEL; }
    public static int bgPanel()      { return OceanTheme.BG_PANEL; }
    public static int bgElevated()   { return OceanTheme.BG_HEADER; }
    public static int bgOverlay()    { return OceanTheme.BG_OVERLAY; }
    public static int bgInner()      { return OceanTheme.BG_INNER; }

    // ─── Foreground ───
    public static int fgPrimary()    { return OceanTheme.TEXT; }
    public static int fgSecondary()  { return OceanTheme.TEXT_DIM; }
    public static int fgMuted()      { return OceanTheme.TEXT_MUTED; }
    public static int fgDisabled()   { return OceanTheme.TEXT_MUTED; }
    public static int fgFoam()       { return OceanTheme.FOAM; }

    // ─── Accent (primary gold, secondary cyan) ───
    public static int accentBase()   { return OceanTheme.ACCENT; }
    public static int accentHover()  { return OceanTheme.ACCENT; }
    public static int accentActive() { return OceanTheme.ACCENT_DARK; }
    public static int accentSecondary() { return OceanTheme.ACCENT_SECONDARY; }
    public static int accentSecondaryActive() { return OceanTheme.ACCENT_SECONDARY_DARK; }

    // ─── Semantic ───
    public static int success()      { return OceanTheme.SUCCESS; }
    public static int disabled()     { return OceanTheme.DISABLED; }
    /** Жёлтый — deprecated/предупреждение. */
    public static int warning()      { return 0xFFE2B10A; }
    /** Красный — удаление, ошибка. */
    public static int danger()       { return 0xFFE53935; }

    // ─── Border ───
    public static int border()       { return OceanTheme.BORDER; }
    public static int borderSubtle() { return OceanTheme.BORDER_SUBTLE; }
    public static int borderWidth()  { return OceanTheme.BORDER_WIDTH; }
}
