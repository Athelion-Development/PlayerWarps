package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.input.PlayerInput;
import dev.revivalo.playerwarps.menu.page.ConfirmationMenu;
import dev.revivalo.playerwarps.menu.page.Menu;
import dev.revivalo.playerwarps.util.EconomyUtil;
import dev.revivalo.playerwarps.util.NumberUtil;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

public class PreTeleportToWarpAction implements WarpAction<String> {
    private Menu menuToOpen = null;

    @Override
    public boolean execute(Player player, Warp warp, String data) {
        final Category category = warp.getCategory();
        if (!category.hasPermission(player)) {
            player.sendMessage(Lang.INSUFFICIENT_PERMISSIONS.asColoredString().replace("%permission%", category.getPermission()));
            return false;
        }

        if (requiresAdmission(player, warp) && !EconomyUtil.has(player, warp.getAdmission())) {
            player.sendMessage(Lang.INSUFFICIENT_BALANCE_TO_TELEPORT.asColoredString()
                    .replace("%warp%", warp.getName())
                    .replace("%price%", NumberUtil.formatNumber(warp.getAdmission())));
            return false;
        }

        if (warp.isPasswordProtected() && !warp.canManage(player)) {
            PlayerInput.request(player, Lang.ENTER_PASSWORD, menuToOpen).thenAccept(input -> {
                if (input.length() < 3 || input.length() > 15) {
                    return;
                }

                if (warp.isPasswordProtected() && !warp.validatePassword(input)) {
                    player.sendMessage(Lang.ENTERED_WRONG_PASSWORD.asColoredString());
                    return;
                }

                PlayerWarpsPlugin.get().runDelayed(() -> teleport(player, warp), 2);
            });

        } else {
            teleport(player, warp);
        }
        return false;
    }

    /**
     * Paid warps are confirmed first. The confirmed admission is handed to the teleport, which
     * never charges more than that - the owner may change the price while the menu is open.
     */
    private void teleport(Player player, Warp warp) {
        if (requiresAdmission(player, warp)) {
            final ConfirmationMenu<String> confirmationMenu = new ConfirmationMenu<>(warp);
            confirmationMenu.setMenuToOpen(menuToOpen);
            confirmationMenu.open(player, new TeleportToWarpAction(warp.getAdmission()));
        } else {
            new TeleportToWarpAction().proceed(player, warp);
        }
    }

    private boolean requiresAdmission(Player player, Warp warp) {
        return warp.hasAdmission() && EconomyUtil.isEnabled() && !warp.canManage(player);
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.USE;
    }

    @Override
    public boolean isPublicAction() {
        return true;
    }

    public PreTeleportToWarpAction setMenuToOpen(Menu menu) {
        this.menuToOpen = menu;
        return this;
    }
}
