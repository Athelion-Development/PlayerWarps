package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

public class TransferOwnershipAction implements WarpAction<String> {
    @Override
    public boolean execute(Player player, Warp warp, String newOwnerName) {
        final Player newOwner = Bukkit.getPlayerExact(newOwnerName);

        if (newOwner != null && (newOwner.isOnline() || newOwner.hasPlayedBefore())) {
            if (Objects.equals(warp.getOwner(), player.getUniqueId())) {
                player.sendMessage(Lang.ALREADY_OWNING.asColoredString());
                return false;
            }

            if (!PlayerWarpsPlugin.getWarpHandler().canHaveWarp(newOwner)) {
                player.sendMessage(Lang.LIMIT_REACHED_OTHER.asColoredString().replace("%player%", newOwner.getName()));
                return false;
            }

            warp.setOwner(newOwner.getUniqueId());
            warp.setOwnerName(newOwner.getName());
            player.sendMessage(Lang.OWNERSHIP_TRANSFER_SUCCESSFUL.asColoredString()
                    .replace("%player%", newOwner.getName())
                    .replace("%warp%", warp.getName()));
            if (newOwner.isOnline()) {
                newOwner.sendMessage(Lang.OWNERSHIP_TRANSFER_INFO.asColoredString()
                        .replace("%player%", player.getName())
                        .replace("%warp%", warp.getName()));
            }

        } else {
            player.sendMessage(Lang.UNAVAILABLE_PLAYER.asColoredString());
            return false;
        }

        return true;
    }

    @Override
    public boolean hasInput() {
        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.TRANSFER_WARP;
    }

    @Override
    public Lang getMessage() {
        return Lang.OWNER_CHANGE_MSG;
    }

    @Override
    public int getFee() {
        return Config.TRANSFER_OWNERSHIP_FEE.asInteger();
    }
}
