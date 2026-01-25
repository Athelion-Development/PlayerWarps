package dev.revivalo.playerwarps.warp.checker;

import eu.athelion.territory.api.Territory;
import eu.athelion.territory.land.Land;
import eu.athelion.territory.land.player.Permission;
import org.bukkit.entity.Player;

import java.util.Optional;

public class TerritoryChecker implements Checker {
    @Override
    public boolean validate(Player player) {
        Optional<Land> landOptional = Territory.getLandWithin(player.getLocation());
        if (!landOptional.isPresent()) {
            return true;
        }

        final Land land = landOptional.get();
        return land.canManipulate(player, Permission.INTERACT);
    }
}
