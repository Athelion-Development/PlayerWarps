package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.page.ManageMenu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ManageCommand implements SubCommand {
    @Override
    public String getName() {
        return "manage";
    }

    @Override
    public String getDescription() {
        return "Opens manage menu for stated warp";
    }

    @Override
    public String getSyntax() {
        return "/pwarp manage [warpName]";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.MANAGE;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        return switch (index) {
            case 0 -> PlayerWarpsPlugin.getWarpHandler()
                    .getPlayerWarps(PermissionUtil.hasPermission(sender, PermissionUtil.Permission.MANAGE_OTHERS) ? null : player)
                    .stream()
                    .map(Warp::getName)
                    .collect(Collectors.toList());
            default -> Collections.emptyList();
        };
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[PlayerWarps] Only in-game command!");
            return;
        }

        if (args.length > 0) {
            Optional<Warp> warpOptional = PlayerWarpsPlugin.getWarpHandler().getWarpFromName(args[0]);
            if (warpOptional.isEmpty()) {
                player.sendMessage(Lang.NON_EXISTING_WARP.asColoredString());
                return;
            }

            Warp warp = warpOptional.get();
            if (!warp.canManage(player)) {
                player.sendMessage(Lang.NOT_OWNING.asColoredString());
                return;
            }

            new ManageMenu(warp)
                    .openFor(player);

        } else {
            player.sendMessage(Lang.BAD_COMMAND_SYNTAX.asColoredString().replace("%syntax%", getSyntax()));
        }
    }
}