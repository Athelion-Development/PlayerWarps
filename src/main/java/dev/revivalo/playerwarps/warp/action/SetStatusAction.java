package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.BlueMapHook;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpStatus;
import org.bukkit.entity.Player;

public class SetStatusAction implements WarpAction<WarpStatus> {
    @Override
    public boolean execute(Player player, Warp warp, WarpStatus status) {
        WarpStatus previousStatus = warp.getStatus();
        if (previousStatus == status) return true;

        if (previousStatus != WarpStatus.OPENED) {
            HookRegister.ifEnabled(BlueMapHook.class, hook -> hook.setMarker(warp));
        } else {
            HookRegister.ifEnabled(BlueMapHook.class, hook -> hook.removeMarker(warp));
        }

        warp.setStatus(status);
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
