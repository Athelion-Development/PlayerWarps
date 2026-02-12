package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.*;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.PlayerUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.checker.*;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class CreateWarpAction implements WarpAction<Void> {

    private final String name;

    private static final List<Checker> checkers = new ArrayList<>();

    static {
        HookRegister.ifEnabled(BentoBoxHook.class, bentoBoxHook -> checkers.add(new BentoBoxIslandChecker(bentoBoxHook)));
        HookRegister.ifEnabled(ResidenceHook.class, residenceHook -> checkers.add(new ResidenceChecker(residenceHook)));
        HookRegister.ifEnabled(WorldGuardHook.class, unused -> checkers.add(new WorldGuardChecker()));
        HookRegister.ifEnabled(TerritoryHook.class, unused -> checkers.add(new TerritoryChecker()));
        HookRegister.ifEnabled(TownyAdvancedHook.class, townyHook -> checkers.add(new TownyChecker(townyHook)));
        HookRegister.ifEnabled(SuperiorSkyBlockHook.class, unused -> checkers.add(new SuperiorSkyBlockChecker()));
        HookRegister.ifEnabled(AngeschossenLandsHook.class, angeschossenLandsHook ->
                checkers.add(new AngeschossenLandsChecker(angeschossenLandsHook)));
        HookRegister.ifEnabled(GriefPreventionHook.class, griefPreventionHook ->
                checkers.add(new GriefPreventationChecker(griefPreventionHook)));
    }

    public CreateWarpAction(String name) {
        this.name = name;
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

        int limit = Config.WARP_NAME_MAX_LENGTH.asInteger();
        if (name.length() > limit) {
            player.sendMessage(Lang.WARP_NAME_IS_ABOVE_LETTERS_LIMIT.asColoredString().replace("%limit%", String.valueOf(limit)));
            return false;
        }

        for (Checker checker : checkers) {
            if (!checker.check(player)) {
                return false;
            }
        }

        if (/*warpName.contains(".") ||*/ name.contains(" ")) {
            player.sendMessage(Lang.NAME_CANT_CONTAINS_SPACE.asColoredString());
            return false;
        }

        final UUID ownerID = player.getUniqueId();
        final UUID warpID = UUID.randomUUID();

        final Location loc = player.getLocation();

        Warp createdWarp = new Warp(
                new HashMap<String, Object>() {{
                    put("uuid", warpID.toString());
                    put("name", name);
                    put("displayName", name);
                    put("owner-id", ownerID.toString());
                    put("need-verification", Config.DEMAND_VERIFICATION.asBoolean());
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

        HookRegister.ifEnabled(DynmapHook.class, dynmapHook -> dynmapHook.setMarker(createdWarp));
        HookRegister.ifEnabled(BlueMapHook.class, blueMapHook -> blueMapHook.setMarker(createdWarp));

        String message;
        //if (HookRegister.isHookEnabled(HookRegister.getVaultHook())) {
        message = Lang.WARP_CREATED_WITH_PRICE.asColoredString()
                .replace("%name%", name)
                .replace("%price%", String.valueOf(getFee()));
        //} else message = Lang.WARP_CREATED.asColoredString().replace("%name%", name);

        BaseComponent[] msg = TextComponent.fromLegacyText(message);
        for (BaseComponent bc : msg) {
            bc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Lang.CLICK_TO_CONFIGURE.asColoredString())));
            bc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/playerwarps:pwarp manage " + name));
        }

        player.spigot().sendMessage(msg);

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
        return Config.WARP_FIXED_PRICE.asInteger();
    }

    @Override
    public boolean isPublicAction() {
        return true;
    }
}
