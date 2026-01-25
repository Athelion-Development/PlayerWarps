package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

public class RenameAction implements WarpAction<String> {
    @Override
    public boolean execute(Player player, Warp warp, String newName) {
        int limit = Config.WARP_NAME_MAX_LENGTH.asInteger();
        if (newName.length() > limit) {
            player.sendMessage(Lang.WARP_NAME_IS_ABOVE_LETTERS_LIMIT.asColoredString().replace("%limit%", String.valueOf(limit)));
            return false;
        }

        if (newName.contains(" ")) {
            player.sendMessage(Lang.NAME_CANT_CONTAINS_SPACE.asColoredString());
            return false;
        }

        warp.setName(newName);
        player.sendMessage(Lang.WARP_NAME_CHANGED.asColoredString().replace("%oldName%", warp.getName()).replace("%newName%", newName));

        return true;
    }

    @Override
    public boolean hasInput() {
        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.RENAME_WARP;
    }

    @Override
    public Lang getMessage() {
        return Lang.RENAME_WRITE_MSG;
    }

    @Override
    public int getFee() {
        return Config.RENAME_WARP_FEE.asInteger();
    }
}
