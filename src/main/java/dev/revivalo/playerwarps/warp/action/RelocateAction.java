package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.checker.*;
import org.bukkit.entity.Player;

import java.util.*;

public class RelocateAction implements WarpAction<Void> {
    private static final List<Checker> checkers = new ArrayList<>();
    static {
        if (HookRegister.isHookEnabled(HookRegister.getBentoBoxHook())) checkers.add(new BentoBoxIslandChecker());
        if (HookRegister.isHookEnabled(HookRegister.getResidenceHook())) checkers.add(new ResidenceChecker());
        if (HookRegister.isHookEnabled(HookRegister.getWorldGuardHook())) checkers.add(new WorldGuardChecker());
        if (HookRegister.isHookEnabled(HookRegister.getTerritoryHook())) checkers.add(new TerritoryChecker());
        if (HookRegister.isHookEnabled(HookRegister.getSuperiorSkyBlockHook())) checkers.add(new SuperiorSkyBlockChecker());
        if (HookRegister.isHookEnabled(HookRegister.getAngeschossenLands())) checkers.add(new AngeschossenLandsChecker());
        if (HookRegister.isHookEnabled(HookRegister.getGriefPreventionHook())) checkers.add(new GriefPreventationChecker());
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
