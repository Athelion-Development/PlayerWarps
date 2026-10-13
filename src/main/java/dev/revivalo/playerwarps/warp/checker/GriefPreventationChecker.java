package dev.revivalo.playerwarps.warp.checker;

import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.GriefPreventionHook;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GriefPreventationChecker implements Checker {
    private final GriefPrevention griefPrevention;
    public GriefPreventationChecker(GriefPreventionHook griefPreventionHook) {
        this.griefPrevention = griefPreventionHook.getApi();
    }

    @Override
    public boolean validate(Player player) {
        Location loc = player.getLocation();
        Claim claim = griefPrevention.dataStore.getClaimAt(loc, true, null);
        if (claim == null) {
            return true;
        }

        // Admin claims have no owner, so only trust can allow a warp there.
        final boolean isOwner = player.getUniqueId().equals(claim.getOwnerID());
        final boolean isTrusted = allowTrusted() && claim.checkPermission(player, ClaimPermission.Build, null) == null;
        if (!isOwner && !isTrusted) {
            player.sendMessage(Lang.TRIED_TO_CREATE_WARP_IN_FOREIGN_CLAIM.asColoredString());
            return false;
        }

        return true;
    }
}