package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.page.ConfirmationMenu;
import dev.revivalo.playerwarps.menu.page.Menu;
import dev.revivalo.playerwarps.menu.page.WarpsMenu;
import dev.revivalo.playerwarps.util.EconomyUtil;
import dev.revivalo.playerwarps.util.NumberUtil;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface WarpAction<T> {
    default void proceed(Player player, Warp warp) {
        proceed(player, warp, null);
    }

    default void proceed(Player player, Warp warp, T data) {
        proceed(player, warp, data, null);
    }

    default void proceed(Player player, Warp warp, T data, @Nullable Menu menu) {
        proceed(player, warp, data, menu, 1);
    }

    default void proceed(Player player, Warp warp, T data, @Nullable Menu menuToOpen, int page) {
        proceed(player, warp, data, menuToOpen, page, false);
    }

    default void proceed(Player player, Warp warp, T data, @Nullable Menu menuToOpen, int page, boolean isConfirmed) {
        // Checked again after the confirmation menu: the warp may have got another owner, or
        // the player lost the permission, while the menu was open.
        if (!isAllowed(player, warp)) {
            return;
        }

        if (!isConfirmed) {
            if (hasFee()) {
                if (!EconomyUtil.has(player, getFee())) {
                    player.sendMessage(
                            Lang.INSUFFICIENT_BALANCE_FOR_ACTION.asColoredString()
                                    .replace("%price%", NumberUtil.formatNumber(getFee()))
                    );
                    return;
                }

                PlayerWarpsPlugin.get().runSync(() -> {
                    new ConfirmationMenu(warp, data)
                            .setMenuToOpen(menuToOpen)
                            .open(player, this);
                });

                return;
            }
        }

        // Taken before the action runs and only kept when it succeeds - the balance may have changed
        // since it was checked, and a failed withdrawal must not leave the action done for free.
        // The admission of a teleport is handled by TeleportToWarpAction itself.
        final int fee = hasFee() && !(this instanceof TeleportToWarpAction) ? getFee() : 0;
        if (!EconomyUtil.withdraw(player, fee)) {
            player.sendMessage(
                    Lang.INSUFFICIENT_BALANCE_FOR_ACTION.asColoredString()
                            .replace("%price%", NumberUtil.formatNumber(fee))
            );
            return;
        }

        boolean proceeded = execute(player, warp, data);

        if (!proceeded) {
            EconomyUtil.deposit(player, fee);
        }

        if (menuToOpen != null) {
            if (menuToOpen instanceof WarpsMenu warpsMenu) {
                warpsMenu.setPage(page);
            }
            menuToOpen.openFor(player);
        }
    }

    /**
     * Whether the player may run this action on the warp: they need the action's permission
     * and, unless the action is public, have to be able to manage the warp. Tells the player
     * why not. Every way of running an action has to go through this - {@link #execute} itself
     * checks nothing.
     * <p>
     * Actions without a permission only navigate between menus and are always allowed.
     */
    default boolean isAllowed(Player player, @Nullable Warp warp) {
        final PermissionUtil.Permission permission = getPermission();
        if (permission == null) {
            return true;
        }

        if (permission != PermissionUtil.Permission.VOID && !PermissionUtil.hasPermission(player, permission)) {
            player.sendMessage(Lang.INSUFFICIENT_PERMISSIONS.asColoredString().replace("%permission%", permission.asString()));
            return false;
        }

        if (warp != null && !isPublicAction() && !warp.canManage(player)) {
            player.sendMessage(Lang.NOT_OWNING.asColoredString());
            return false;
        }

        return true;
    }

    boolean execute(Player player, Warp warp, T data);

    default PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.VOID;
    }

    default boolean hasToBeConfirmed() {
        return hasFee() || this instanceof RemoveWarpAction;
    }

    default boolean hasInput() {
        return false;
    }

    default Lang getMessage() {
        return null;
    }

    default int getFee() {
        return 0;
    }

    /**
     * Without an economy there is nothing to pay, so no fee is shown or confirmed either.
     */
    default boolean hasFee() {
        return getFee() > 0 && EconomyUtil.isEnabled();
    }

    default boolean isPublicAction() {
        return false;
    }

    default WarpManager getWarpManager() {
        return PlayerWarpsPlugin.getWarpHandler();
    }
}
