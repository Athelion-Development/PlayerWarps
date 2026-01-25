package dev.revivalo.playerwarps.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

public final class PlayerUtil {
    public static OfflinePlayer getOfflinePlayer(final UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }

    public static OfflinePlayer getOfflinePlayer(final String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }

    public static void announce(String message, Player... except) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!Arrays.asList(except).contains(player)) {
                player.sendMessage(message);
            }
        }
    }

//    public static void sendMessage(Player player, String message) {
//        if (!message.isEmpty()) player.sendMessage(message);
//        dokončit a otestovat!
//    }

    public static void announce(String message) {
        announce(message, new Player[]{});
    }

    public static boolean isPlayerOnline(final UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }
}