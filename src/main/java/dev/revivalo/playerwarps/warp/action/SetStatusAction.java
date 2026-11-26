package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpStatus;
import org.bukkit.entity.Player;

public class SetStatusAction implements WarpAction<WarpStatus> {
    @Override
    public boolean execute(Player player, Warp warp, WarpStatus status) {
        WarpStatus previousStatus = warp.getStatus();
        if (previousStatus == status) return true;

        warp.setStatus(status);

        // Has to run after the status change - the markers follow the warp's accessibility.
        getWarpManager().showOnMaps(warp);
        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.SET_STATUS;
    }

    @Override
    public int getFee() {
        return Config.SET_STATUS_FEE.asInteger();
    }
}
