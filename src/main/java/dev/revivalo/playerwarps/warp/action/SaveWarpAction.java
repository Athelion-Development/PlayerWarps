package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.playerconfig.PlayerConfig;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.entity.Player;

import java.util.List;

public class SaveWarpAction implements WarpAction<Void> {
    @Override
    public boolean execute(Player player, Warp warp, Void data) {
        final PlayerConfig playerData = PlayerConfig.getConfig(player);
        final List<String> favorites = playerData.getStringList("favorites");
        final String warpString = warp.toString();

        if (favorites.remove(warpString)) {
            player.sendMessage(Lang.REMOVE_FAVORITE_WARP.asColoredString().replace("%warp%", warp.getName()));
        } else {
            favorites.add(warpString);
            player.sendMessage(Lang.FAVORITE_WARP_ADDED.asColoredString().replace("%warp%", warp.getName()));
        }

        playerData.set("favorites", favorites);
        playerData.save();

        return true;
    }


    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.FAVORITE_WARP;
    }

    @Override
    public boolean isPublicAction() {
        return true;
    }
}
