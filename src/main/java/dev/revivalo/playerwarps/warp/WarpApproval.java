package dev.revivalo.playerwarps.warp;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.playerconfig.PlayerConfig;
import dev.revivalo.playerwarps.util.EconomyUtil;
import dev.revivalo.playerwarps.util.NumberUtil;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.PlayerUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Approval of new warps, turned on by {@code demand-verification} in config.yml. A warp created
 * by a player without {@code playerwarps.verify} waits for approval: it is hidden from everyone
 * except its owner and the approvers until one of them approves it, or removed when it is denied.
 */
public final class WarpApproval implements Listener {
    /**
     * Messages for owners who were offline when their warp was decided, delivered on their next join.
     */
    private static final String NOTICES_KEY = "approval-notices";
    private static final String APPROVALS_COMMAND = "/playerwarps:pwarp approvals";
    private static final long JOIN_MESSAGE_DELAY = 40;

    public WarpApproval() {
        PlayerWarpsPlugin.get().registerListeners(this);
    }

    public static boolean isEnabled() {
        return Config.DEMAND_VERIFICATION.asBoolean();
    }

    /**
     * Whether a warp the player creates now has to be approved first. Approvers' own warps never do.
     */
    public static boolean requiresApproval(Player creator) {
        return isEnabled() && !PermissionUtil.hasPermission(creator, PermissionUtil.Permission.VERIFY);
    }

    /**
     * Warps waiting for approval, the longest waiting first.
     */
    public static List<Warp> getPendingWarps() {
        return PlayerWarpsPlugin.getWarpHandler().getWarps().stream()
                .filter(Warp::isVerificationNeeded)
                .sorted(Comparator.comparingLong(Warp::getDateCreated))
                .toList();
    }

    /**
     * Tells the approvers online that a new warp waits for them.
     */
    public static void notifyApprovers(Warp warp) {
        final String message = Lang.WARP_AWAITING_APPROVAL.asReplacedString(Map.of(
                "%warp%", warp.getName(),
                "%player%", warp.getOwnerName()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PermissionUtil.hasPermission(player, PermissionUtil.Permission.VERIFY)
                    && !player.getUniqueId().equals(warp.getOwner())) {
                sendOpeningApprovals(player, message);
            }
        }
    }

    public static boolean approve(CommandSender approver, Warp warp) {
        if (!isAwaitingApproval(warp)) {
            approver.sendMessage(Lang.WARP_NOT_AWAITING_APPROVAL.asColoredString().replace("%warp%", warp.getName()));
            return false;
        }

        warp.setVerificationNeeded(false);
        warp.setApprovalFee(0);
        PlayerWarpsPlugin.getWarpHandler().saveWarps();
        PlayerWarpsPlugin.getWarpHandler().showOnMaps(warp);

        approver.sendMessage(Lang.YOU_APPROVED_WARP.asReplacedString(Map.of(
                "%warp%", warp.getName(),
                "%player%", warp.getOwnerName())));

        notifyOwner(warp, Lang.WARP_APPROVED.asColoredString().replace("%warp%", warp.getName()), null);

        // The creation is announced now, when other players can actually visit the warp.
        if (Config.WARP_CREATION_NOTIFICATION.asBoolean()) {
            final Player owner = Bukkit.getPlayer(warp.getOwner());
            PlayerUtil.announce(Lang.WARP_CREATION_NOTIFICATION.asColoredString()
                    .replace("%warp%", warp.getName())
                    .replace("%player%", warp.getOwnerName()), owner);
        }

        return true;
    }

    /**
     * Menus and reason prompts hold on to the warp, so it may have been decided or removed meanwhile.
     */
    private static boolean isAwaitingApproval(Warp warp) {
        return warp.isVerificationNeeded()
                && PlayerWarpsPlugin.getWarpHandler().getWarpByID(warp.getWarpID()).isPresent();
    }

    /**
     * Removes the warp and tells its owner why. What they paid for creating it is given back
     * when {@code refund-denied-warps} is enabled.
     *
     * @param reason shown to the owner, or null when none was given
     */
    public static boolean deny(CommandSender approver, Warp warp, @Nullable String reason) {
        if (!isAwaitingApproval(warp)) {
            approver.sendMessage(Lang.WARP_NOT_AWAITING_APPROVAL.asColoredString().replace("%warp%", warp.getName()));
            return false;
        }

        PlayerWarpsPlugin.getWarpHandler().removeWarp(warp);
        // Written right away, the same as a removed warp - a refund may be paid for it.
        PlayerWarpsPlugin.getWarpHandler().saveWarps();
        PlayerWarpsPlugin.getWarpHandler().hideFromMaps(warp);

        final int refund = warp.getApprovalFee();
        final boolean refunded = refund > 0 && Config.REFUND_DENIED_WARPS.asBoolean() && EconomyUtil.isEnabled();
        if (refunded) {
            EconomyUtil.deposit(PlayerUtil.getOfflinePlayer(warp.getOwner()), refund);
        }

        approver.sendMessage(Lang.YOU_DENIED_WARP.asReplacedString(Map.of(
                "%warp%", warp.getName(),
                "%player%", warp.getOwnerName())));

        final String shownReason = reason == null || reason.isBlank()
                ? Lang.NO_DENIAL_REASON.asColoredString()
                : reason.trim();
        notifyOwner(warp,
                Lang.WARP_DENIED.asColoredString().replace("%warp%", warp.getName()),
                Lang.WARP_DENIAL_REASON.asColoredString().replace("%reason%", shownReason));

        if (refunded) {
            notifyOwner(warp, Lang.DENIED_WARP_REFUNDED.asReplacedString(Map.of(
                    "%warp%", warp.getName(),
                    "%refund%", NumberUtil.formatNumber(refund))), null);
        }

        return true;
    }

    /**
     * Sends the message to the owner, or keeps it for their next join when they are offline.
     */
    private static void notifyOwner(Warp warp, String message, @Nullable String hover) {
        final Player owner = Bukkit.getPlayer(warp.getOwner());
        if (owner != null) {
            send(owner, message, hover);
            return;
        }

        final PlayerConfig config = PlayerConfig.getConfig(warp.getOwner());
        final List<String> notices = new ArrayList<>(config.getStringList(NOTICES_KEY));
        notices.add(hover == null ? message : message + "\n" + hover);
        config.set(NOTICES_KEY, notices);
        config.save();
    }

    private static void send(Player player, String message, @Nullable String hover) {
        if (hover == null) {
            player.sendMessage(message);
            return;
        }

        final BaseComponent[] components = TextComponent.fromLegacyText(message);
        final BaseComponent[] hoverComponents = TextComponent.fromLegacyText(hover);
        for (BaseComponent component : components) {
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents));
        }

        player.spigot().sendMessage(components);
    }

    private static void sendOpeningApprovals(Player player, String message) {
        final BaseComponent[] components = TextComponent.fromLegacyText(message);
        final BaseComponent[] hover = TextComponent.fromLegacyText(Lang.CLICK_TO_OPEN_APPROVALS.asColoredString());
        for (BaseComponent component : components) {
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, APPROVALS_COMMAND));
        }

        player.spigot().sendMessage(components);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        PlayerWarpsPlugin.get().runDelayed(() -> {
            if (!player.isOnline()) {
                return;
            }

            deliverNotices(player);

            if (isEnabled() && Config.REMIND_APPROVERS_ON_JOIN.asBoolean()
                    && PermissionUtil.hasPermission(player, PermissionUtil.Permission.VERIFY)) {
                final int pending = getPendingWarps().size();
                if (pending > 0) {
                    sendOpeningApprovals(player, Lang.WARPS_AWAITING_APPROVAL_REMINDER.asColoredString()
                            .replace("%count%", String.valueOf(pending)));
                }
            }
        }, JOIN_MESSAGE_DELAY);
    }

    private static void deliverNotices(Player player) {
        final PlayerConfig config = PlayerConfig.getConfig(player);
        final List<String> notices = config.getStringList(NOTICES_KEY);
        if (notices.isEmpty()) {
            return;
        }

        notices.forEach(notice -> {
            final int separator = notice.indexOf('\n');
            if (separator < 0) {
                send(player, notice, null);
            } else {
                send(player, notice.substring(0, separator), notice.substring(separator + 1));
            }
        });

        config.set(NOTICES_KEY, null);
        config.save();
    }
}
