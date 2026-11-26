package dev.revivalo.playerwarps.warp.teleport;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.warp.teleport.task.TeleportTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cancels a running teleport delay as soon as the player moves away, takes damage or leaves.
 * The task itself only checks every half a second, which would let a player step away and
 * back in between.
 */
public class TeleportListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent event) {
        final TeleportTask task = TeleportTask.getRunning(event.getPlayer().getUniqueId());
        if (task != null && task.hasMoved(event.getTo())) {
            task.fail();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !Config.CANCEL_TELEPORT_ON_DAMAGE.asBoolean()) {
            return;
        }

        final TeleportTask task = TeleportTask.getRunning(player.getUniqueId());
        if (task != null && task.isDelayed()) {
            task.fail();
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        final TeleportTask task = TeleportTask.getRunning(event.getPlayer().getUniqueId());
        if (task != null) {
            task.fail();
        }
    }
}
