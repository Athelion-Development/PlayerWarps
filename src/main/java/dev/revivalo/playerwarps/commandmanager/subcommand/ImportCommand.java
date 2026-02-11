package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.util.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ImportCommand implements SubCommand {
    @Override
    public String getName() {
        return "import";
    }

    @Override
    public String getDescription() {
        return "Imports the input mode";
    }

    @Override
    public String getSyntax() {
        return "/pwarp import";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.ADMIN;
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

        //HookRegister.getEssentialsHook().importWarps();
    }
}
