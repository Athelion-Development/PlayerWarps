package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.menu.MenuTemplate;
import dev.revivalo.playerwarps.menu.page.Menu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand implements SubCommand {
    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reloads a plugin's configuration";
    }

    @Override
    public String getSyntax() {
        return "/pwarp reload";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.RELOAD_PLUGIN;
    }

    @Override
    public List<String> getTabCompletion(CommandSender commandSender, int index, String[] args) {
        return null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        PlayerWarpsPlugin.getWarpHandler().reloadWarps(sender);
        Menu.TEMPLATE_CACHE.values().forEach(MenuTemplate::reload);
    }
}
