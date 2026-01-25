package dev.revivalo.playerwarps.commandmanager.subcommand;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.commandmanager.SubCommand;
import dev.revivalo.playerwarps.hook.Hook;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.util.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AboutSubCommand implements SubCommand {
    @Override
    public @NotNull String getName() {
        return "about";
    }

    @Override
    public @NotNull String getDescription() {
        return "Shows information about plugin";
    }

    @Override
    public @NotNull String getSyntax() {
        return "/pwarp about";
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.ABOUT;
    }

    @Override
    public List<String> getTabCompletion(@NotNull CommandSender sender, int index, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void perform(@NotNull CommandSender sender, String[] args) {
        PlayerWarpsPlugin plugin = PlayerWarpsPlugin.get();
        plugin.runAsync(() -> {
            sender.sendMessage(
                    "Developers: " + String.join(", ", plugin.getDescription().getAuthors() )+ "\n" +
                            "Version: " + plugin.getDescription().getVersion() + "\n" +
                            "Wiki: https://playerwarps.athelion.eu/\n" +
                            "Support: https://discord.athelion.eu/\n" +
                            "Platform: " + plugin.getServer().getName() + " " + plugin.getServer().getVersion()
            );
            Collection<Hook<?>> hooks = HookRegister.getHooks();
            if (!hooks.isEmpty()) {
                sender.sendMessage("Hooks: " + (hooks.stream().noneMatch(Hook::isOn) ? "None" : ""));
                hooks.stream().filter(Hook::isOn).forEach(hook -> sender.sendMessage(" " + hook.getName() + " - " + hook.getVersion()));
            }
        });
    }
}
