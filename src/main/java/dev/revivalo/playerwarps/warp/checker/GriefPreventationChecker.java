package dev.revivalo.playerwarps.warp.checker;

import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GriefPreventationChecker implements Checker {
    @Override
    public boolean validate(Player player) {
        Location loc = player.getLocation();
        GriefPrevention griefPrevention = HookRegister.getGriefPreventionHook().getApi();
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