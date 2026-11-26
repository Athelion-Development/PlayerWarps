package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.economy.WarpPrice;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.*;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.PlayerUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpApproval;
import dev.revivalo.playerwarps.warp.checker.ClaimCheckers;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class CreateWarpAction implements WarpAction<Void> {

    private final String name;

    /**
     * Worked out once, when the action is created, so the price the player confirms is the
     * price they pay.
     */
    private final int fee;

    public CreateWarpAction(String name, Player creator) {
        this.name = name;
        this.fee = WarpPrice.of(creator);
    }

    @Override
    public boolean execute(Player player, Warp warp, Void ignored) {
        if (!PlayerWarpsPlugin.getWarpHandler().canHaveWarp(player)) {
            player.sendMessage(Lang.LIMIT_REACHED.asColoredString()
                    .replace(
                            "%limit%",
                            String.valueOf(PermissionUtil.getLimit(player, Config.DEFAULT_LIMIT_SIZE.asInteger()))
                    )
            );
            return false;
        }

        final String worldName = Objects.requireNonNull(player.getLocation().getWorld()).getName();
        if (PlayerWarpsPlugin.getWarpHandler().getBannedWorlds().contains(worldName)
                && !PermissionUtil.hasPermission(player, PermissionUtil.Permission.ADMIN)) {
            player.sendMessage(Lang.TRIED_TO_CREATE_WARP_IN_DISABLED_WORLD.asColoredString().replace("%world%", worldName));
            return false;
        }

        if (PlayerWarpsPlugin.getWarpHandler().existsWarp(name)) {
            player.sendMessage(Lang.WARP_ALREADY_CREATED.asColoredString());
            return false;
        }

        if (getWarpManager().isReservedName(name)) {
            player.sendMessage(Lang.WARP_NAME_RESERVED.asColoredString().replace("%name%", name));
            return false;
        }

        int limit = Config.WARP_NAME_MAX_LENGTH.asInteger();
        if (name.length() > limit) {
            player.sendMessage(Lang.WARP_NAME_IS_ABOVE_LETTERS_LIMIT.asColoredString().replace("%limit%", String.valueOf(limit)));
            return false;
        }

        if (!ClaimCheckers.canPlaceWarp(player)) {
            return false;
        }

        if (/*warpName.contains(".") ||*/ name.contains(" ")) {
            player.sendMessage(Lang.NAME_CANT_CONTAINS_SPACE.asColoredString());
            return false;
        }

        final UUID ownerID = player.getUniqueId();
        final UUID warpID = UUID.randomUUID();

        final Location loc = player.getLocation();
        final boolean needsApproval = WarpApproval.requiresApproval(player);

        Warp createdWarp = new Warp(
                new HashMap<String, Object>() {{
                    put("uuid", warpID.toString());
                    put("name", name);
                    put("displayName", name);
                    put("owner-id", ownerID.toString());
                    put("need-verification", needsApproval);
                    // Kept to be given back if the warp gets denied.
                    put("approval-fee", needsApproval && hasFee() ? getFee() : 0);
                    put("loc", loc);
                    put("ratings", 0);
                    put("visits", 0);
                    put("category", "all");
                    put("lore", null);
                    put("admission", 0);
                    put("reviewers", Collections.emptyList());
                    put("blocked-players", Collections.emptyList());
                    put("todayVisits", 0);
                    put("date-created", System.currentTimeMillis());
                    put("featured", 0);
                    put("item", null);
                    put("status", Config.DEFAULT_WARP_STATUS.asUppercase());
                }}
        );

        PlayerWarpsPlugin.getWarpHandler().addWarp(createdWarp);
        // Not left to the next autosave: the warp has been paid for, a crash must not lose it.
        PlayerWarpsPlugin.getWarpHandler().saveWarps();

        HookRegister.ifEnabled(DynmapHook.class, dynmapHook -> dynmapHook.setMarker(createdWarp));
        HookRegister.ifEnabled(BlueMapHook.class, blueMapHook -> blueMapHook.setMarker(createdWarp));
        HookRegister.ifEnabled(SquaremapHook.class, squaremapHook -> squaremapHook.setMarker(createdWarp));

        // Without an economy (or with a zero price) nothing was paid, so no price is mentioned.
        final String message = hasFee()
                ? Lang.WARP_CREATED_WITH_PRICE.asColoredString()
                        .replace("%name%", name)
                        .replace("%price%", String.valueOf(getFee()))
                : Lang.WARP_CREATED.asColoredString().replace("%name%", name);

        BaseComponent[] msg = TextComponent.fromLegacyText(message);
        for (BaseComponent bc : msg) {
            bc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Lang.CLICK_TO_CONFIGURE.asColoredString())));
            bc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/playerwarps:pwarp manage " + name));
        }

        player.spigot().sendMessage(msg);

        if (needsApproval) {
            player.sendMessage(Lang.WARP_SUBMITTED_FOR_APPROVAL.asColoredString().replace("%warp%", name));
            WarpApproval.notifyApprovers(createdWarp);
            // Announced once the warp is approved and other players can visit it.
            return true;
        }

        if (Config.WARP_CREATION_NOTIFICATION.asBoolean())
            PlayerUtil.announce(Lang.WARP_CREATION_NOTIFICATION.asColoredString()
                            .replace("%warp%", name)
                            .replace("%player%", player.getName()),
                    player
            );

        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.CREATE_WARP;
    }

    @Override
    public int getFee() {
        return fee;
    }

    @Override
    public boolean isPublicAction() {
        return true;
    }
}
