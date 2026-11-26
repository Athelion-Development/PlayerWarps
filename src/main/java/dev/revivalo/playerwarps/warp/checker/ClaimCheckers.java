package dev.revivalo.playerwarps.warp.checker;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.*;
import dev.revivalo.playerwarps.util.PermissionUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides whether a player may place a warp (create or relocate one) where they stand,
 * using every claim plugin PlayerWarps hooks into.
 */
public final class ClaimCheckers {
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

    private ClaimCheckers() {
        throw new RuntimeException("This class cannot be instantiated");
    }

    public static boolean canPlaceWarp(Player player) {
        // Admins may place warps anywhere, the same as for disabled worlds.
        if (PermissionUtil.hasPermission(player, PermissionUtil.Permission.ADMIN)) {
            return true;
        }

        // One plugin may see wilderness where another has the player's own claim (a GriefPrevention
        // claim outside of any WorldGuard region), so the own-claim rule is decided over all of them.
        boolean inOwnClaim = false;
        for (Checker checker : checkers) {
            switch (checker.validate(player)) {
                case DENIED:
                    return false;
                case ALLOWED:
                    inOwnClaim = true;
                    break;
                default:
                    break;
            }
        }

        if (isOwnClaimRequired() && !inOwnClaim) {
            player.sendMessage(Lang.WARP_CREATION_REQUIRES_OWN_CLAIM.asColoredString());
            return false;
        }

        return true;
    }

    /**
     * Logs when require-own-claim is on but cannot be enforced, so it is not silently ignored.
     */
    public static void warnIfOwnClaimUnenforceable() {
        if (Config.REQUIRE_OWN_CLAIM.asBoolean() && checkers.isEmpty()) {
            PlayerWarpsPlugin.get().getLogger().warning("require-own-claim is enabled, but no supported claim "
                    + "plugin is hooked. The option is ignored and warps can be placed anywhere.");
        }
    }

    // Without any claim plugin no location can be owned, so the option would block every warp.
    private static boolean isOwnClaimRequired() {
        return Config.REQUIRE_OWN_CLAIM.asBoolean() && !checkers.isEmpty();
    }
}
