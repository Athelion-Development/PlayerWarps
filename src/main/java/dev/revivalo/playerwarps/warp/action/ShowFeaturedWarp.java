package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

public class ShowFeaturedWarp implements WarpAction<Void> {
    @Override
    public boolean execute(Player player, Warp warp, Void data) {
        return false;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return null;
    }
}
