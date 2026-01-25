package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class BlockPlayerAction implements WarpAction<String> {
    @Override
    public boolean execute(Player player, Warp warp, String text) {
        int textLength = TextUtil.removeColors(text).length();
        if (textLength < 3 || textLength > 32) {
            player.sendMessage(Lang.TEXT_SIZE_ERROR.asColoredString());
            return false;
        }

        warp.setDisplayName(text);

        player.sendMessage(Lang.DISPLAY_NAME_CHANGED.asReplacedString(player, new HashMap<String, String>() {{
            put("%warp%", warp.getName());
            put("%displayName%", warp.getDisplayName());
        }}));

        return true;
    }

    @Override
    public boolean hasInput() {
        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.CHANGE_DISPLAY_NAME;
    }

    @Override
    public Lang getMessage() {
        return Lang.WRITE_NEW_DISPLAY_NAME;
    }

    @Override
    public int getFee() {
        return Config.SET_DISPLAY_NAME_FEE.asInteger();
    }
}
