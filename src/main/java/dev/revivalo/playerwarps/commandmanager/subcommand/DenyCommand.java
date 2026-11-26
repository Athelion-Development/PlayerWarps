package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpApproval;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DenyCommand implements SubCommand {
    @Override
    public String getName() {
        return "deny";
    }

    @Override
    public String getDescription() {
        return "Denies and removes a warp waiting for approval";
    }

    @Override
    public String getSyntax() {
        return "/pwarp deny [warpName] (reason)";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.VERIFY;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        return index == 0 ? ApproveCommand.pendingWarpNames() : Collections.emptyList();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Lang.BAD_COMMAND_SYNTAX.asColoredString().replace("%syntax%", getSyntax()));
            return;
        }

        final Optional<Warp> warp = PlayerWarpsPlugin.getWarpHandler().getWarpFromName(args[0]);
        if (warp.isEmpty()) {
            sender.sendMessage(Lang.NON_EXISTING_WARP.asColoredString());
            return;
        }

        final String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        WarpApproval.deny(sender, warp.get(), reason);
    }
}
