package dev.revivalo.playerwarps.economy;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import org.bukkit.entity.Player;

public abstract class EconomyAction {
    public long calculate(Player player) {
        int free = Config.FREE_WARPS.asInteger();
        int size = PlayerWarpsPlugin.getWarpHandler().getPlayerWarps(player).size();
        if (size < free) return 0;
        return getPrice(size - free);
    }

    abstract long getPrice(int size);

    boolean hasPrice(Player player) {
        return calculate(player) > 0;
    }

    public enum Type {
        CREATE_WARP
    }
}
