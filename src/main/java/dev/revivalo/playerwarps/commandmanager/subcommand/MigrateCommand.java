package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.storage.SqlWarpStorage;
import dev.revivalo.playerwarps.storage.YamlWarpStorage;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.WarpManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

public class MigrateCommand implements SubCommand {
    private static final String PREFIX = "§8[§bPlayerWarps§8] §7";
    private static final String LOCAL_TO_DB = "local-to-db";
    private static final String DB_TO_LOCAL = "db-to-local";

    @Override
    public String getName() {
        return "migrate";
    }

    @Override
    public String getDescription() {
        return "Moves warps between data.yml and the database";
    }

    @Override
    public String getSyntax() {
        return "/pwarp migrate <" + LOCAL_TO_DB + "|" + DB_TO_LOCAL + ">";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.ADMIN;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        return index == 0 ? List.of(LOCAL_TO_DB, DB_TO_LOCAL) : null;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Lang.BAD_COMMAND_SYNTAX.asColoredString().replace("%syntax%", getSyntax()));
            return;
        }

        final WarpManager warpManager = PlayerWarpsPlugin.getWarpHandler();
        if (!(warpManager.getStorage() instanceof SqlWarpStorage storage)) {
            sender.sendMessage(PREFIX + "Warps are stored in data.yml. Set database.type in config.yml to "
                    + "SQLITE, MYSQL, MARIADB or POSTGRESQL and restart the server first.");
            return;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case LOCAL_TO_DB -> storage.importLocalWarps(warpManager, sender);
            case DB_TO_LOCAL -> {
                if (!storage.isLoaded()) {
                    sender.sendMessage(PREFIX + "The database is not connected, see the console.");
                    return;
                }

                if (!YamlWarpStorage.export(warpManager.getWarps())) {
                    sender.sendMessage(PREFIX + "§cdata.yml cannot be read, so it was not overwritten. Fix or remove it and try again.");
                    return;
                }

                sender.sendMessage(PREFIX + "Wrote " + warpManager.getWarps().size() + " warps to data.yml (the previous file is kept as data.yml.bak).");
                sender.sendMessage(PREFIX + "To use it, set database.type to YAML in config.yml and restart the server.");
            }
            default -> sender.sendMessage(Lang.BAD_COMMAND_SYNTAX.asColoredString().replace("%syntax%", getSyntax()));
        }
    }
}
