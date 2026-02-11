package dev.revivalo.playerwarps.warp.checker;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import org.bukkit.entity.Player;

public class WorldGuardChecker implements Checker {
    private final WorldGuard worldGuard;

    public WorldGuardChecker() {
        this.worldGuard = WorldGuard.getInstance();
    }

    @Override
    public boolean validate(Player player) {
        RegionContainer regionContainer = worldGuard.getPlatform().getRegionContainer();
        if (regionContainer == null) {
            return true;
        }

        Location location = BukkitAdapter.adapt(player.getLocation());
        ApplicableRegionSet set = regionContainer.createQuery().getApplicableRegions(location);
        if (set.size() == 0) {
            return true;
        }

        for (ProtectedRegion region : set) {
            if (Config.ENABLED_REGIONS.asList().contains(region.getId())) continue;
            if (!region.isOwner(WorldGuardPlugin.inst().wrapPlayer(player))) {
                player.sendMessage(Lang.TRIED_TO_CREATE_WARP_IN_FOREIGN_REGION.asColoredString().replace("%region%", region.getId()));
                return false;
            }
        }

        return true;
    }
}
