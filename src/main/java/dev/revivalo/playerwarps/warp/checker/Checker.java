package dev.revivalo.playerwarps.warp.checker;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.util.PermissionUtil;
import org.bukkit.entity.Player;

public interface Checker {
    default boolean check(Player player) {
        // Admins may place warps anywhere, the same as for disabled worlds.
        if (PermissionUtil.hasPermission(player, PermissionUtil.Permission.ADMIN)) {
            return true;
        }

        return validate(player);
    }

    boolean validate(Player player);

    /**
     * Whether players trusted in a claim may create warps there too, not only its owner.
     */
    default boolean allowTrusted() {
        return Config.WARP_CREATION_FOR_TRUSTED_PLAYERS.asBoolean();
    }
}
