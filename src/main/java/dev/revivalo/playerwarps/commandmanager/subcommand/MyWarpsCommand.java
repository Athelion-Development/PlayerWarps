package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.menu.page.WarpsMenu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class MyWarpsCommand implements SubCommand {
    @Override
    public String getName() {
        return "mywarps";
    }

    @Override
    public String getDescription() {
        return "Opens the menu with warps owned by the player";
    }

    @Override
    public String getSyntax() {
        return "/pwarp mywarps";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.MY_WARPS;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[PlayerWarps] You can open the menu with your warps only as a player!");
            return;
        }

        new WarpsMenu.MyWarpsMenu().openFor(player);
    }
}
