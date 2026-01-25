package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.page.CategoriesMenu;
import dev.revivalo.playerwarps.menu.page.WarpsMenu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

public class ChangeDisplayNameAction implements WarpAction<String> {

    @Override
    public boolean execute(Player player, Warp warp, String playerToBlockName) {
        if (Config.ENABLE_CATEGORIES.asBoolean()) {
            new CategoriesMenu().open(player);
        } else {
            new WarpsMenu.DefaultWarpsMenu().setPage(1).open(player, "all", PlayerWarpsPlugin.getWarpHandler().getSortingManager().getDefaultSortType());
        }

        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.BLOCK_PLAYER;
    }

    @Override
    public Lang getMessage() {
        return Lang.BLOCKED_PLAYER_INPUT;
    }
}
