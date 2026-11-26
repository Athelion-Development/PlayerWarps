package dev.revivalo.playerwarps.warp.checker;

import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.BentoBoxHook;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Optional;

public class BentoBoxIslandChecker implements Checker {

    private final BentoBox bentoBox;
    public BentoBoxIslandChecker(BentoBoxHook bentoBoxHook) {
        this.bentoBox = bentoBoxHook.getApi();
    }

    @Override
    public ClaimResult validate(Player player) {
        Optional<Island> islandOptional = bentoBox.getIslands().getIslandAt(player.getLocation());
        if (islandOptional.isEmpty()) {
            return ClaimResult.UNCLAIMED;
        }
        
        Island island = islandOptional.get();
        if (!island.isOwned()) {
            return ClaimResult.UNCLAIMED;
        }

        final boolean isOwner = player.getUniqueId().equals(island.getOwner());
        final boolean isMember = allowTrusted() && island.getMemberSet().contains(player.getUniqueId());
        if (!isOwner && !isMember) {
            player.sendMessage(Lang.TRIED_TO_CREATE_WARP_IN_FOREIGN_ISLAND.asColoredString());
            return ClaimResult.DENIED;
        }

        return ClaimResult.ALLOWED;
    }
}