package dev.revivalo.playerwarps.warp.checker;

import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Optional;

public class BentoBoxIslandChecker implements Checker {
    @Override
    public boolean validate(Player player) {
        Optional<Island> islandOptional = HookRegister.getBentoBoxHook().getApi().getIslands().getIslandAt(player.getLocation());
        if (!islandOptional.isPresent()) {
            return true;
        }
        
        Island island = islandOptional.get();
        if (!island.isOwned()) {
            return true;
        }

        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Lang.TRIED_TO_CREATE_WARP_IN_FOREIGN_ISLAND.asColoredString());
            return false;
        }

        return true;
    }
}