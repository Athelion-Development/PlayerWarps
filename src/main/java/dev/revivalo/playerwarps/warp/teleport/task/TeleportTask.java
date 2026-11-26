package dev.revivalo.playerwarps.warp.teleport.task;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.warp.teleport.Teleport;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class TeleportTask extends BukkitRunnable {
    /**
     * How far the player may move while the delay runs. Small steps and jumping on the spot
     * are fine, walking away or falling is not.
     */
    private static final double MAX_HORIZONTAL_MOVE_SQUARED = 0.5 * 0.5;
    private static final double MAX_VERTICAL_MOVE = 1.5;

    /**
     * The teleport each player is currently waiting for. Only touched on the main thread.
     */
    private static final Map<UUID, TeleportTask> RUNNING = new HashMap<>();

    private final Teleport teleport;
    private final Player player;
    private final Location location;
    private final Location startLocation;

    private Teleport.Status status = Teleport.Status.PROCESSING;
    private int cycle = 0;

    public TeleportTask(Teleport teleport) {
        this.teleport = teleport;
        this.player = teleport.getPlayer();
        this.location = teleport.getTargetLocation();
        this.startLocation = this.player.getLocation().clone();
    }

    /**
     * Starts the teleport. A teleport the player is still waiting for is cancelled, so two
     * delays never run side by side.
     */
    public void begin() {
        final TeleportTask previous = RUNNING.put(player.getUniqueId(), this);
        if (previous != null && previous != this) {
            previous.fail();
        }

        runTaskTimer(PlayerWarpsPlugin.get(), 0, 10);
    }

    @Nullable
    public static TeleportTask getRunning(UUID playerId) {
        return RUNNING.get(playerId);
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            fail();
            return;
        }

        if (teleport.shouldRunTimer()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(Lang.TELEPORTATION_PROGRESS.asColoredString()
                            .replace("%time%", String.valueOf(Config.TELEPORT_DELAY.asInteger() - (cycle / 2))))
            );

            if (hasMoved(player.getLocation())) {
                fail();
                return;
            }

            if (cycle == teleport.getDelay() * 2) {
                teleportPlayer();
                return;
            }

            ++cycle;
        } else {
            teleportPlayer();
        }
    }

    private void teleportPlayer() {
        // The world may have been unloaded while the delay was running.
        if (location == null || !location.isWorldLoaded()) {
            fail();
            return;
        }

        // A teleport fires a move event itself, which must not count as walking away.
        RUNNING.remove(player.getUniqueId(), this);

        // Another plugin (combat tag, jail, ...) can refuse the teleport.
        finish(player.teleport(location) ? Teleport.Status.SUCCESS : Teleport.Status.ERROR);
    }

    /**
     * Whether the player has left the spot the teleport was requested from. Only relevant while
     * the delay is running.
     */
    public boolean hasMoved(Location currentLocation) {
        if (!teleport.shouldRunTimer() || currentLocation == null) {
            return false;
        }

        if (!Objects.equals(currentLocation.getWorld(), startLocation.getWorld())) {
            return true;
        }

        final double dx = currentLocation.getX() - startLocation.getX();
        final double dz = currentLocation.getZ() - startLocation.getZ();
        return dx * dx + dz * dz > MAX_HORIZONTAL_MOVE_SQUARED
                || Math.abs(currentLocation.getY() - startLocation.getY()) > MAX_VERTICAL_MOVE;
    }

    public boolean isDelayed() {
        return teleport.shouldRunTimer();
    }

    /**
     * Cancels the teleport. The caller that started it learns about it through {@link #getStatus()}.
     */
    public void fail() {
        finish(Teleport.Status.ERROR);
    }

    private void finish(Teleport.Status result) {
        if (isResulted()) {
            return;
        }

        cancel();
        status = result;
        RUNNING.remove(player.getUniqueId(), this);
    }

    public boolean isResulted() {
        return status != Teleport.Status.PROCESSING;
    }

    public Teleport.Status getStatus() {
        return status;
    }
}
