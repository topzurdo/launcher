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

    private int footerLogX, footerLogY, footerLogW, footerLogH;
    private int clothConfigX, clothConfigY, clothConfigW, clothConfigH;
    private final Screen parent;
    private boolean moduleScrollbarDragging;
    private boolean settingsScrollbarDragging;

    private static final int CARD_H = 52;
    private static final int CARD_GAP = 4;
    private static final int SCROLL_DELTA = 25;
    private static final int TITLE_BAR_H = 48;
    private static final int MODULE_PANEL_PAD_TOP = 14;

    public TopZurdoMenuScreen() {
        this(null);
    }

    public TopZurdoMenuScreen(Screen parent) {
        super(new LiteralText("TopZurdo"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        try {
        moduleManager = TopZurdoMod.getModuleManager();
        guiWidth = Math.min(640, width - 40);
        guiHeight = Math.min(420, height - OceanTheme.BOTTOM_MARGIN);
        settingsPanelWidth = Math.max(200, guiWidth - categoryPanelWidth - modulePanelWidth - 14);
        guiLeft = (width - guiWidth) / 2;
        guiTop = (height - guiHeight) / 2;

        openAnimation = 1f;
        openAnimationPrev = 1f;
        categoryAnimation = 1f;
        categoryAnimationPrev = 1f;

        restoreUIState();

        createModuleCards();
        int modContentTop = guiTop + TITLE_BAR_H + MODULE_PANEL_PAD_TOP;
        int contentBottom = guiTop + guiHeight - OceanTheme.CONTENT_BOTTOM_OFFSET;
        int modVisibleH = contentBottom - modContentTop;
        int modTotalH = moduleManager.getModulesByCategory(selectedCategory).size() * (CARD_H + CARD_GAP);
        int modPanelX = guiLeft + categoryPanelWidth + 6;
        moduleScroll = new ScrollContainer(modPanelX, modContentTop, modulePanelWidth - 12, modVisibleH, modTotalH);

        int setContentTop = guiTop + TITLE_BAR_H + OceanTheme.SETTINGS_HEADER_H;
        int setVisibleH = contentBottom - setContentTop;
        settingsScroll = new ScrollContainer(guiLeft + categoryPanelWidth + modulePanelWidth + 6, setContentTop, settingsPanelWidth - 12, setVisibleH, 0);

        selectRestoredModule();
        restoreScrollOffsets();
        } catch (Throwable t) {
            if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().error("[TopZurdo] TopZurdoMenuScreen.init", t);
            throw t;
        }
    }

    private void createModuleCards() {
        moduleCards.clear();
        for (Module m : moduleManager.getAllModules()) {
            String title = GuiUtil.resolveKey(m.getName());
            String desc = GuiUtil.resolveKey(m.getDescription());
            int cardW = modulePanelWidth - 24 - OceanTheme.SCROLLBAR_WIDTH - 8;
            ModuleCard card =
                new ModuleCard(0, 0, cardW, CARD_H, title, desc, m.isEnabled(), m == selectedModule);
            moduleCards.put(m, card);
        }
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

    private void updateSettingsComponents() {
        settingRows.clear();
        settingsList.clear();
        totalSettingsHeight = 0;
        if (selectedModule == null) {
            if (settingsScroll != null) settingsScroll.setContentHeight(0);
            return;
        }

        int w = settingsPanelWidth - OceanTheme.SETTINGS_PAD_X * 2 - OceanTheme.SCROLLBAR_WIDTH - 28;
        int y = 0;
        for (Setting<?> s : selectedModule.getSettings()) {
            String name = GuiUtil.resolveKey(s.getName());
            UIComponent control = null;
            if (s.isBoolean()) {
                Toggle t = new Toggle(0, 0, w, OceanTheme.ROW_TOGGLE_H, name, (Boolean) s.getValue(),
                    v -> { ((Setting<Boolean>) s).setValue(v); s.save(selectedModule.getId()); });
                control = t;
            } else if (s.isColor()) {
                // Color picker for color settings
                int colorVal = (Integer) s.getValue();
                ColorPicker cp = new ColorPicker(0, 0, w, OceanTheme.ROW_TOGGLE_H, name, colorVal,
                    v -> { ((Setting<Integer>) s).setValue(v); s.save(selectedModule.getId()); });
                control = cp;
            } else if (s.isNumber()) {
                double min = s.getMin() != null ? s.getMin().doubleValue() : 0;
                double max = s.getMax() != null ? s.getMax().doubleValue() : 100;
                double val = ((Number) s.getValue()).doubleValue();
                double step = s.isInteger() ? 1 : 0.1;
                Slider sl = new Slider(0, 0, w, OceanTheme.ROW_SLIDER_H, name, min, max, val, step, !s.isInteger(),
                    v -> {
                        if (s.isInteger()) ((Setting<Integer>) s).setValue((int) Math.round(v));
                        else if (s.isFloat()) ((Setting<Float>) s).setValue(v.floatValue());
                        else ((Setting<Double>) s).setValue(v);
                        s.save(selectedModule.getId());
                    });
                control = sl;
            } else if (s.isOptions()) {
                Object v = s.getValue();
                String opt = (v instanceof String && s.getOptions().contains(v)) ? (String) v
                    : (s.getOptions().isEmpty() ? "" : s.getOptions().get(0));
                Selector sel = new Selector(0, 0, w, OceanTheme.ROW_SELECTOR_H, name, s.getOptions(), opt,
                    v2 -> { ((Setting<String>) s).setValue(v2); s.save(selectedModule.getId()); });
                control = sel;
            } else if (s.isString()) {
                String val = (String) s.getValue();
                if (val == null) val = "";
                TextInput ti = new TextInput(0, 0, w, OceanTheme.ROW_SLIDER_H + 14, name, val, 64,
                    v -> { ((Setting<String>) s).setValue(v); s.save(selectedModule.getId());
                        if (selectedModule instanceof com.topzurdo.mod.modules.utility.ContainerSearcherModule) {
                            ((com.topzurdo.mod.modules.utility.ContainerSearcherModule) selectedModule).setSearchQuery(v);
                        }
                    });
                control = ti;
            }
            if (control != null) {
                SettingRow row = new SettingRow(0, 0, w, control);
                settingRows.add(row);
                settingsList.add(s);
            }
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
        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c instanceof Toggle) ((Toggle) c).tick();
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        try {
        UIRenderHelper.setPartialTicks(partialTicks);

        float rawOpen = openAnimationPrev + (openAnimation - openAnimationPrev) * partialTicks;
        float eased = getEasedOpen(rawOpen);
        float rawCat = categoryAnimationPrev + (categoryAnimation - categoryAnimationPrev) * partialTicks;

        // Background with gradient
        UIRenderHelper.fillVerticalGradient(ms, 0, 0, width, height, 0xFF0A0A18, OceanTheme.BG_DEEP);

        // Subtle grid
        int gridColor = UIRenderHelper.withAlpha(OceanTheme.NEON_PURPLE, 0.02f);
        for (int gx = 0; gx < width; gx += 32) {
            UIRenderHelper.fill(ms, gx, 0, gx + 1, height, gridColor);
        }
        for (int gy = 0; gy < height; gy += 32) {
            UIRenderHelper.fill(ms, 0, gy, width, gy + 1, gridColor);
        }

        // Scanlines
        UIRenderHelper.drawScanlines(ms, 0, 0, width, height, 3, 0.025f);

        // Overlay
        int overlayAlpha = (int) ((OceanTheme.BG_OVERLAY >>> 24) * eased);
        UIRenderHelper.fill(ms, 0, 0, width, height, (Math.min(255, overlayAlpha) << 24) | (OceanTheme.BG_OVERLAY & 0x00FFFFFF));

        ms.push();
        int lmx = (int) toLogicalX(mouseX);
        int lmy = (int) toLogicalY(mouseY);

        drawOceanPanel(ms, guiLeft, guiTop, guiWidth, guiHeight, categoryPanelWidth);
        drawPanelShadow(ms, guiLeft, guiTop, guiWidth, guiHeight, 1f);
        renderTitleBar(ms);
        renderCategoryPanel(ms, lmx, lmy);

        int contentBottom = guiTop + guiHeight - OceanTheme.CONTENT_BOTTOM_OFFSET;
        drawVerticalLine(ms, guiLeft + categoryPanelWidth, guiTop + (TITLE_BAR_H - 2), guiHeight - 54);
        renderModulePanel(ms, lmx, lmy, contentBottom, eased, rawCat, partialTicks);
        drawVerticalLine(ms, guiLeft + categoryPanelWidth + modulePanelWidth, guiTop + (TITLE_BAR_H - 2), guiHeight - 54);

        renderSettingsPanel(ms, lmx, lmy, contentBottom, eased, partialTicks);
        renderFooter(ms);

        ms.pop();
        super.render(ms, mouseX, mouseY, partialTicks);
        } catch (Throwable t) {
            if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().error("[TopZurdo] TopZurdoMenuScreen.render", t);
            throw t;
        }
    }

    private void renderTitleBar(MatrixStack ms) {
        String title = "TOPZURDO";
        int tw = textRenderer.getWidth(title);
        float scale = 1.5f;
        int cx = guiLeft + guiWidth / 2;
        int ty = guiTop + 12;

        ms.push();
        ms.translate(cx, ty, 0);
        ms.scale(scale, scale, 1f);

        int glowColor = UIRenderHelper.withAlpha(OceanTheme.NEON_GOLD, 0.2f);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) {
                    textRenderer.draw(ms, title, -tw / 2f + dx, dy, glowColor);
                }
            }
        }

        float x = -tw / 2f;
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            float t = (float) i / Math.max(1, title.length() - 1);
            int color = UIRenderHelper.lerpColor(OceanTheme.NEON_GOLD, OceanTheme.NEON_CYAN, t);
            textRenderer.draw(ms, String.valueOf(c), x, 0, color);
            x += textRenderer.getWidth(String.valueOf(c));
        }
        ms.pop();

        // Decorative accents
        int leftEdge = (int) (cx - tw * scale / 2);
        UIRenderHelper.fill(ms, leftEdge - 12, ty + 4, leftEdge - 4, ty + 6, OceanTheme.NEON_GOLD);
        UIRenderHelper.fill(ms, cx + (int)(tw * scale / 2) + 4, ty + 4, cx + (int)(tw * scale / 2) + 12, ty + 6, OceanTheme.NEON_GOLD);

        String sub = I18n.translate("topzurdo.gui.subtitle");
        textRenderer.drawWithShadow(ms, sub, guiLeft + (guiWidth - textRenderer.getWidth(sub)) / 2f, guiTop + 28, OceanTheme.TEXT_DIM);

        UIRenderHelper.fill(ms, guiLeft + 24, guiTop + 42, guiLeft + guiWidth - 24, guiTop + 43, UIRenderHelper.withAlpha(OceanTheme.NEON_PURPLE, 0.4f));
    }

    private void renderCategoryPanel(MatrixStack ms, int lmx, int lmy) {
        int x = guiLeft + OceanTheme.SPACE_8;
        int y = guiTop + TITLE_BAR_H;
        int catBtnW = categoryPanelWidth - OceanTheme.SPACE_12;
        int catBtnH = 36;
        int catStep = 40;
        for (Module.Category cat : Module.Category.values()) {
            String label = elegantCategoryName(cat);
            int cnt = moduleManager.getModulesByCategory(cat).size();
            int on = (int) moduleManager.getModulesByCategory(cat).stream().filter(Module::isEnabled).count();
            String icon = iconForCategory(cat);
            CategoryTab tab = new CategoryTab(x, y, catBtnW, catBtnH, label, icon, on, cnt, cat == selectedCategory);
            tab.render(ms, lmx, lmy);
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
        int panelY = guiTop + TITLE_BAR_H;
        int modContentTop = panelY + MODULE_PANEL_PAD_TOP;
        int modVisibleH = contentBottom - modContentTop;

        if (moduleScroll != null) {
            moduleScroll.setContentHeight(modules.size() * (CARD_H + CARD_GAP));
        }

        textRenderer.drawWithShadow(ms, I18n.translate("topzurdo.gui.modules"), panelX + 8, panelY, OceanTheme.ACCENT_SECONDARY);
        applyScissor(panelX, modContentTop, modulePanelWidth - 12, modVisibleH);
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
                if (y + CARD_H >= panelY + 12 && y < contentBottom - 2)
                    card.render(ms, lmx, lmy);
            }
            y += CARD_H + CARD_GAP;
        }
        ms.pop();
        if (moduleScroll != null && moduleScroll.getMaxScroll() > 0)
            moduleScroll.renderScrollbar(ms);
        RenderSystem.disableScissor();
    }

    private void renderSettingsPanel(MatrixStack ms, int lmx, int lmy, int contentBottom, float eased, float partialTicks) {
        int panelX = guiLeft + categoryPanelWidth + modulePanelWidth + 6;
        int panelW = settingsPanelWidth;
        int baseY = guiTop + TITLE_BAR_H;
        int setContentTop = baseY + OceanTheme.SETTINGS_HEADER_H;
        int setContentBottom = contentBottom;
        int visibleH = setContentBottom - setContentTop;

        settingsScroll.setContentHeight(totalSettingsHeight);

        if (selectedModule == null) {
            DrawableHelper.drawCenteredText(ms, textRenderer, I18n.translate("topzurdo.gui.select_module"), panelX + panelW / 2, baseY + 60, OceanTheme.TEXT_DIM);
            return;
        }

        String name = GuiUtil.resolveKey(selectedModule.getName());
        String nameUp = GuiUtil.truncate(textRenderer, name.toUpperCase(), panelW - 24);
        textRenderer.drawWithShadow(ms, nameUp, panelX + (panelW - textRenderer.getWidth(nameUp)) / 2f, baseY, OceanTheme.ACCENT);
        String status = selectedModule.isEnabled() ? I18n.translate("topzurdo.module.enabled") : I18n.translate("topzurdo.module.disabled");
        textRenderer.drawWithShadow(ms, status, panelX + (panelW - textRenderer.getWidth(status)) / 2f, baseY + 10, selectedModule.isEnabled() ? OceanTheme.SUCCESS : OceanTheme.TEXT_DIM);
        drawHorizontalLine(ms, panelX + 16, baseY + 22, panelW - 32);
        String desc = GuiUtil.resolveKey(selectedModule.getDescription());
        DrawableHelper.drawCenteredText(ms, textRenderer, GuiUtil.truncate(textRenderer, desc, panelW - 24), panelX + panelW / 2, baseY + 34, OceanTheme.TEXT_DIM);

        if (settingRows.isEmpty()) {
            DrawableHelper.drawCenteredText(ms, textRenderer, I18n.translate("topzurdo.gui.no_settings"), panelX + panelW / 2, baseY + 90, OceanTheme.TEXT_MUTED);
        } else {
            int off = settingsScroll.getScrollOffset();
            int adjMy = lmy + off;
            int settingsX = guiLeft + categoryPanelWidth + modulePanelWidth + OceanTheme.SETTINGS_PAD_X;
            int rowY = setContentTop;
            String hoveredDesc = null;
            applyScissor(panelX, setContentTop, settingsPanelWidth - 12, visibleH);
            ms.push();
            ms.translate(0, -off, 0);
            int idx = 0;
            for (SettingRow row : settingRows) {
                UIComponent c = row.getControl();
                c.setPartialTicks(partialTicks);
                row.setPosition(settingsX, rowY);
                if (idx < settingsList.size()) {
                    Setting<?> s = settingsList.get(idx);
                    if (c instanceof Slider && s.isNumber())
                        ((Slider) c).setValue(((Number) s.getValue()).doubleValue());
                    else if (c instanceof Toggle && s.isBoolean())
                        ((Toggle) c).setValue((Boolean) s.getValue());
                    else if (c instanceof ColorPicker && s.isColor())
                        ((ColorPicker) c).setValue((Integer) s.getValue());
                    else if (c instanceof TextInput && s.isString()) {
                        Object v = s.getValue();
                        ((TextInput) c).setText(v != null ? v.toString() : "");
                    } else if (c instanceof Selector && s.isOptions()) {
                        Object v = s.getValue();
                        String o = (v instanceof String && s.getOptions().contains(v)) ? (String) v : (s.getOptions().isEmpty() ? "" : s.getOptions().get(0));
                        ((Selector) c).setValue(o);
                    }
                }
                row.render(ms, lmx, adjMy);
                if (idx < settingsList.size() && isSettingRowHovered(row, lmx, adjMy))
                    hoveredDesc = GuiUtil.resolveKey(settingsList.get(idx).getDescription());
                rowY += row.getHeight();
                idx++;
            }
            ms.pop();
            if (settingsScroll.getMaxScroll() > 0)
                settingsScroll.renderScrollbar(ms);
            RenderSystem.disableScissor();
            if (hoveredDesc != null) {
                hoveredDesc = GuiUtil.truncate(textRenderer, hoveredDesc, panelW - 24);
                int hoverY = guiTop + guiHeight - OceanTheme.FOOTER_H - OceanTheme.HOVER_DESC_H - 2;
                textRenderer.drawWithShadow(ms, hoveredDesc, panelX + 8, hoverY, OceanTheme.TEXT_MUTED);
            }
        }
    }

    private boolean isSettingRowHovered(SettingRow row, int mx, int my) {
        UIComponent c = row.getControl();
        if (c instanceof Toggle) return ((Toggle) c).isMouseOver(mx, my);
        if (c instanceof Slider) return ((Slider) c).isMouseOver(mx, my);
        if (c instanceof Selector) return ((Selector) c).isMouseOver(mx, my);
        if (c instanceof TextInput) return ((TextInput) c).isMouseOver(mx, my);
        if (c instanceof ColorPicker) return ((ColorPicker) c).isMouseOver(mx, my);
        return false;
    }

    private void renderFooter(MatrixStack ms) {
        boolean logOn = TopZurdoMod.getConfig() != null && TopZurdoMod.getConfig().isDebugLogging();
        String logLabel = logOn ? I18n.translate("topzurdo.gui.debug_logging.on") : I18n.translate("topzurdo.gui.debug_logging.off");
        String clothLabel = I18n.translate("topzurdo.gui.cloth_config");
        footerLogW = textRenderer.getWidth(logLabel) + 14;
        footerLogH = 18;
        clothConfigW = textRenderer.getWidth(clothLabel) + 14;
        clothConfigH = 18;
        clothConfigX = guiLeft + guiWidth - footerLogW - clothConfigW - 28;
        clothConfigY = guiTop + guiHeight - 28;
        footerLogX = guiLeft + guiWidth - footerLogW - 20;
        footerLogY = guiTop + guiHeight - 28;

        UIRenderHelper.fill(ms, clothConfigX, clothConfigY, clothConfigX + clothConfigW, clothConfigY + clothConfigH, OceanTheme.BG_INNER);
        UIRenderHelper.fill(ms, clothConfigX, clothConfigY, clothConfigX + clothConfigW, clothConfigY + OceanTheme.BORDER_WIDTH, OceanTheme.BORDER_SUBTLE);
        UIRenderHelper.fill(ms, clothConfigX, clothConfigY + clothConfigH - OceanTheme.BORDER_WIDTH, clothConfigX + clothConfigW, clothConfigY + clothConfigH, OceanTheme.BORDER_SUBTLE);
        UIRenderHelper.fill(ms, clothConfigX, clothConfigY, clothConfigX + OceanTheme.BORDER_WIDTH, clothConfigY + clothConfigH, OceanTheme.BORDER_SUBTLE);
        UIRenderHelper.fill(ms, clothConfigX + clothConfigW - OceanTheme.BORDER_WIDTH, clothConfigY, clothConfigX + clothConfigW, clothConfigY + clothConfigH, OceanTheme.BORDER_SUBTLE);
        textRenderer.drawWithShadow(ms, clothLabel, clothConfigX + 7, clothConfigY + 5, OceanTheme.TEXT_MUTED);

        int hintMaxW = guiWidth - footerLogW - clothConfigW - 36;
        List<String> lines = GuiUtil.wrapHint(textRenderer, I18n.translate("topzurdo.gui.footer"), hintMaxW);
        int fy = lines.size() > 1 ? guiTop + guiHeight - 34 : guiTop + guiHeight - 28;
        for (int i = 0; i < lines.size(); i++)
            textRenderer.drawWithShadow(ms, lines.get(i), guiLeft + 10, fy + i * 10, OceanTheme.TEXT_MUTED);

        UIRenderHelper.fill(ms, footerLogX, footerLogY, footerLogX + footerLogW, footerLogY + footerLogH, OceanTheme.BG_INNER);
        int brd = logOn ? (OceanTheme.SUCCESS & 0xCCFFFFFF) : OceanTheme.BORDER_SUBTLE;
        UIRenderHelper.fill(ms, footerLogX, footerLogY, footerLogX + footerLogW, footerLogY + OceanTheme.BORDER_WIDTH, brd);
        UIRenderHelper.fill(ms, footerLogX, footerLogY + footerLogH - OceanTheme.BORDER_WIDTH, footerLogX + footerLogW, footerLogY + footerLogH, brd);
        UIRenderHelper.fill(ms, footerLogX, footerLogY, footerLogX + OceanTheme.BORDER_WIDTH, footerLogY + footerLogH, brd);
        UIRenderHelper.fill(ms, footerLogX + footerLogW - OceanTheme.BORDER_WIDTH, footerLogY, footerLogX + footerLogW, footerLogY + footerLogH, brd);
        textRenderer.drawWithShadow(ms, logLabel, footerLogX + 7, footerLogY + 5, logOn ? OceanTheme.SUCCESS : OceanTheme.TEXT_MUTED);
    }

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

        int catY = guiTop + TITLE_BAR_H, catBtnH = 36, catStep = 40;
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

        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c instanceof Toggle) {
                if (((Toggle) c).isMouseOver(mx, adjMy)) { ((Toggle) c).onMouseClick(); return true; }
            } else if (c instanceof Slider) {
                if (((Slider) c).mouseClicked(lx, adjMy, button)) return true;
            } else if (c instanceof ColorPicker) {
                if (((ColorPicker) c).mouseClicked(lx, adjMy, button)) return true;
            } else if (c instanceof Selector) {
                if (((Selector) c).mouseClicked(lx, adjMy, button)) return true;
            } else if (c instanceof TextInput) {
                if (((TextInput) c).isMouseOver(mx, adjMy)) {
                    for (SettingRow r : settingRows) {
                        UIComponent oc = r.getControl();
                        if (oc instanceof TextInput) ((TextInput) oc).setFocused(false);
                    }
                    ((TextInput) c).setFocused(true);
                    return true;
                }
            }
        }

        if (button == 0 && settingsScroll != null && settingsScroll.getMaxScroll() > 0 && settingsScroll.isOverScrollbar(mx, localMy)) {
            settingsScroll.setOffsetFromScrollbarMouseY(localMy);
            settingsScrollbarDragging = true;
            return true;
        }

        if (button == 0 && mx >= clothConfigX && mx < clothConfigX + clothConfigW && localMy >= clothConfigY && localMy < clothConfigY + clothConfigH) {
            try {
                Screen clothScreen = com.topzurdo.mod.config.TopZurdoClothConfigScreen.create(this);
                if (clothScreen != null && client != null) client.openScreen(clothScreen);
            } catch (Throwable t) {
                if (TopZurdoMod.getInstance() != null) TopZurdoMod.getLogger().warn("[TopZurdo] Cloth Config open failed: {}", t.getMessage());
            }
            return true;
        }
        if (button == 0 && mx >= footerLogX && mx < footerLogX + footerLogW && localMy >= footerLogY && localMy < footerLogY + footerLogH) {
            ModConfig cfg = TopZurdoMod.getConfig();
            if (cfg != null) {
                cfg.setDebugLogging(!cfg.isDebugLogging());
                TopZurdoMod.logEvent("Логирование: " + (cfg.isDebugLogging() ? "вкл" : "выкл"));
            }
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
        int contentBottom = guiTop + guiHeight - OceanTheme.CONTENT_BOTTOM_OFFSET;

        int modPanelX = guiLeft + categoryPanelWidth, modPanelW = modulePanelWidth;
        int modContentTop = guiTop + TITLE_BAR_H + MODULE_PANEL_PAD_TOP;
        if (mx >= modPanelX && mx < modPanelX + modPanelW && localMy >= modContentTop && localMy < contentBottom) {
            if (moduleScroll != null) { moduleScroll.addScroll(delta * SCROLL_DELTA); return true; }
        }

        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c instanceof Slider && ((Slider) c).mouseScrolled(mx, adjMy, delta)) return true;
            if (c instanceof Selector && ((Selector) c).mouseScrolled(mx, adjMy, delta)) return true;
        }

        int setPanelX = guiLeft + categoryPanelWidth + modulePanelWidth, setPanelW = settingsPanelWidth;
        int setContentTop = guiTop + TITLE_BAR_H + OceanTheme.SETTINGS_HEADER_H;
        if (mx >= setPanelX && mx < setPanelX + setPanelW && localMy >= setContentTop && localMy < contentBottom) {
            if (settingsScroll != null) { settingsScroll.addScroll(delta * SCROLL_DELTA); return true; }
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (SettingRow row : settingRows) {
            UIComponent c = row.getControl();
            if (c instanceof TextInput && ((TextInput) c).isFocused()) {
                if (((TextInput) c).keyPressed(keyCode, scanCode, modifiers)) return true;
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) { ((TextInput) c).setFocused(false); return true; }
            }
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        saveUIState();
        if (client != null) client.openScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
