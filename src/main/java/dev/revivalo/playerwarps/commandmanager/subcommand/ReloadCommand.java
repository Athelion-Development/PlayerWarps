package dev.revivalo.playerwarps.commandmanager.subcommand;

import com.tchristofferson.configupdater.ConfigUpdater;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.input.InputMode;
import dev.revivalo.playerwarps.input.PlayerInput;
import dev.revivalo.playerwarps.menu.MenuTemplate;
import dev.revivalo.playerwarps.menu.page.Menu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.WarpManager;
import dev.revivalo.playerwarps.warp.action.SetPreviewItemAction;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

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
        // Adds options introduced by a newer version, so it has to run before the values are read.
        final File configFile = new File(PlayerWarpsPlugin.get().getDataFolder(), "config.yml");
        try {
            ConfigUpdater.update(PlayerWarpsPlugin.get(), "config.yml", configFile, Collections.emptyList());
        } catch (IOException ex) {
            PlayerWarpsPlugin.get().getLogger().log(Level.WARNING, "Cannot update config.yml", ex);
        }

        Config.reload();
        CategoryManager.loadCategories();

        final WarpManager warpManager = PlayerWarpsPlugin.getWarpHandler();
        warpManager.reloadSettings();
        warpManager.refreshCategories();
        SetPreviewItemAction.reloadBannedItems();
        Menu.TEMPLATE_CACHE.values().forEach(MenuTemplate::reload);

        // The config may have been fixed, so let a bad input-mode be reported again.
        InputMode.resetWarning();

        sender.sendMessage(Lang.RELOAD_MESSAGE.asColoredString());
        sender.sendMessage("§8[§bPlayerWarps§8] §7Input mode: §f" + PlayerInput.getMode());
        // Hooks are only set up on startup.
        sender.sendMessage("§8[§bPlayerWarps§8] §7Changes to the hook options take effect after a restart.");
    }
}
