package com.topzurdo.mod.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.config.ModConfig;
import com.topzurdo.mod.gui.components.atoms.ColorPicker;
import com.topzurdo.mod.gui.components.atoms.Selector;
import com.topzurdo.mod.gui.components.atoms.Slider;
import com.topzurdo.mod.gui.components.atoms.TextInput;
import com.topzurdo.mod.gui.components.atoms.Toggle;
import com.topzurdo.mod.gui.components.molecules.CategoryTab;
import com.topzurdo.mod.gui.components.molecules.ModuleCard;
import com.topzurdo.mod.gui.components.molecules.ScrollContainer;
import com.topzurdo.mod.gui.components.molecules.SettingRow;
import com.topzurdo.mod.gui.theme.DesignTokens;
import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.ModuleManager;
import com.topzurdo.mod.modules.Setting;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.MathHelper;

/**
 * Фаза 4: новый экран меню на VStack/HStack, молекулах (CategoryTab, ModuleCard, SettingRow, ScrollContainer)
 * и атомах (Toggle, Slider, Selector). Навигация, state, анимация открытия ~150ms.
 */
public class TopZurdoMenuScreen extends Screen {

    private int guiLeft, guiTop, guiWidth, guiHeight;
    private int categoryPanelWidth = 118;
    private int modulePanelWidth = 205;
    private int settingsPanelWidth;
    /** Adaptive scale factor so UI fits at any GUI Scale (1-4). Based on reference 640x420. */
    private double uiScaleFactor = 1.0;
    /** Scaled layout values (computed in init from uiScaleFactor). */
    private int scaledTitleBarH, scaledContentBottomOffset, scaledCardH, scaledCardGap, scaledModulePanelPadTop;

    private Module.Category selectedCategory = Module.Category.RENDER;
    private Module selectedModule = null;
    private float openAnimation = 0f;
    private float openAnimationPrev = 0f;
    private float categoryAnimation = 0f;
    private float categoryAnimationPrev = 0f;

    private ModuleManager moduleManager;
    private final Map<Module, ModuleCard> moduleCards = new HashMap<>();
    private List<SettingRow> settingRows = new ArrayList<>();
    private List<Setting<?>> settingsList = new ArrayList<>();
    private ScrollContainer moduleScroll;
    private ScrollContainer settingsScroll;
    private int totalSettingsHeight = 0;

    private final Screen parent;
    private long handCursor = 0;
    private boolean moduleScrollbarDragging;
    private boolean settingsScrollbarDragging;

    private static final int CARD_H = 52;
    private static final int CARD_GAP = 4;
    private static final int SCROLL_DELTA = 25;
    private static final int TITLE_BAR_H = 48;
    private static final int MODULE_PANEL_PAD_TOP = 14;
    private static final int SETTINGS_ROW_EXTRA_PADDING = 12;

    /** Footer debug-logging button bounds (computed in init). */
    private int footerLogX, footerLogY, footerLogW, footerLogH;

    /**
     * Constructs a TopZurdoMenuScreen with no parent screen.
     *
     * Use the single-argument constructor to create the screen with a parent.
     */
    public TopZurdoMenuScreen() {
        this(null);
    }

    public TopZurdoMenuScreen(Screen parent) {
        super(new LiteralText("TopZurdo"));
        this.parent = parent;
    }

    /**
     * Initializes the menu screen: computes adaptive layout, restores UI state, and constructs UI components.
     *
     * Sets up scaled GUI metrics and panel widths, restores the previously selected category and module,
     * creates module cards, and initializes the module and settings scroll containers. Also restores scroll
     * offsets, computes footer debug-button bounds, and creates the hand cursor used for hover interactions.
     */
    @Override
    protected void init() {
        super.init();
        try {
        moduleManager = TopZurdoMod.getModuleManager();
        if (moduleManager == null) {
            if (client != null) client.openScreen(parent);
            return;
        }
        // Layout: левая 280px | центр 280px | правая 400px (референс 960×540)
        final int refWidth = 960;
        final int refHeight = 540;
        uiScaleFactor = Math.min(1.0, Math.min((double) (width - 40) / refWidth, (double) (height - OceanTheme.BOTTOM_MARGIN) / refHeight));
        uiScaleFactor = Math.max(0.6, uiScaleFactor);
        guiWidth = (int) (refWidth * uiScaleFactor);
        guiHeight = (int) (refHeight * uiScaleFactor);
        categoryPanelWidth = (int) (OceanTheme.SIDEBAR_WIDTH * uiScaleFactor);
        modulePanelWidth = (int) (OceanTheme.MODULE_PANEL_DEFAULT_WIDTH * uiScaleFactor);
        settingsPanelWidth = Math.max((int)(OceanTheme.SETTINGS_PANEL_MIN_WIDTH * uiScaleFactor), (int)(OceanTheme.SETTINGS_PANEL_DEFAULT_WIDTH * uiScaleFactor));
        if (categoryPanelWidth + modulePanelWidth + settingsPanelWidth + 24 > guiWidth) {
            settingsPanelWidth = Math.max(OceanTheme.SETTINGS_PANEL_MIN_WIDTH, guiWidth - categoryPanelWidth - modulePanelWidth - 24);
        }
        guiLeft = (width - guiWidth) / 2;
        guiTop = (height - guiHeight) / 2;

        // Внутренний лейаут без уменьшения — текст и кнопки остаются читаемыми
        scaledTitleBarH = TITLE_BAR_H;
        scaledContentBottomOffset = OceanTheme.CONTENT_BOTTOM_OFFSET;
        scaledCardH = CARD_H;
        scaledCardGap = CARD_GAP;
        scaledModulePanelPadTop = MODULE_PANEL_PAD_TOP;

        openAnimation = 1f;
        openAnimationPrev = 1f;
        categoryAnimation = 1f;
        categoryAnimationPrev = 1f;

        restoreUIState();

        int modContentWidth = modulePanelWidth - 12 - OceanTheme.SCROLLBAR_GUTTER;
        int setContentWidth = settingsPanelWidth - 12 - OceanTheme.SCROLLBAR_GUTTER;

        createModuleCards(modContentWidth);
        int modContentTop = guiTop + scaledTitleBarH + scaledModulePanelPadTop;
        int contentBottom = guiTop + guiHeight - scaledContentBottomOffset;
        int modVisibleH = contentBottom - modContentTop;
        int modTotalH = moduleManager.getModulesByCategory(selectedCategory).size() * (scaledCardH + scaledCardGap);
        int modPanelX = guiLeft + categoryPanelWidth + 6;
        moduleScroll = new ScrollContainer(modPanelX, modContentTop, modulePanelWidth - 12, modVisibleH, modTotalH);
        moduleScroll.setContentZoneWidth(modContentWidth);

        int setContentTop = guiTop + scaledTitleBarH + OceanTheme.SETTINGS_HEADER_H;
        int setVisibleH = contentBottom - setContentTop;
        settingsScroll = new ScrollContainer(guiLeft + categoryPanelWidth + modulePanelWidth + 6, setContentTop, settingsPanelWidth - 12, setVisibleH, 0);
        settingsScroll.setContentZoneWidth(setContentWidth);

        selectRestoredModule();
        restoreScrollOffsets();

        int footerRowTop = guiTop + guiHeight - OceanTheme.FOOTER_H;
        String logOn = I18n.translate("topzurdo.gui.debug_logging.on");
        String logOff = I18n.translate("topzurdo.gui.debug_logging.off");
        int logW1 = client != null && client.textRenderer != null ? client.textRenderer.getWidth(logOn) : 120;
        int logW2 = client != null && client.textRenderer != null ? client.textRenderer.getWidth(logOff) : 120;
        footerLogW = Math.max(logW1, logW2) + OceanTheme.SPACE_12 * 2;
        footerLogH = 20;
        footerLogX = guiLeft + guiWidth - footerLogW - OceanTheme.SPACE_12;
        footerLogY = footerRowTop + (OceanTheme.FOOTER_H - footerLogH) / 2;

        handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        } catch (Throwable t) {
            if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().error("[TopZurdo] TopZurdoMenuScreen.init", t);
            throw t;
        }
    }

    private void createModuleCards(int moduleContentWidth) {
        moduleCards.clear();
        int cardW = Math.max(100, moduleContentWidth - 16);
        for (Module m : moduleManager.getAllModules()) {
            ModuleCard card = new ModuleCard(0, 0, cardW, scaledCardH, m, this::onModuleSelected);
            moduleCards.put(m, card);
        }
    }

    private void onModuleSelected(Module mod) {
        selectedModule = mod;
        updateSettingsComponents();
    }

    private void restoreUIState() {
        ModConfig config = TopZurdoMod.getConfig();
        if (config == null) return;
        String cat = config.getLastCategory();
        if (cat != null && !cat.isEmpty()) {
            try {
                selectedCategory = Module.Category.valueOf(cat);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void selectRestoredModule() {
        ModConfig config = TopZurdoMod.getConfig();
        String lastId = config != null ? config.getLastModuleId() : "";
        List<Module> modules = moduleManager.getModulesByCategory(selectedCategory);
        if (lastId != null && !lastId.isEmpty()) {
            for (Module m : modules) {
                if (lastId.equals(m.getId())) {
                    selectedModule = m;
                    updateSettingsComponents();
                    return;
                }
            }
        }
        if (!modules.isEmpty()) {
            selectedModule = modules.get(0);
            updateSettingsComponents();
        } else {
            selectedModule = null;
            updateSettingsComponents();
        }
    }

    private void restoreScrollOffsets() {
        ModConfig config = TopZurdoMod.getConfig();
        if (config == null) return;
        if (moduleScroll != null) {
            int off = config.getModuleScrollOffset();
            moduleScroll.setScrollOffset(Math.min(off, moduleScroll.getMaxScroll()));
        }
        if (settingsScroll != null) {
            int off = config.getSettingsScrollOffset();
            settingsScroll.setScrollOffset(Math.min(off, settingsScroll.getMaxScroll()));
        }
    }

    private void saveUIState() {
        ModConfig config = TopZurdoMod.getConfig();
        if (config == null) return;
        config.setLastCategory(selectedCategory != null ? selectedCategory.name() : "RENDER");
        config.setLastModuleId(selectedModule != null ? selectedModule.getId() : "");
        if (moduleScroll != null) config.setModuleScrollOffset(moduleScroll.getScrollOffset());
        if (settingsScroll != null) config.setSettingsScrollOffset(settingsScroll.getScrollOffset());
    }

    /**
     * Rebuilds the settings UI for the currently selected module.
     *
     * Clears existing setting rows and the backing settings list, then if a module is selected
     * creates a SettingRow for each of its settings using the panel-derived row width. Each row
     * is wired with an onChanged handler that persists the setting value and—when the selected
     * module is a ContainerSearcherModule and the setting is a string—updates that module's
     * search query. After creating rows, updates the total settings content height and, if a
     * settings scroll container exists, sets its content height and resets its scroll offset.
     */
    private void updateSettingsComponents() {
        settingRows.clear();
        settingsList.clear();
        totalSettingsHeight = 0;
        if (selectedModule == null) {
            if (settingsScroll != null) settingsScroll.setContentHeight(0);
            return;
        }

        int settingsContentWidth = settingsPanelWidth - 12 - OceanTheme.SCROLLBAR_GUTTER;
        int w = Math.max(OceanTheme.SETTINGS_ROW_MIN_WIDTH, settingsContentWidth - OceanTheme.SETTINGS_PAD_X * 2 - SETTINGS_ROW_EXTRA_PADDING);
        String moduleId = selectedModule.getId();
        for (Setting<?> s : selectedModule.getSettings()) {
            if (s == null) continue;
            Runnable onChanged = () -> {
                if (selectedModule != null) {
                    s.save(moduleId);
                    if (selectedModule instanceof com.topzurdo.mod.modules.utility.ContainerSearcherModule && s.isString()) {
                        String v = (String) s.getValue();
                        ((com.topzurdo.mod.modules.utility.ContainerSearcherModule) selectedModule).setSearchQuery(v != null ? v : "");
                    }
                }
            };
            SettingRow row = new SettingRow(0, 0, w, s, onChanged);
            settingRows.add(row);
            settingsList.add(s);
        }
        totalSettingsHeight = 0;
        for (SettingRow r : settingRows) totalSettingsHeight += r.getHeight();

        if (settingsScroll != null) {
            settingsScroll.setContentHeight(totalSettingsHeight);
            settingsScroll.setScrollOffset(0);
        }
    }

    @Override
    public void tick() {
        super.tick();
        UIRenderHelper.tickAnimation();

        openAnimationPrev = openAnimation;
        openAnimation = 1f;
        categoryAnimationPrev = categoryAnimation;
        categoryAnimation = 1f;

        for (Map.Entry<Module, ModuleCard> e : moduleCards.entrySet()) {
            e.getValue().setEnabled(e.getKey().isEnabled());
            e.getValue().setSelected(e.getKey() == selectedModule);
            e.getValue().tick();
        }
        for (SettingRow row : settingRows) row.tick();
    }

    /**
     * Render the TopZurdo menu screen, drawing the background, UI panels, controls, and footer,
     * and updating the mouse cursor for interactive regions.
     *
     * This method paints the full screen UI (background, title bar, category/module/settings panels,
     * shadows, and footer) and then delegates to super.render to allow child components to draw.
     *
     * @param ms the current MatrixStack for rendering transforms
     * @param mouseX the current mouse x position in screen coordinates
     * @param mouseY the current mouse y position in screen coordinates
     * @param partialTicks interpolation value between ticks for smooth animations
     */
    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        try {
        UIRenderHelper.setPartialTicks(partialTicks);

        float rawOpen = openAnimationPrev + (openAnimation - openAnimationPrev) * partialTicks;
        float eased = getEasedOpen(rawOpen);
        float rawCat = categoryAnimationPrev + (categoryAnimation - categoryAnimationPrev) * partialTicks;

        // Фон: градиент #0a0e1a → #1a1f2e (геймерская тема)
        UIRenderHelper.fillVerticalGradient(ms, 0, 0, width, height, OceanTheme.BG_GRADIENT_TOP, OceanTheme.BG_GRADIENT_BOTTOM);

        // Лёгкая сетка для глубины
        int gridColor = UIRenderHelper.withAlpha(OceanTheme.ACCENT_SECONDARY, 0.03f);
        for (int gx = 0; gx < width; gx += 40) {
            UIRenderHelper.fill(ms, gx, 0, gx + 1, height, gridColor);
        }
        for (int gy = 0; gy < height; gy += 40) {
            UIRenderHelper.fill(ms, 0, gy, width, gy + 1, gridColor);
        }

        // Overlay затемнения при открытии
        int overlayAlpha = (int) ((OceanTheme.BG_OVERLAY >>> 24) * (1f - eased * 0.3f));
        UIRenderHelper.fill(ms, 0, 0, width, height, (Math.min(255, overlayAlpha) << 24) | (OceanTheme.BG_OVERLAY & 0x00FFFFFF));

        ms.push();
        int lmx = (int) toLogicalX(mouseX);
        int lmy = (int) toLogicalY(mouseY);

        drawOceanPanel(ms, guiLeft, guiTop, guiWidth, guiHeight, categoryPanelWidth);
        drawPanelShadow(ms, guiLeft, guiTop, guiWidth, guiHeight, 1f);
        renderTitleBar(ms);
        renderCategoryPanel(ms, lmx, lmy, partialTicks);

        int contentBottom = guiTop + guiHeight - scaledContentBottomOffset;
        renderModulePanel(ms, lmx, lmy, contentBottom, eased, rawCat, partialTicks);

        renderSettingsPanel(ms, lmx, lmy, contentBottom, eased, partialTicks);
        renderFooter(ms, lmx, lmy);

        ms.pop();

        // Cursor pointer for interactive components
        if (handCursor != 0 && client != null && client.getWindow() != null) {
            long win = client.getWindow().getHandle();
            boolean overInteractive = isOverInteractiveComponent(lmx, lmy);
            GLFW.glfwSetCursor(win, overInteractive ? handCursor : 0);
        }

        super.render(ms, mouseX, mouseY, partialTicks);
        } catch (Throwable t) {
            if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().error("[TopZurdo] TopZurdoMenuScreen.render", t);
            throw t;
        }
    }

    private void renderTitleBar(MatrixStack ms) {
        String title = "TOPZURDO";
        int tw = textRenderer.getWidth(title);
        float scale = 1.35f;
        int cx = guiLeft + guiWidth / 2;
        int ty = guiTop + 14;

        ms.push();
        ms.translate(cx, ty, 0);
        ms.scale(scale, scale, 1f);

        // Лёгкое свечение заголовка (primary accent)
        int glowColor = UIRenderHelper.withAlpha(OceanTheme.ACCENT, 0.25f);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    textRenderer.draw(ms, title, -tw / 2f + dx, dy, glowColor);
                }
            }
        }

        float x = -tw / 2f;
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            float t = (float) i / Math.max(1, title.length() - 1);
            int color = UIRenderHelper.lerpColor(OceanTheme.ACCENT, OceanTheme.ACCENT_SECONDARY, t);
            textRenderer.draw(ms, String.valueOf(c), x, 0, color);
            x += textRenderer.getWidth(String.valueOf(c));
        }
        ms.pop();

        int leftEdge = (int) (cx - tw * scale / 2);
        UIRenderHelper.fill(ms, leftEdge - 10, ty + 5, leftEdge - 4, ty + 7, OceanTheme.ACCENT);
        UIRenderHelper.fill(ms, cx + (int)(tw * scale / 2) + 4, ty + 5, cx + (int)(tw * scale / 2) + 10, ty + 7, OceanTheme.ACCENT_SECONDARY);

        String sub = I18n.translate("topzurdo.gui.subtitle");
        textRenderer.drawWithShadow(ms, sub, guiLeft + (guiWidth - textRenderer.getWidth(sub)) / 2f, guiTop + 30, OceanTheme.TEXT_SECONDARY);

        UIRenderHelper.fill(ms, guiLeft + 24, guiTop + 44, guiLeft + guiWidth - 24, guiTop + 45, OceanTheme.BORDER);
    }

    private void renderCategoryPanel(MatrixStack ms, int lmx, int lmy, float partialTicks) {
        int x = guiLeft + OceanTheme.SPACE_8;
        int y = guiTop + scaledTitleBarH;
        int catBtnW = categoryPanelWidth - OceanTheme.SPACE_12;
        int catBtnH = 36;
        int catStep = 40;
        for (Module.Category cat : Module.Category.values()) {
            CategoryTab tab = new CategoryTab(x, y, catBtnW, catBtnH, cat, c -> { selectedCategory = c; });
            tab.setSelected(cat == selectedCategory);
            tab.render(ms, lmx, lmy, partialTicks);
            y += catStep;
        }
    }

    private static String iconForCategory(Module.Category c) {
        switch (c) {
            case RENDER:    return "\u25C6"; // ◆
            case HUD:       return "\u25CF"; // ●
            case UTILITY:   return "\u25C7"; // ◇
            case PERFORMANCE: return "\u26A1"; // ⚡
            default:        return "";
        }
    }

    private String elegantCategoryName(Module.Category c) {
        String key = "topzurdo.gui.category." + c.name().toLowerCase();
        return I18n.hasTranslation(key) ? I18n.translate(key) : c.getName().toUpperCase();
    }

    private void renderModulePanel(MatrixStack ms, int lmx, int lmy, int contentBottom, float eased, float rawCat, float partialTicks) {
        List<Module> modules = moduleManager.getModulesByCategory(selectedCategory);
        int panelX = guiLeft + categoryPanelWidth + 6;
        int panelY = guiTop + scaledTitleBarH;
        int modContentTop = panelY + scaledModulePanelPadTop;
        int modVisibleH = contentBottom - modContentTop;

        if (moduleScroll != null) {
            moduleScroll.setContentHeight(modules.size() * (scaledCardH + scaledCardGap));
        }

        int modContentWidth = modulePanelWidth - 12 - OceanTheme.SCROLLBAR_GUTTER;
        textRenderer.drawWithShadow(ms, I18n.translate("topzurdo.gui.modules"), panelX + 8, panelY, OceanTheme.ACCENT_SECONDARY);
        applyScissor(panelX, modContentTop, modContentWidth, modVisibleH);
        ms.push();
        int off = moduleScroll != null ? moduleScroll.getScrollOffset() : 0;
        int y = modContentTop - off;
        for (Module m : modules) {
            ModuleCard card = moduleCards.get(m);
            if (card != null) {
                card.setEnabled(m.isEnabled());
                card.setSelected(m == selectedModule);
                card.setAlphaMultiplier(rawCat);
                card.setPartialTicks(partialTicks);
                card.setPosition(panelX + 4, y);
                if (y + scaledCardH >= panelY + 12 && y < contentBottom - 2)
                    card.render(ms, lmx, lmy, partialTicks);
            }
            y += scaledCardH + scaledCardGap;
        }
        ms.pop();
        RenderSystem.disableScissor();
        if (moduleScroll != null && moduleScroll.getMaxScroll() > 0)
            moduleScroll.renderScrollbar(ms, lmx, lmy);
    }

    private void renderSettingsPanel(MatrixStack ms, int lmx, int lmy, int contentBottom, float eased, float partialTicks) {
        int panelX = guiLeft + categoryPanelWidth + modulePanelWidth + 6;
        int panelW = settingsPanelWidth;
        int baseY = guiTop + scaledTitleBarH;
        int setContentTop = baseY + OceanTheme.SETTINGS_HEADER_H;
        int setContentBottom = contentBottom;
        int visibleH = setContentBottom - setContentTop;

        settingsScroll.setContentHeight(totalSettingsHeight);

        int headerContentW = settingsPanelWidth - 12 - OceanTheme.SCROLLBAR_GUTTER - 24;
        if (selectedModule == null) {
            DrawableHelper.drawCenteredText(ms, textRenderer, I18n.translate("topzurdo.gui.select_module"), panelX + headerContentW / 2, baseY + 60, DesignTokens.fgSecondary());
            return;
        }
        String name = GuiUtil.resolveKey(selectedModule.getName());
        String nameUp = GuiUtil.truncate(textRenderer, name.toUpperCase(), Math.min(panelW - 24, headerContentW));
        textRenderer.drawWithShadow(ms, nameUp, panelX + (headerContentW - textRenderer.getWidth(nameUp)) / 2f, baseY, OceanTheme.ACCENT);
        String status = selectedModule.isEnabled() ? I18n.translate("topzurdo.module.enabled") : I18n.translate("topzurdo.module.disabled");
        textRenderer.drawWithShadow(ms, status, panelX + (headerContentW - textRenderer.getWidth(status)) / 2f, baseY + 10, selectedModule.isEnabled() ? OceanTheme.SUCCESS : DesignTokens.fgSecondary());
        drawHorizontalLine(ms, panelX + OceanTheme.SPACE_16, baseY + 22, headerContentW - OceanTheme.SPACE_16);
        String desc = GuiUtil.resolveKey(selectedModule.getDescription());
        DrawableHelper.drawCenteredText(ms, textRenderer, GuiUtil.truncate(textRenderer, desc, headerContentW), panelX + headerContentW / 2, baseY + 34, DesignTokens.fgSecondary());

        if (settingRows.isEmpty()) {
            DrawableHelper.drawCenteredText(ms, textRenderer, I18n.translate("topzurdo.gui.no_settings"), panelX + headerContentW / 2, baseY + 90, DesignTokens.fgMuted());
        } else {
            int off = settingsScroll.getScrollOffset();
            int adjMy = lmy + off;
            int settingsContentWidth = settingsPanelWidth - 12 - OceanTheme.SCROLLBAR_GUTTER;
            int settingsX = panelX + OceanTheme.SETTINGS_PAD_X;
            int rowY = setContentTop;
            String hoveredDesc = null;
            applyScissor(panelX, setContentTop, settingsContentWidth, visibleH);
            ms.push();
            ms.translate(0, -off, 0);
            for (SettingRow row : settingRows) {
                row.setPosition(settingsX, rowY);
                row.render(ms, lmx, adjMy);
                if (isSettingRowHovered(row, lmx, adjMy)) {
                    Setting<?> st = row.getSetting();
                    if (st != null) hoveredDesc = GuiUtil.resolveKey(st.getDescription());
                }
                rowY += row.getHeight();
            }
            ms.pop();
            if (settingsScroll.getMaxScroll() > 0)
                settingsScroll.renderScrollbar(ms, lmx, lmy);
            RenderSystem.disableScissor();
            rowY = setContentTop;
            ms.push();
            ms.translate(0, -off, 0);
            for (SettingRow row : settingRows) {
                row.setPosition(settingsX, rowY);
                UIComponent c = row.getControl();
                if (c instanceof ColorPicker && ((ColorPicker) c).isExpanded()) {
                    ((ColorPicker) c).renderExpandedPart(ms, lmx, adjMy);
                }
                if (c instanceof Selector && ((Selector) c).isExpanded()) {
                    ((Selector) c).renderExpandedPart(ms, lmx, adjMy, setContentBottom, off);
                }
                rowY += row.getHeight();
            }
            ms.pop();
            if (hoveredDesc != null) {
                hoveredDesc = GuiUtil.truncate(textRenderer, hoveredDesc, headerContentW);
                int hoverY = guiTop + guiHeight - scaledContentBottomOffset + 4;
                textRenderer.drawWithShadow(ms, hoveredDesc, panelX + OceanTheme.SPACE_8, hoverY, DesignTokens.fgMuted());
            }
        }
    }

    /**
     * Determines whether the specified mouse coordinates lie over any interactive UI element (category tabs, module cards, or any setting control),
     * taking current scroll offsets into account.
     *
     * @param mx the mouse x-coordinate in GUI/screen space
     * @param my the mouse y-coordinate in GUI/screen space
     * @return `true` if the coordinates are over an interactive component, `false` otherwise
     */
    private boolean isOverInteractiveComponent(int mx, int my) {
        int localMy = my;
        int off = settingsScroll != null ? settingsScroll.getScrollOffset() : 0;
        int adjMy = localMy + off;

        // Categories
        int catY = guiTop + scaledTitleBarH, catBtnH = 36, catStep = 40;
        int catLeft = guiLeft + OceanTheme.SPACE_8, catBtnW = categoryPanelWidth - OceanTheme.SPACE_12;
        for (Module.Category cat : Module.Category.values()) {
            if (mx >= catLeft && mx < catLeft + catBtnW && my >= catY && my < catY + catBtnH) return true;
            catY += catStep;
        }

        // Module cards
        List<Module> modules = moduleManager != null ? moduleManager.getModulesByCategory(selectedCategory) : java.util.Collections.emptyList();
        int modPanelX = guiLeft + categoryPanelWidth + 6;
        int modOff = moduleScroll != null ? moduleScroll.getScrollOffset() : 0;
        int modContentTop = guiTop + scaledTitleBarH + scaledModulePanelPadTop;
        int y = modContentTop - modOff;
        for (Module m : modules) {
            ModuleCard card = moduleCards.get(m);
            if (card != null && mx >= modPanelX + 4 && mx < modPanelX + 4 + (modulePanelWidth - 16) && my >= y && my < y + scaledCardH)
                return true;
            y += scaledCardH + scaledCardGap;
        }

        // Setting rows
        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c != null && (c instanceof Toggle && ((Toggle) c).isMouseOver(mx, adjMy)
                || c instanceof Slider && ((Slider) c).isMouseOver(mx, adjMy)
                || c instanceof Selector && ((Selector) c).isMouseOver(mx, adjMy)
                || c instanceof ColorPicker && ((ColorPicker) c).isMouseOver(mx, adjMy)
                || c instanceof TextInput && ((TextInput) c).isMouseOver(mx, adjMy)))
                return true;
        }

        return false;
    }

    private boolean isSettingRowHovered(SettingRow row, int mx, int my) {
        UIComponent c = row.getControl();
        if (c == null) return false;
        if (c instanceof Toggle) return ((Toggle) c).isMouseOver(mx, my);
        if (c instanceof Slider) return ((Slider) c).isMouseOver(mx, my);
        if (c instanceof Selector) return ((Selector) c).isMouseOver(mx, my);
        if (c instanceof TextInput) return ((TextInput) c).isMouseOver(mx, my);
        if (c instanceof ColorPicker) return ((ColorPicker) c).isMouseOver(mx, my);
        return false;
    }

    /**
     * Renders the screen footer: a wrapped hint text block and a toggleable debug-logging button.
     *
     * The footer displays one or more wrapped hint lines at the left and a debug state button at the right.
     * The debug button shows the current debug label (on/off), highlights when hovered, and renders a glowing border
     * and background according to the hover state and theme colors.
     *
     * @param ms      the current matrix stack for rendering
     * @param mouseX  current mouse X position in screen coordinates
     * @param mouseY  current mouse Y position in screen coordinates
     */
    private void renderFooter(MatrixStack ms, int mouseX, int mouseY) {
        int footerRowTop = guiTop + guiHeight - OceanTheme.FOOTER_H;
        int hintMaxW = guiWidth - OceanTheme.SPACE_24 - (footerLogW + OceanTheme.SPACE_12);
        List<String> lines = GuiUtil.wrapHint(textRenderer, I18n.translate("topzurdo.gui.footer"), hintMaxW);
        int hoverZoneTop = guiTop + guiHeight - scaledContentBottomOffset;
        int fy = lines.size() > 1 ? hoverZoneTop + 6 : footerRowTop + (OceanTheme.FOOTER_H - 9) / 2;
        for (int i = 0; i < lines.size(); i++)
            textRenderer.drawWithShadow(ms, lines.get(i), guiLeft + OceanTheme.SPACE_12, fy + i * 10, DesignTokens.fgMuted());

        ModConfig cfg = TopZurdoMod.getConfig();
        boolean debugOn = cfg != null && cfg.isDebugLogging();
        String logLabel = I18n.translate(debugOn ? "topzurdo.gui.debug_logging.on" : "topzurdo.gui.debug_logging.off");
        boolean hover = mouseX >= footerLogX && mouseX < footerLogX + footerLogW && mouseY >= footerLogY && mouseY < footerLogY + footerLogH;
        int btnBg = hover ? UIRenderHelper.withAlpha(OceanTheme.ACCENT, 0.25f) : UIRenderHelper.withAlpha(OceanTheme.BG_CARD, 0.9f);
        UIRenderHelper.fill(ms, footerLogX, footerLogY, footerLogX + footerLogW, footerLogY + footerLogH, btnBg);
        UIRenderHelper.drawGlowingBorder(ms, footerLogX, footerLogY, footerLogW, footerLogH, UIRenderHelper.withAlpha(OceanTheme.NEON_PURPLE, hover ? 0.6f : 0.35f), 1);
        textRenderer.drawWithShadow(ms, logLabel, footerLogX + (footerLogW - textRenderer.getWidth(logLabel)) / 2, footerLogY + (footerLogH - 9) / 2, debugOn ? OceanTheme.ACCENT : DesignTokens.fgMuted());
    }

    /**
     * Renders an "ocean" themed panel with layered glow and decorative accents.
     *
     * The panel consists of a soft multi-layered glow, a header band, the main
     * panel body, an optional colored category column, a glowing border, a thin
     * top highlight line, and corner accent marks.
     *
     * @param ms   the current matrix stack for rendering transforms
     * @param x    the left X coordinate of the panel
     * @param y    the top Y coordinate of the panel
     * @param w    the width of the panel
     * @param h    the height of the panel
     * @param catW the width of the left category column; if <= 0 no column is drawn
     */
    private void drawOceanPanel(MatrixStack ms, int x, int y, int w, int h, int catW) {
        int glowColor = UIRenderHelper.withAlpha(OceanTheme.NEON_PURPLE, 0.08f);
        for (int i = 4; i > 0; i -= 2) {
            float alpha = (float) i / 4 * 0.06f;
            int layerColor = UIRenderHelper.withAlpha(OceanTheme.NEON_PURPLE, alpha);
            UIRenderHelper.fill(ms, x - i, y - i, x + w + i, y + h + i, layerColor);
        }

        int headerH = 48;
        UIRenderHelper.fill(ms, x, y, x + w, y + headerH, OceanTheme.BG_HEADER);
        UIRenderHelper.fill(ms, x, y + headerH, x + w, y + h, OceanTheme.BG_PANEL);
        if (catW > 0) UIRenderHelper.fill(ms, x + 1, y + headerH, x + catW, y + h - 1, OceanTheme.BG_HEADER);

        UIRenderHelper.drawGlowingBorder(ms, x, y, w, h, UIRenderHelper.withAlpha(OceanTheme.NEON_PURPLE, 0.5f), 2);

        // Top highlight
        UIRenderHelper.fill(ms, x + 1, y + 1, x + w - 1, y + 2, UIRenderHelper.withAlpha(0xFFFFFFFF, 0.08f));

        // Corner accents
        UIRenderHelper.drawCornerAccents(ms, x, y, w, h, OceanTheme.NEON_CYAN, 16);
    }

    private void drawPanelShadow(MatrixStack ms, int x, int y, int w, int h, float eased) {
        for (int i = 0; i < 8; i++) {
            int a = (int) ((1f - (float) i / 8) * 32 * eased);
            if (a <= 0) break;
            UIRenderHelper.fill(ms, x + 4, y + h + i, x + w - 4, y + h + i + 1, (a << 24));
        }
    }

    private void drawHorizontalLine(MatrixStack ms, int x, int y, int w) {
        for (int i = 0; i < w; i++) {
            float t = (float) i / w;
            int a = (int) ((1f - Math.abs(2f * t - 1f)) * 160);
            UIRenderHelper.fill(ms, x + i, y, x + i + 1, y + 1, (OceanTheme.ACCENT & 0x00FFFFFF) | (a << 24));
        }
    }

    private void drawVerticalLine(MatrixStack ms, int x, int y, int h) {
        for (int i = 0; i < h; i++) {
            float t = (float) i / h;
            int a = (int) ((1f - Math.abs(2f * t - 1f)) * 100);
            UIRenderHelper.fill(ms, x, y + i, x + 1, y + i + 1, (OceanTheme.ACCENT_SECONDARY & 0x00FFFFFF) | (a << 24));
        }
    }

    /** Ease-out cubic: t' = 1 - (1-t)^3 for smooth deceleration at the end. */
    private float getEasedOpen(float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private double toLogicalX(double sx) { return sx; }
    private double toLogicalY(double sy) { return sy; }

    /** Enable scissor in window pixels. (guiX, guiY, guiW, guiH) — Y down from top. */
    private void applyScissor(int guiX, int guiY, int guiW, int guiH) {
        if (client == null || client.getWindow() == null) return;
        int winW = client.getWindow().getWidth();
        int winH = client.getWindow().getHeight();
        double scaleX = (double) winW / (double) width;
        double scaleY = (double) winH / (double) height;
        int sx = (int) (guiX * scaleX);
        int sy = (int) ((height - guiY - guiH) * scaleY);
        int sw = Math.max(0, (int) (guiW * scaleX));
        int sh = Math.max(0, (int) (guiH * scaleY));
        RenderSystem.enableScissor(sx, sy, sw, sh);
    }

    /**
     * Handles a mouse click within the menu UI, dispatching the event to interactive components and performing the associated actions.
     *
     * <p>Possible observable effects include: changing the selected category or module, toggling a module, updating and focusing setting controls,
     * starting scrollbar dragging, toggling the footer debug-logging state, saving UI state, and delegating unhandled clicks to the superclass.</p>
     *
     * @param mouseX the mouse X coordinate in window space
     * @param mouseY the mouse Y coordinate in window space
     * @param button the mouse button pressed (0 = left, 1 = right, etc.)
     * @return {@code true} if the click was handled by this screen (consumed), {@code false} if it was not handled
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
        double lx = toLogicalX(mouseX);
        double ly = toLogicalY(mouseY);
        int mx = (int) lx;
        int localMy = (int) ly;

        int off = settingsScroll != null ? settingsScroll.getScrollOffset() : 0;
        int adjMy = localMy + off;
        boolean onTextInput = false;
        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c instanceof TextInput && ((TextInput) c).isMouseOver(mx, adjMy)) { onTextInput = true; break; }
        }
        if (!onTextInput) {
            for (SettingRow r : settingRows) {
                UIComponent oc = r.getControl();
                if (oc instanceof TextInput) ((TextInput) oc).setFocused(false);
            }
        }

        int catY = guiTop + scaledTitleBarH, catBtnH = 36, catStep = 40;
        int catLeft = guiLeft + OceanTheme.SPACE_8, catBtnW = categoryPanelWidth - OceanTheme.SPACE_12;
        for (Module.Category cat : Module.Category.values()) {
            if (mx >= catLeft && mx < catLeft + catBtnW && localMy >= catY && localMy < catY + catBtnH) {
                if (cat != selectedCategory) {
                    saveUIState();
                    selectedCategory = cat;
                    if (moduleScroll != null) moduleScroll.setScrollOffset(0);
                    categoryAnimation = 0f;
                    categoryAnimationPrev = 0f;
                    selectRestoredModule();
                    restoreScrollOffsets();
                    saveUIState();
                }
                return true;
            }
            catY += catStep;
        }

        List<Module> modules = moduleManager.getModulesByCategory(selectedCategory);
        for (Module m : modules) {
            ModuleCard card = moduleCards.get(m);
            if (card != null && card.isMouseOver(mx, localMy)) {
                if (button == 0) {
                    try { m.toggle(); } catch (Exception e) {
                        if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().error("[TopZurdo] Module toggle: {}", m.getId(), e);
                    }
                } else if (button == 1) {
                    selectedModule = m;
                    updateSettingsComponents();
                    saveUIState();
                }
                return true;
            }
        }

        if (button == 0 && moduleScroll != null && moduleScroll.getMaxScroll() > 0 && moduleScroll.isOverScrollbar(mx, localMy)) {
            moduleScroll.setOffsetFromScrollbarMouseY(localMy);
            moduleScrollbarDragging = true;
            return true;
        }

        // Expanded overlays first (reverse order so topmost wins): ColorPicker palette, Selector dropdown list only
        int setContentBottom = guiTop + guiHeight - scaledContentBottomOffset;
        for (int i = settingRows.size() - 1; i >= 0; i--) {
            SettingRow row = settingRows.get(i);
            UIComponent c = row.getControl();
            if (c instanceof ColorPicker && ((ColorPicker) c).isExpanded() && ((ColorPicker) c).isMouseOver(mx, adjMy)) {
                if (((ColorPicker) c).mouseClicked(lx, adjMy, button)) return true;
            }
            if (c instanceof Selector && ((Selector) c).isMouseOverDropdown(mx, adjMy, setContentBottom, off)) {
                if (((Selector) c).mouseClicked(lx, adjMy, button)) return true;
            }
        }

        for (int i = 0; i < settingRows.size(); i++) {
            SettingRow row = settingRows.get(i);
            UIComponent c = row.getControl();
            if (c instanceof Toggle) {
                if (((Toggle) c).isMouseOver(mx, adjMy)) { focusedSettingIndex = i; ((Toggle) c).onMouseClick(); return true; }
            } else if (c instanceof Slider) {
                if (((Slider) c).mouseClicked(lx, adjMy, button)) { focusedSettingIndex = i; return true; }
            } else if (c instanceof ColorPicker) {
                if (((ColorPicker) c).mouseClicked(lx, adjMy, button)) { focusedSettingIndex = i; return true; }
            } else if (c instanceof Selector) {
                if (((Selector) c).mouseClicked(lx, adjMy, button)) { focusedSettingIndex = i; return true; }
            } else if (c instanceof TextInput) {
                if (((TextInput) c).isMouseOver(mx, adjMy)) {
                    for (SettingRow r : settingRows) {
                        UIComponent oc = r.getControl();
                        if (oc instanceof TextInput) ((TextInput) oc).setFocused(false);
                    }
                    ((TextInput) c).setFocused(true);
                    focusedSettingIndex = i;
                    return true;
                }
            }
        }

        if (button == 0 && settingsScroll != null && settingsScroll.getMaxScroll() > 0 && settingsScroll.isOverScrollbar(mx, localMy)) {
            settingsScroll.setOffsetFromScrollbarMouseY(localMy);
            settingsScrollbarDragging = true;
            return true;
        }

        ModConfig cfg = TopZurdoMod.getConfig();
        if (button == 0 && cfg != null && mx >= footerLogX && mx < footerLogX + footerLogW && localMy >= footerLogY && localMy < footerLogY + footerLogH) {
            cfg.setDebugLogging(!cfg.isDebugLogging());
            String msg = I18n.translate("topzurdo.log.toggle") + " " + I18n.translate(cfg.isDebugLogging() ? "topzurdo.log.on" : "topzurdo.log.off");
            TopZurdoMod.logEvent(msg);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
        } catch (Throwable t) {
            if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().error("[TopZurdo] TopZurdoMenuScreen.mouseClicked", t);
            return false;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        try {
        if (button == 0 && (moduleScrollbarDragging || settingsScrollbarDragging)) {
            saveUIState();
            moduleScrollbarDragging = false;
            settingsScrollbarDragging = false;
            return true;
        }
        double lx = toLogicalX(mouseX);
        int adjMy = (int) toLogicalY(mouseY) + (settingsScroll != null ? settingsScroll.getScrollOffset() : 0);
        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c instanceof Slider) {
                if (((Slider) c).mouseReleased(lx, adjMy, button)) return true;
            } else if (c instanceof ColorPicker) {
                if (((ColorPicker) c).mouseReleased(lx, adjMy, button)) return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
        } catch (Throwable t) {
            if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().error("[TopZurdo] TopZurdoMenuScreen.mouseReleased", t);
            return false;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        try {
        int localMy = (int) toLogicalY(mouseY);
        if (button == 0 && moduleScrollbarDragging && moduleScroll != null) {
            moduleScroll.setOffsetFromScrollbarMouseY(localMy);
            return true;
        }
        if (button == 0 && settingsScrollbarDragging && settingsScroll != null) {
            settingsScroll.setOffsetFromScrollbarMouseY(localMy);
            return true;
        }
        double lx = toLogicalX(mouseX);
        int adjMy = localMy + (settingsScroll != null ? settingsScroll.getScrollOffset() : 0);
        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c instanceof Slider) {
                if (((Slider) c).mouseDragged(lx, adjMy, button)) return true;
            } else if (c instanceof ColorPicker) {
                if (((ColorPicker) c).mouseDragged(lx, adjMy, button)) return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        } catch (Throwable t) {
            if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().error("[TopZurdo] TopZurdoMenuScreen.mouseDragged", t);
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        try {
        int mx = (int) toLogicalX(mouseX);
        int localMy = (int) toLogicalY(mouseY);
        int off = settingsScroll != null ? settingsScroll.getScrollOffset() : 0;
        int adjMy = localMy + off;
        int contentBottom = guiTop + guiHeight - scaledContentBottomOffset;

        int modPanelX = guiLeft + categoryPanelWidth, modPanelW = modulePanelWidth;
        int modContentTop = guiTop + scaledTitleBarH + scaledModulePanelPadTop;
        if (mx >= modPanelX && mx < modPanelX + modPanelW && localMy >= modContentTop && localMy < contentBottom) {
            if (moduleScroll != null) { moduleScroll.addScroll(-delta * SCROLL_DELTA); return true; }
        }

        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c instanceof Slider && ((Slider) c).mouseScrolled(mx, adjMy, delta)) return true;
            if (c instanceof Selector && ((Selector) c).mouseScrolled(mx, adjMy, delta)) return true;
        }

        int setPanelX = guiLeft + categoryPanelWidth + modulePanelWidth, setPanelW = settingsPanelWidth;
        int setContentTop = guiTop + scaledTitleBarH + OceanTheme.SETTINGS_HEADER_H;
        if (mx >= setPanelX && mx < setPanelX + setPanelW && localMy >= setContentTop && localMy < contentBottom) {
            if (settingsScroll != null) { settingsScroll.addScroll(-delta * SCROLL_DELTA); return true; }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
        } catch (Throwable t) {
            if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().error("[TopZurdo] TopZurdoMenuScreen.mouseScrolled", t);
            return false;
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c instanceof TextInput && ((TextInput) c).isFocused()) {
                if (((TextInput) c).charTyped(chr, modifiers)) return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    private int focusedSettingIndex = -1;

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int focusedIdx = findFocusedSettingIndex();
        if (focusedIdx >= 0) {
            UIComponent c = settingRows.get(focusedIdx).getControl();
            if (c instanceof TextInput && ((TextInput) c).isFocused()) {
                if (((TextInput) c).keyPressed(keyCode, scanCode, modifiers)) return true;
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) { clearFocus(); return true; }
                if (keyCode == GLFW.GLFW_KEY_TAB) {
                    int dir = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1;
                    moveFocus(dir);
                    return true;
                }
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_TAB && !settingRows.isEmpty()) {
            int dir = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1;
            moveFocus(dir);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (focusedSettingIndex >= 0 && focusedSettingIndex < settingRows.size()) {
                UIComponent c = settingRows.get(focusedSettingIndex).getControl();
                if (c instanceof Toggle) {
                    ((Toggle) c).onMouseClick();
                    return true;
                }
                if (c instanceof Selector) {
                    ((Selector) c).toggleExpanded();
                    return true;
                }
            }
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int findFocusedSettingIndex() {
        for (int i = 0; i < settingRows.size(); i++) {
            UIComponent c = settingRows.get(i).getControl();
            if (c instanceof TextInput && ((TextInput) c).isFocused()) return i;
        }
        return -1;
    }

    private void moveFocus(int direction) {
        int n = settingRows.size();
        if (n == 0) return;
        int idx = focusedSettingIndex < 0 ? (direction > 0 ? 0 : n - 1) : (focusedSettingIndex + direction + n) % n;
        focusSettingRow(idx);
    }

    private void focusSettingRow(int index) {
        clearFocus();
        focusedSettingIndex = index;
        UIComponent c = settingRows.get(index).getControl();
        if (c instanceof TextInput) ((TextInput) c).setFocused(true);
    }

    private void clearFocus() {
        focusedSettingIndex = -1;
        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c instanceof TextInput) ((TextInput) c).setFocused(false);
        }
    }

    @Override
    public void removed() {
        if (handCursor != 0) {
            GLFW.glfwDestroyCursor(handCursor);
            handCursor = 0;
        }
        super.removed();
    }

    @Override
    public void onClose() {
        saveUIState();
        ModConfig config = TopZurdoMod.getConfig();
        if (config != null) config.saveImmediate();
        if (client != null) client.openScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}