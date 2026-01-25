package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.page.ConfirmationMenu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.CreateWarpAction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class CreateCommand implements SubCommand {
    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Creates new pwarp";
    }

    @Override
    public String getSyntax() {
        return "/pwarp create [warpName]";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.CREATE_WARP;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        return null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("[PlayerWarps] Warps can create only player!");
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(Lang.BAD_COMMAND_SYNTAX.asColoredString().replace("%syntax%", getSyntax()));
            return;
        }

        final Player player = (Player) sender;

        new ConfirmationMenu(new Warp(new HashMap<>() {{
            put("name", args[0]);
        }})).open(player, new CreateWarpAction(args[0]));
    }
}
