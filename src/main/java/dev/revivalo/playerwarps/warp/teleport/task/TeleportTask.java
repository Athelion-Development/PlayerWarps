package dev.revivalo.playerwarps.warp.teleport.task;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.warp.teleport.Teleport;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportTask extends BukkitRunnable {
    private final Teleport teleport;
    private final Player player;
    private final Location location;

    private final int locationXZ;

    private Teleport.Status status = Teleport.Status.PROCESSING;
    private int cycle = 0;

    private static final Set<Material> UNSAFE_BLOCKS = Set.of(
        Material.MAGMA_BLOCK,
        Material.CAMPFIRE,
        Material.SOUL_CAMPFIRE,
        Material.FIRE,
        Material.SOUL_FIRE,
        Material.CACTUS,
        Material.SWEET_BERRY_BUSH,
        Material.LAVA,
        Material.POWDER_SNOW,
        Material.CAULDRON
    );

    public TeleportTask(Teleport teleport) {
        this.teleport = teleport;
        this.player = teleport.getPlayer();
        this.location = teleport.getTargetLocation();
        this.locationXZ = this.player.getLocation().getBlockX() + this.player.getLocation().getBlockZ();
    }

    @Override
    public void run() {
        if (teleport.shouldRunTimer()) {
            if (!player.isOnline()) {
                cancel();
                status = Teleport.Status.ERROR;
                return;
            }

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(Lang.TELEPORTATION_PROGRESS.asColoredString()
                            .replace("%time%", String.valueOf(Config.TELEPORT_DELAY.asInteger() - (cycle / 2))))
            );

            if (locationXZ != (player.getLocation().getBlockX() + player.getLocation().getBlockZ())) {
                cancel();
                status = Teleport.Status.ERROR;
                return;
            }

            if (cycle == teleport.getDelay() * 2) {
                cancel();
                performTeleport();
                return;
            }

            ++cycle;
        } else {
            cancel();
            performTeleport();
        }
    }

    private boolean isSafeLocation() {
        // Clone location to make checks with it while not changing the original location
        Location checkLoc = location.clone();

        // Check if the location is air
        if (!checkLoc.getBlock().getType().equals(Material.AIR)) {
            return false;
        }

        // Check if the block above is air
        checkLoc.setY(checkLoc.getY() + 1);
        if (!checkLoc.getBlock().getType().equals(Material.AIR)) {
            return false;
        }

        // Check if the block below is a safe solid block
        checkLoc.setY(checkLoc.getY() - 2);
        if (
            !checkLoc.getBlock().getType().isSolid() ||
            UNSAFE_BLOCKS.contains(checkLoc.getBlock().getType()) ||
            checkLoc.getBlock().getType().name().endsWith("_PRESSURE_PLATE") ||
            checkLoc.getBlock().getType().name().endsWith("_TRAPDOOR") ||
            checkLoc.getBlock().getType().name().endsWith("_SIGN") ||
            checkLoc.getBlock().getType().name().endsWith("_DOOR") ||
            checkLoc.getBlock().getType().name().endsWith("_GATE") ||
            checkLoc.getBlock().getType().name().endsWith("_FENCE") ||
            checkLoc.getBlock().getType().name().endsWith("_BANNER") ||
            checkLoc.getBlock().getType().name().endsWith("_SHULKER_BOX")
        ) {
            return false;
        }

        return true;
    }

    private void performTeleport() {
        if (Config.CHECK_FOR_SAFE_TELEPORT.asBoolean() && !isSafeLocation()) {
            status = Teleport.Status.UNSAFE;
            return;
        }

        // If everything is good, teleport the player
        player.teleport(location);
        status = Teleport.Status.SUCCESS;
    }

    public boolean isResulted() {
        return status != Teleport.Status.PROCESSING;
    }

    public Teleport.Status getStatus() {
        return status;
    }
}