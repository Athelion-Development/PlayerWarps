package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.EconomyUtil;
import dev.revivalo.playerwarps.util.NumberUtil;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.PlayerUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.teleport.Teleport;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TeleportToWarpAction implements WarpAction<String> {
    /**
     * Players who were warned that a warp location is unsafe. Repeating the teleport to the
     * same warp within the timeout forces it through, as the warning message advertises.
     */
    private final static Map<UUID, UnsafeConfirmation> unsafeConfirmations = new HashMap<>();
    private final static long UNSAFE_CONFIRMATION_TIMEOUT = 15_000L;

    /**
     * Admissions taken from visitors whose teleport has not finished yet. Identity based, so
     * two teleports of one player with the same price are kept apart.
     */
    private final static Set<HeldAdmission> heldAdmissions = Collections.newSetFromMap(new IdentityHashMap<>());

    private final int fee;

    public TeleportToWarpAction() {
        this.fee = 0;
    }

    public TeleportToWarpAction(int fee) {
        this.fee = fee;
    }

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

        if (warp.getLocation() == null) {
            player.sendMessage(Lang.WARP_IN_DELETED_WORLD.asColoredString());
            return false;
        }

        Teleport teleport = new Teleport(player, warp.getLocation());
        if (Config.CHECK_FOR_SAFE_TELEPORT.asBoolean() && !teleport.isSafe() && !hasConfirmedUnsafe(player, warp)) {
            unsafeConfirmations.put(player.getUniqueId(),
                    new UnsafeConfirmation(warp.getWarpID(), System.currentTimeMillis() + UNSAFE_CONFIRMATION_TIMEOUT));
            player.sendMessage(Lang.TELEPORTATION_UNSAFE.asColoredString());
            return false;
        }

        unsafeConfirmations.remove(player.getUniqueId());

        // Never more than the player confirmed: the owner can change the admission while the
        // teleport delay is running. Taken up front and held until the teleport is resolved, so
        // the owner is only paid what has really been taken from the visitor.
        final int admission = isOwner ? 0 : Math.min(fee, warp.getAdmission());
        if (!EconomyUtil.withdraw(player, admission)) {
            player.sendMessage(Lang.INSUFFICIENT_BALANCE_TO_TELEPORT.asColoredString()
                    .replace("%warp%", warpName)
                    .replace("%price%", NumberUtil.formatNumber(admission)));
            return false;
        }

        final HeldAdmission heldAdmission = new HeldAdmission(player, admission);
        if (admission > 0) {
            heldAdmissions.add(heldAdmission);
        }

        teleport.proceed();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (teleport.getTask().isResulted()) {
                    cancel();
                    heldAdmissions.remove(heldAdmission);

                    if (teleport.getTask().getStatus() == Teleport.Status.SUCCESS) {
                        EconomyUtil.deposit(PlayerUtil.getOfflinePlayer(warp.getOwner()), admission);

                        if (Config.WARP_VISIT_NOTIFICATION.asBoolean()) {
                            PlayerUtil.announce(Lang.WARP_VISIT_NOTIFICATION.asColoredString()
                                            .replace("%warp%", warpName)
                                            .replace("%player%", player.getName()),
                                    player
                            );
                        }

                        if (admission > 0) {
                            player.sendMessage(Lang.TELEPORT_TO_WARP_WITH_ADMISSION.asColoredString()
                                    .replace("%price%", String.valueOf(admission))
                                    .replace("%warp%", warpName)
                                    .replace("%player%", warp.getOwnerName()));
                        } else
                            player.sendMessage(Lang.TELEPORT_TO_WARP.asColoredString()
                                    .replace("%warp%", warpName)
                                    .replace("%player%", warp.getOwnerName()));

                        if (!isOwner) {
                            warp.setVisits(warp.getVisits() + 1);
                            warp.setTodayVisits(warp.getTodayVisits() + 1);
                            warp.addUniqueVisitor(player.getUniqueId());
                        }
                    } else {
                        heldAdmission.refund();
                        player.sendMessage(Lang.TELEPORTATION_CANCELLED.asColoredString());
                    }
                }
            }

        }.runTaskTimer(PlayerWarpsPlugin.get(), 2, 2);

        return true;
    }

    private static boolean hasConfirmedUnsafe(Player player, Warp warp) {
        final UnsafeConfirmation confirmation = unsafeConfirmations.get(player.getUniqueId());
        if (confirmation == null) {
            return false;
        }

        if (confirmation.expiresAt() < System.currentTimeMillis()) {
            unsafeConfirmations.remove(player.getUniqueId());
            return false;
        }

        return Objects.equals(confirmation.warpID(), warp.getWarpID());
    }

    private record UnsafeConfirmation(UUID warpID, long expiresAt) {
    }

    /**
     * Gives back every admission of a teleport that is still running. Called on shutdown, when
     * the tasks that would pay the owner or refund the visitor no longer run.
     */
    public static void refundHeldAdmissions() {
        heldAdmissions.forEach(HeldAdmission::refund);
        heldAdmissions.clear();
    }

    private record HeldAdmission(OfflinePlayer player, int amount) {
        void refund() {
            EconomyUtil.deposit(player, amount);
        }
    }

    @Override
    public int getFee() {
        return fee;
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
