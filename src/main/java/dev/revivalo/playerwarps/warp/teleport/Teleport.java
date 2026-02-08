package dev.revivalo.playerwarps.warp.teleport;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.teleport.task.TeleportTask;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class Teleport {
    private final Player player;
    private final Location targetLocation;
    private final boolean runTimer;
    private final int delay;

    private TeleportTask task;

    public Teleport(Player player, Location targetLocation) {
        this.player = player;
        this.targetLocation = targetLocation;

        boolean withCooldown = !PermissionUtil.hasPermission(player, PermissionUtil.Permission.BYPASS_TELEPORT_DELAY);
        this.delay = Config.TELEPORT_DELAY.asInteger();

        this.runTimer = withCooldown && this.delay != 0;
    }

    public void proceed() {
        if (shouldRunTimer()) player.sendMessage(Lang.TELEPORTATION.asColoredString().replace("%time%", String.valueOf(delay)));
        this.task = new TeleportTask(this);
        this.task.runTaskTimer(PlayerWarpsPlugin.get(), 0, 10);
    }

    public boolean isSafe() {
        if (targetLocation == null || targetLocation.getWorld() == null) {
            return false;
        }

        Block headBlock = targetLocation.clone().add(0, 1, 0).getBlock();
        Block groundBlock = targetLocation.clone().add(0, -1, 0).getBlock();

        if (!isAir(headBlock)) {
            return false;
        }

        return isSolidGround(groundBlock.getType());
    }

    private boolean isAir(Block block) {
        return block.isEmpty();
    }

    private boolean isSolidGround(Material material) {
        return material.isSolid() && material != Material.LAVA && material != Material.CACTUS;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getTargetLocation() {
        return targetLocation;
    }

    public TeleportTask getTask() {
        return task;
    }

    public boolean shouldRunTimer() {
        return runTimer;
    }

    public int getDelay() {
        return delay;
    }

    public enum Status {
        SUCCESS,
        PROCESSING,
        ERROR
    }
}
