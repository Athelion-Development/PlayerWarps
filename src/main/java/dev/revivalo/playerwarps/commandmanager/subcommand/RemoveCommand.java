package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.page.ConfirmationMenu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.RemoveWarpAction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class RemoveCommand implements SubCommand {
    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Deletes existing pwarp";
    }

    @Override
    public String getSyntax() {
        return "/pwarp remove [warpName]";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.REMOVE_WARP;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        return null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Lang.BAD_COMMAND_SYNTAX.asColoredString().replace("%syntax%", getSyntax()));
            return;
        }

        final Optional<Warp> warp = PlayerWarpsPlugin.getWarpHandler().getWarpFromName(args[0]);
        if (warp.isEmpty()) {
            sender.sendMessage(Lang.NON_EXISTING_WARP.asColoredString());
            return;
        }

        if (sender instanceof Player) {
            new ConfirmationMenu(warp.get())
                    .open((Player) sender, new RemoveWarpAction());
        } else new RemoveWarpAction().execute(sender, warp.get());
    }
}