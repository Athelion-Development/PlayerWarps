package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.*;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.checker.*;
import org.bukkit.entity.Player;

import java.util.*;

public class RelocateAction implements WarpAction<Void> {
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

    @Override
    public boolean execute(Player player, Warp warp, Void data) {
        final String worldName = Objects.requireNonNull(player.getLocation().getWorld()).getName();
        if (PlayerWarpsPlugin.getWarpHandler().getBannedWorlds().contains(worldName)
                && !PermissionUtil.hasPermission(player, PermissionUtil.Permission.ADMIN)) {
            player.sendMessage(Lang.TRIED_TO_RELOCATE_WARP_TO_DISABLED_WORLD.asColoredString().replace("%world%", worldName));
            return false;
        }

        for (Checker checker : checkers) {
            if (!checker.check(player)) {
                return false;
            }
        }

        warp.setLocation(player.getLocation());
        player.sendMessage(Lang.WARP_RELOCATED.asReplacedString(player, new HashMap<String, String>() {{
            put("%warp%", warp.getName());
        }}));

        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.RELOCATE_WARP;
    }

    @Override
    public int getFee() {
        return Config.RELOCATE_WARP_FEE.asInteger();
    }
}
