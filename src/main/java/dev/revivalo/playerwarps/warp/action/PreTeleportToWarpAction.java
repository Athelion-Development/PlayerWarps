package dev.revivalo.playerwarps.warp.action;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.register.VaultHook;
import dev.revivalo.playerwarps.menu.page.ConfirmationMenu;
import dev.revivalo.playerwarps.menu.page.Menu;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.util.NumberUtil;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collections;

public class PreTeleportToWarpAction implements WarpAction<String> {
    private Menu menuToOpen = null;

    @Override
    public boolean execute(Player player, Warp warp, String data) {
        final Category category = warp.getCategory();
        if (!category.hasPermission(player)) {
            player.sendMessage(Lang.INSUFFICIENT_PERMISSIONS.asColoredString().replace("%permission%", category.getPermission()));
            return false;
        }

        if (!warp.canManage(player)) {
            if (warp.hasAdmission()) {
                boolean canAfford = HookRegister.mapIfEnabled(VaultHook.class, vaultHook -> {
                    return vaultHook.getApi().has(player, warp.getAdmission());
                }, true);

                if (!canAfford) {
                    player.sendMessage(Lang.INSUFFICIENT_BALANCE_TO_TELEPORT.asColoredString()
                            .replace("%warp%", warp.getName())
                            .replace("%price%", NumberUtil.formatNumber(warp.getAdmission())));
                    return false;
                }

            }
        }

        final ConfirmationMenu<String> confirmationMenu = new ConfirmationMenu<>(warp);
        confirmationMenu.setMenuToOpen(menuToOpen);

        final TeleportToWarpAction teleportToWarpAction = new TeleportToWarpAction(warp.getAdmission());
        //confirmationMenu.open(player, teleportToWarpAction);

        if (warp.isPasswordProtected() && !warp.canManage(player)) {
            SignGUI gui;
            try {
                gui = SignGUI.builder()
                        .setType(Material.OAK_SIGN)
                        .setColor(DyeColor.BLACK)
                        .setLine(1, Lang.ENTER_PASSWORD.asColoredString())
                        .setHandler((p, result) -> {
                            String input = result.getLineWithoutColor(0);

                            if (input.isEmpty()) {
                                return Collections.emptyList();
                            }

                            if (input.length() < 3 || input.length() > 15) {
                                return Collections.emptyList();
                            }

                            if (warp.isPasswordProtected()) {
                                if (!warp.validatePassword(input)) {
                                    player.sendMessage(Lang.ENTERED_WRONG_PASSWORD.asColoredString());
                                    return Collections.emptyList();
                                }
                            }

                            PlayerWarpsPlugin.get().runDelayed(() -> {
                                if (warp.hasAdmission() && !warp.canManage(player)) {
                                    confirmationMenu.open(player, teleportToWarpAction);
                                } else new TeleportToWarpAction().proceed(player, warp, input);
                            }, 2);

                            return Collections.emptyList();
                        })

                        .build();
            } catch (SignGUIVersionException e) {
                throw new RuntimeException(e);
            }

            gui.open(player);

        } else {
            if (warp.hasAdmission() && !warp.canManage(player))
                confirmationMenu.open(player, teleportToWarpAction);
            else new TeleportToWarpAction().proceed(player, warp);
        }
        return false;
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
