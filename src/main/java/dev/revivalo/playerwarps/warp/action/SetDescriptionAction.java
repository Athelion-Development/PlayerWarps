package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class SetDescriptionAction implements WarpAction<String> {
    private static final int MINIMUM_LENGTH = 5;

    @Override
    public boolean execute(Player player, Warp warp, String text) {
        int textLength = TextUtil.removeColors(text).length();
        int limit = Config.SET_DESCRIPTION_CHARACTERS_LIMIT.asInteger();

        if (textLength < MINIMUM_LENGTH || textLength > limit) {
            player.sendMessage(Lang.TEXT_SIZE_ERROR.asColoredString()
                    .replace("%min%", String.valueOf(MINIMUM_LENGTH))
                    .replace("%limit%", String.valueOf(limit)));
            return false;
        }

        warp.setDescription(text);
        player.sendMessage(Lang.DESCRIPTION_CHANGED.asReplacedString(player, new HashMap<String, String>() {{
            put("%warp%", warp.getName());
        }}));

        return true;
    }

    @Override
    public boolean hasInput() {
        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.SET_DESCRIPTION;
    }

    @Override
    public Lang getMessage() {
        return Lang.SET_DESCRIPTION_MSG;
    }

    @Override
    public int getFee() {
        return Config.SET_DESCRIPTION_FEE.asInteger();
    }
}
