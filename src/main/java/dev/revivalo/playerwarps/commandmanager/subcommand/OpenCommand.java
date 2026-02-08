package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.menu.page.WarpsMenu;
import dev.revivalo.playerwarps.user.DataSelectorType;
import dev.revivalo.playerwarps.user.User;
import dev.revivalo.playerwarps.user.UserHandler;
import dev.revivalo.playerwarps.util.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class OpenCommand implements SubCommand {
    @Override
    public String getName() {
        return "open";
    }

    @Override
    public String getDescription() {
        return "Opens specified menu";
    }

    @Override
    public String getSyntax() {
        return "/pwarp open <menu>";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.OPEN;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        return null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[PlayerWarps] Warps can create only player!");
            return;
        }

        if (args.length < 1) {
            return;
        }

        if (args[0].equalsIgnoreCase("my_warps")) {
            new WarpsMenu.MyWarpsMenu().openFor(player);
        } else if (args[0].equalsIgnoreCase("warps")) {
            final User user = UserHandler.getUser(player);
            new WarpsMenu.DefaultWarpsMenu()
                    .open(player.getPlayer(), (String) user.getData(DataSelectorType.SELECTED_CATEGORY), PlayerWarpsPlugin.getWarpHandler().getSortingManager().getDefaultSortType());
        } else if (args[0].equalsIgnoreCase("saved_warps")) {
            new WarpsMenu.FavoriteWarpsMenu().openFor(player);
        } else if (args[0].equalsIgnoreCase("category")) {
            try {
                String categoryName = args[1];
                CategoryManager.getCategory(categoryName).ifPresent(
                        category -> getDescription()
                );
            } catch (IndexOutOfBoundsException ex) {

            }
        }
    }
}
