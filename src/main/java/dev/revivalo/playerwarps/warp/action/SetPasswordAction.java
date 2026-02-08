package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.page.ManageMenu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpState;
import org.bukkit.entity.Player;

public class SetPasswordAction implements WarpAction<String>, Inputable {
    @Override
    public boolean execute(Player player, Warp warp, String input) {
        if (input.isEmpty()) {
            player.sendMessage(Lang.INVALID_INPUT.asColoredString());
            return false;
        }

        if (input.length() < 3 || input.length() > 15) {
            player.sendMessage(Lang.PASSWORD_TOO_SHORT.asColoredString());
            return false;
        }

        warp.setPassword(input);
        player.sendMessage(Lang.PASSWORD_CHANGED.asColoredString());

        warp.setStatus(WarpState.PASSWORD_PROTECTED);

        PlayerWarpsPlugin.get().runDelayed(() -> new ManageMenu(warp).openFor(player), 3);

        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.SET_STATUS;
    }

    @Override
    public Lang getInputText() {
        return Lang.ENTER_PASSWORD;
    }
}
