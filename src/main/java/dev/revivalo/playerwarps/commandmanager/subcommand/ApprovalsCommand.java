package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.page.WarpsMenu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpApproval;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Opens the menu with warps waiting for approval. From the console it lists them instead.
 */
public class ApprovalsCommand implements SubCommand {
    @Override
    public String getName() {
        return "approvals";
    }

    @Override
    public String getDescription() {
        return "Opens the menu with warps waiting for approval";
    }

    @Override
    public String getSyntax() {
        return "/pwarp approvals";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.VERIFY;
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, int index, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        final List<Warp> pending = WarpApproval.getPendingWarps();
        if (pending.isEmpty()) {
            sender.sendMessage(Lang.NO_WARPS_AWAITING_APPROVAL.asColoredString());
            return;
        }

        if (sender instanceof Player player) {
            new WarpsMenu.ApprovalsMenu().openFor(player);
            return;
        }

        sender.sendMessage("[PlayerWarps] Warps waiting for approval (" + pending.size() + "):");
        for (Warp warp : pending) {
            sender.sendMessage(" - " + warp.getName() + " by " + warp.getOwnerName());
        }
    }
}
