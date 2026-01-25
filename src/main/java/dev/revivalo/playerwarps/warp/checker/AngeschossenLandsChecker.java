package dev.revivalo.playerwarps.warp.checker;

import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.land.LandWorld;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class AngeschossenLandsChecker implements Checker {
    @Override
    public boolean validate(Player player) {
        LandWorld world = HookRegister.getAngeschossenLands().getApi().getWorld(player.getWorld());
        if (world == null) {
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        Land land = world.getLandByChunk(chunk.getX(), chunk.getZ());

        if (land == null) {
            return true;
        }

        if (!land.isTrusted(player.getUniqueId())) {
            player.sendMessage(Lang.TRIED_TO_CREATE_WARP_IN_FOREIGN_LAND.asColoredString());
            return false;
        }

        return true;
    }
}
