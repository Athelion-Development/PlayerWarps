package dev.revivalo.playerwarps.warp.checker;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

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

        // Regions listed here allow warps for everyone; the list is missing when the key is blank or not a list
        List<String> enabledRegions = Config.ENABLED_REGIONS.asList();
        if (enabledRegions == null) {
            enabledRegions = Collections.emptyList();
        }

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        for (ProtectedRegion region : set) {
            // WorldGuard stores region IDs in lowercase, so match config entries regardless of case
            if (enabledRegions.stream().anyMatch(region.getId()::equalsIgnoreCase)) continue;
            if (!region.isOwner(localPlayer)) {
                player.sendMessage(Lang.TRIED_TO_CREATE_WARP_IN_FOREIGN_REGION.asColoredString().replace("%region%", region.getId()));
                return false;
            }
        }

        return true;
    }
}
