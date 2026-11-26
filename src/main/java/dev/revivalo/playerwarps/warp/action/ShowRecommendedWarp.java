package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

/**
 * Marks a menu slot that shows a currently popular warp. The slot is filled by the menu, so
 * the action itself only has to identify the slot.
 */
public class ShowRecommendedWarp implements WarpAction<Void> {
    @Override
    public boolean execute(Player player, Warp warp, Void data) {
        return false;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return null;
    }
}
