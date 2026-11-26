package dev.revivalo.playerwarps.warp.checker;

import dev.revivalo.playerwarps.configuration.file.Config;
import org.bukkit.entity.Player;

public interface Checker {
    ClaimResult validate(Player player);

    /**
     * Whether players trusted in a claim may create warps there too, not only its owner.
     */
    default boolean allowTrusted() {
        return Config.WARP_CREATION_FOR_TRUSTED_PLAYERS.asBoolean();
    }
}
