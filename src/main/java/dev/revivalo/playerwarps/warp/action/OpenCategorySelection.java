package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.menu.page.WarpsMenu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

public class OpenCategorySelection implements WarpAction<String> {
    @Override
    public boolean execute(Player player, Warp warp, String categoryName) {
        if (!CategoryManager.isCategory(categoryName)) {
            return false;
        }

        Category category = CategoryManager.getCategoryFromName(categoryName);

        new WarpsMenu.DefaultWarpsMenu().setPage(1).open(player, categoryName, PlayerWarpsPlugin.getWarpHandler().getSortingManager().getDefaultSortType());

        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return null;
    }
}
