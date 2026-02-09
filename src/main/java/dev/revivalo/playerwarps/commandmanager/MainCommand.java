package dev.revivalo.playerwarps.commandmanager;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PlayerUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.PreTeleportToWarpAction;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public abstract class MainCommand implements TabExecutor {
    protected final Set<SubCommand> subCommands = new HashSet<>();

    protected final Lang noPermMessage;
    protected final ArgumentMatcher argumentMatcher;

    public MainCommand(Lang noPermissionMessage, ArgumentMatcher argumentMatcher) {
        this.noPermMessage = noPermissionMessage;
        this.argumentMatcher = argumentMatcher;

        registerSubCommands();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            perform(sender);
            return true;
        }

        SubCommand subCommand = subCommands.stream().filter(sc -> sc.getName().equalsIgnoreCase(args[0])).findAny().orElse(getDefaultSyntax());

        if (subCommand == null) {
            Optional<Warp> warpOptional = PlayerWarpsPlugin.getWarpHandler().getWarpFromName(args[0]);
            if (warpOptional.isEmpty()) {
                sender.sendMessage(Lang.NON_EXISTING_WARP.asColoredString());
                return true;
            }

            Player player;
            try {
                player = PlayerUtil.getPlayerFromName(args[1]);
            } catch (NullPointerException | IndexOutOfBoundsException ex) {
                player = (Player) sender;
            }

            new PreTeleportToWarpAction().proceed(player, warpOptional.get());
            return true;
        }

        if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission().asString())) {
            subCommand.perform(sender, Arrays.copyOfRange(args, 1, args.length));
        } else {
            sender.sendMessage(noPermMessage.asColoredString().replace("%permission%", subCommand.getPermission().asString()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 0) {
            return null;
        }

        if (args.length == 1) {
            List<String> subCommandsTC = subCommands.stream().filter(sc -> sc.getPermission() == null || sender.hasPermission(sc.getPermission().asString())).map(SubCommand::getName).collect(Collectors.toList());

            ArrayList<String> accessibleWarps = new ArrayList<>(PlayerWarpsPlugin.getWarpHandler().getWarps().stream()
                    .filter(Warp::isAccessible)
                    .map(Warp::getName)
                    .toList());

            subCommandsTC.addAll(accessibleWarps);

            return getMatchingStrings(subCommandsTC, args[args.length - 1], argumentMatcher);
        }

        SubCommand subCommand = subCommands.stream().filter(sc -> sc.getName().equalsIgnoreCase(args[0])).findAny().orElse(null);

        if (subCommand == null) {
            return null;
        }

        List<String> subCommandTB = subCommand.getTabCompletion(sender, args.length - 2, args);

        return getMatchingStrings(subCommandTB, args[args.length - 1], argumentMatcher);
    }

    private static List<String> getMatchingStrings(List<String> tabCompletions, String arg, ArgumentMatcher argumentMatcher) {
        if (tabCompletions == null || arg == null) {
            return null;
        }

        List<String> result = argumentMatcher.filter(tabCompletions, arg);

        Collections.shuffle(result);

        return result;
    }

    public void registerMainCommand(JavaPlugin main, String cmdName) {
        PluginCommand cmd = main.getCommand(cmdName);

        if (cmd == null) {
            PlayerWarpsPlugin.get().getLogger().info("Command " + cmdName + " isn't registered in plugin.yml!");
            return;
        }

        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
        //cmd.setPermissionMessage(noPermMessage);
    }

    protected abstract void registerSubCommands();

    protected abstract void perform(CommandSender sender);

    protected SubCommand getDefaultSyntax() {
        return subCommands.stream().filter(sc -> sc.getName().equalsIgnoreCase("default")).findAny().orElse(null);
    }

    @SuppressWarnings("unused")
    public Set<SubCommand> getSubCommands() {
        return new HashSet<>(subCommands);
    }
}