package com.topzurdo.mod.modules.utility;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

/**
 * Container Searcher Module - highlights items in containers
 */
public class ContainerSearcherModule extends Module {

    private Setting<String> searchTerm;
    private Setting<Integer> highlightColor;
    private Setting<Boolean> caseSensitive;

    public ContainerSearcherModule() {
        super("container_searcher", "Container Searcher", "Поиск предметов в контейнерах", Category.UTILITY);

        searchTerm = addSetting(Setting.ofString("search_term", "Поиск", "Текст для поиска", ""));
        highlightColor = addSetting(Setting.ofInt("highlight_color", "Цвет подсветки", "Цвет выделения найденных", 0x55FF55, 0, 0xFFFFFF));
        caseSensitive = addSetting(Setting.ofBoolean("case_sensitive", "Регистр", "Учитывать регистр", false));
    }

    public String getSearchTerm() { return searchTerm.getValue(); }
    public int getHighlightColor() { return highlightColor.getValue(); }
    public boolean isCaseSensitive() { return caseSensitive.getValue(); }

    public boolean matches(String itemName) {
        String search = searchTerm.getValue();
        if (search == null || search.isEmpty()) return false;
        if (caseSensitive.getValue()) {
            return itemName.contains(search);
        }
        return itemName.toLowerCase().contains(search.toLowerCase());
    }

    public void setSearchQuery(String query) {
        searchTerm.setValue(query);
    }

    public void drawHighlights(net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen, net.minecraft.client.util.math.MatrixStack matrices) {
        if (!isEnabled()) return;
        String search = searchTerm.getValue();
        if (search == null || search.isEmpty()) return;

        for (net.minecraft.screen.slot.Slot slot : screen.getScreenHandler().slots) {
            net.minecraft.item.ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            String name = stack.getName().getString();
            if (matches(name)) {
                int x = slot.x;
                int y = slot.y;
                int color = 0x8000FF00 | (highlightColor.getValue() & 0xFFFFFF);
                net.minecraft.client.gui.DrawableHelper.fill(matrices, x, y, x + 16, y + 16, color);
            }
        }
    }

    public void onScreenClosed() {
        // Nothing to clean up
    }
}
