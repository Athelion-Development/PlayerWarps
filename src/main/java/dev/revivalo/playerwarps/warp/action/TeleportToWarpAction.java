package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookManager;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.PlayerUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.teleport.Teleport;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.UUID;

public class TeleportToWarpAction implements WarpAction<String> {
    @Override
    public boolean execute(Player player, Warp warp, String password) {
        if (warp == null) {
            return false;
        }

        final String warpName = warp.getName();
        boolean isOwner = warp.canManage(player);

        if (!warp.isAccessible() && !isOwner) {
            player.sendMessage(Lang.WARP_IS_DISABLED.asColoredString().replace("%warp%", warpName));
            return false;
        }

        if (warp.isBlocked(player) && !isOwner) {
            player.sendMessage(Lang.WARP_ACCESS_BLOCKED.asColoredString().replace("%warp%", warpName));
            return false;
        }

        Teleport teleport = new Teleport(player, warp.getLocation());
        teleport.proceed();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (teleport.getTask().isResulted()) {
                    cancel();
                    if (teleport.getTask().getStatus() == Teleport.Status.SUCCESS) {
                        if (!warp.canManage(player)) {
                            if (HookManager.isHookEnabled(HookManager.getVaultHook())) {
                                Economy economy = HookManager.getVaultHook().getApi();

                                economy.withdrawPlayer(player, warp.getAdmission());

                                PlayerUtil.getOfflinePlayer(warp.getOwner()).thenAccept(
                                        offlinePlayer -> economy.depositPlayer(offlinePlayer, warp.getAdmission())
                                );
                            }
                        }

                        if (Config.WARP_VISIT_NOTIFICATION.asBoolean()) {
                            PlayerUtil.announce(Lang.WARP_VISIT_NOTIFICATION.asColoredString()
                                            .replace("%warp%", warpName)
                                            .replace("%player%", player.getName()),
                                    player
                            );
                        }

                        final UUID ownerID = warp.getOwner();

                        PlayerUtil.getOfflinePlayer(ownerID).thenAccept(offlinePlayer -> {
                            if (warp.getAdmission() != 0 && !isOwner) {
                                player.sendMessage(Lang.TELEPORT_TO_WARP_WITH_ADMISSION.asColoredString()
                                        .replace("%price%", String.valueOf(warp.getAdmission()))
                                        .replace("%warp%", warpName)
                                        .replace("%player%", Objects.requireNonNull(Bukkit.getOfflinePlayer(ownerID).getName())));
                            } else
                                player.sendMessage(Lang.TELEPORT_TO_WARP.asColoredString()
                                        .replace("%warp%", warpName)
                                        .replace("%player%", Objects.requireNonNull(Bukkit.getOfflinePlayer(ownerID).getName())));

                        });

                        if (!isOwner) {
                            warp.setVisits(warp.getVisits() + 1);
                            warp.setTodayVisits(warp.getTodayVisits() + 1);
                        }
                    } else if (teleport.getTask().getStatus() == Teleport.Status.ERROR) {
                        player.sendMessage(Lang.TELEPORTATION_CANCELLED.asColoredString());
                    } else if (teleport.getTask().getStatus() == Teleport.Status.UNSAFE) {
                        player.sendMessage(Lang.DANGEROUS_POSITION_MSG.asColoredString());
                    }
                }
            }

        }.runTaskTimer(PlayerWarpsPlugin.get(), 2, 2);

        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.USE;
    }

    @Override
    public boolean isPublicAction() {
        return true;
    }
}
