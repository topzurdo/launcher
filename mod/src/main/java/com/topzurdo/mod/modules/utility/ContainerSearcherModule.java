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
}
