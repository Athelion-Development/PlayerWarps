package dev.revivalo.playerwarps.warp.checker;

import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.GriefPreventionHook;
import me.ryanhamshire.GriefPrevention.Claim;
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

        if (!claim.getOwnerID().equals(player.getUniqueId())) {
            player.sendMessage(Lang.TRIED_TO_CREATE_WARP_IN_FOREIGN_CLAIM.asColoredString());
            return false;
        }

        return true;
    }
}