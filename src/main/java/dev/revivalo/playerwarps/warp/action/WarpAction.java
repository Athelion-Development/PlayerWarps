package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.menu.page.ConfirmationMenu;
import dev.revivalo.playerwarps.menu.page.Menu;
import dev.revivalo.playerwarps.menu.page.WarpsMenu;
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
        if (!isConfirmed) {
            if (!PermissionUtil.hasPermission(player, getPermission())) {
                player.sendMessage(Lang.INSUFFICIENT_PERMISSIONS.asColoredString().replace("%permission%", getPermission().asString()));
                return;
            }

            if (warp != null) {
                if (!isPublicAction()) {
                    if (!warp.canManage(player)) {
                        player.sendMessage(Lang.NOT_OWNING.asColoredString());
                        return;
                    }
                }
            }

            if (getFee() != 0) {
                if (HookRegister.isHookEnabled(HookRegister.getVaultHook())) {
                    if (!HookRegister.getVaultHook().getApi().has(player, getFee())) {
                        player.sendMessage(Lang.INSUFFICIENT_BALANCE_FOR_ACTION.asColoredString().replace("%price%", NumberUtil.formatNumber(getFee())));
                        return;
                    }
                }

                PlayerWarpsPlugin.get().runSync(() -> {
                    new ConfirmationMenu(warp, data)
                            .setMenuToOpen(menuToOpen)
                            .open(player, this);
                });

                return;
            }
        }

        boolean proceeded = execute(player, warp, data);

        if (proceeded) {
            if (HookRegister.isHookEnabled(HookRegister.getVaultHook())) {
                HookRegister.getVaultHook().getApi().withdrawPlayer(player, getFee());
            }
        }

        if (menuToOpen != null) {
            if (menuToOpen instanceof WarpsMenu.DefaultWarpsMenu
                    || menuToOpen instanceof WarpsMenu.FavoriteWarpsMenu
                    || menuToOpen instanceof WarpsMenu.MyWarpsMenu) {
                ((WarpsMenu) menuToOpen)
                        .setPage(page);
            }
            menuToOpen
                    .open(player);
        }
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

    default boolean hasFee() {
        return getFee() != 0;
    }

    default Lang getInputText() {
        return Lang.ENTER_PASSWORD;
    }

    default boolean isPublicAction() {
        return false;
    }

    default WarpManager getWarpManager() {
        return PlayerWarpsPlugin.getWarpHandler();
    }
}
