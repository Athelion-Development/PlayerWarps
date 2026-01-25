package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.PlayerUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveWarpAction implements WarpAction<Void> {
    @Override
    public boolean execute(Player player, Warp warp, Void data) {
        if (!warp.canManage(player)) {
            player.sendMessage(Lang.NOT_OWNING.asColoredString());
            return false;
        }

        PlayerWarpsPlugin.getWarpHandler().removeWarp(warp);
        HookRegister.getDynmapHook().removeMarker(warp);
        HookRegister.getBlueMapHook().removeMarker(warp);
        if (HookRegister.isHookEnabled(HookRegister.getVaultHook())) {
            final OfflinePlayer offlinePlayer = PlayerUtil.getOfflinePlayer(warp.getOwner());
            HookRegister.getVaultHook().getApi().depositPlayer(offlinePlayer, Config.DELETE_WARP_REFUND.asInteger());
            player.sendMessage(Lang.WARP_REMOVED_WITH_REFUND.asColoredString().replace("%warp%", warp.getName()).replace("%refund%", Config.DELETE_WARP_REFUND.asString()));
        } else player.sendMessage(Lang.WARP_REMOVED.asColoredString().replace("%warp%", warp.getName()));

        return true;
    }

    public boolean execute(CommandSender sender, Warp warp) {
        PlayerWarpsPlugin.getWarpHandler().removeWarp(warp);
        HookRegister.getDynmapHook().removeMarker(warp);
        sender.sendMessage(Lang.WARP_REMOVED.asColoredString().replace("%warp%", warp.getName()));

        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.REMOVE_WARP;
    }
}
