package dev.revivalo.playerwarps.user;

import dev.revivalo.playerwarps.menu.page.Menu;
import dev.revivalo.playerwarps.menu.sort.Sortable;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.WarpAction;

public enum DataSelectorType {
    ACTUAL_PAGE(Integer.class),
    ACTUAL_MENU(Menu.class),
    PREVIOUS_PAGE_TEST(Menu.class),
    CURRENT_WARP_ACTION(WarpAction.class),
    SELECTED_WARP(Warp.class),
    SELECTED_CATEGORY(String.class),
    SELECTED_SORT(Sortable.class);

    private final Class<?> dataType;

    DataSelectorType(Class<?> dataType) {
        this.dataType = dataType;
    }

    public Class<?> getDataType() {
        return dataType;
    }
}

