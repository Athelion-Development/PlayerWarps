package dev.revivalo.playerwarps;

import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.commandmanager.command.PwarpMainCommand;
import dev.revivalo.playerwarps.configuration.Data;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.menu.MenuRegister;
import dev.revivalo.playerwarps.updatechecker.UpdateChecker;
import dev.revivalo.playerwarps.updatechecker.UpdateNotificator;
import dev.revivalo.playerwarps.user.UserHandler;
import dev.revivalo.playerwarps.util.VersionUtil;
import dev.revivalo.playerwarps.warp.WarpManager;
import io.github.g00fy2.versioncompare.Version;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

public final class PlayerWarpsPlugin extends JavaPlugin {
    private static PlayerWarpsPlugin plugin;
    private static WarpManager warpHandler;
    private static Data data;
    private static String latestVersion;

    @Override
    public void onEnable() {
        setPlugin(this);

        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String[] languages = {
                "English", "Czech", "Chinese", "French",
                "German", "Polish", "Russian", "Turkish",
                "Portuguese", "Spanish"
        };

        for (String language : languages) {
            copyResource("lang/" + language + ".yml");
        }

        Config.reload();

        new Metrics(this, 12061);
        new UserHandler(this);

        HookRegister.hook();

        if (Config.UPDATE_CHECKER.asBoolean()) {
            new UpdateChecker(this, 79089).getVersion(pluginVersion -> {
                setLatestVersion(pluginVersion);

                final String actualVersion = getDescription().getVersion();
                Version version = new Version(pluginVersion);
                final boolean isNewerVersion = version.isHigherThan(actualVersion);
                final boolean isDevelopmentBuild = version.isLowerThan(actualVersion);

                if (isDevelopmentBuild) {
                    getLogger().info(String.format("You are running a development build (%s).", actualVersion));
                } else {

                    if (isNewerVersion) {
                        getLogger().info(String.format("There is a new v%s update available (You are running v%s).\n" +
                                "Outdated versions are no longer supported, get the latest one here: " +
                                "https://bit.ly/revivalo-playerwarps", pluginVersion, actualVersion));
                    } else {
                        getLogger().info(String.format("You are running the latest release (%s).", pluginVersion));
                    }
                }
                VersionUtil.setLatestVersion(!isNewerVersion);
            });
        }

        CategoryManager.loadCategories();

        setWarpHandler(new WarpManager());
        setDataManager(new Data());

        registerCommands();

        warpHandler.loadWarps();

        if (Config.AUTOSAVE_ENABLED.asBoolean()) {
            long intervalInTicks = (Config.AUTOSAVE_INTERVAL.asLong() * 60) * 20;
            new BukkitRunnable() {
                @Override
                public void run() {
                    warpHandler.saveWarps();
                }
            }.runTaskTimerAsynchronously(this, intervalInTicks, intervalInTicks);
        }

        MenuRegister.registerFiles();

        new UpdateNotificator();
    }

    @Override
    public void onDisable() {warpHandler.saveWarps();}

    private void registerCommands(){
        new PwarpMainCommand().registerMainCommand(this, "pwarp");
    }

    public void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private void copyResource(String resourcePath) {
        File outFile = new File(getDataFolder(), resourcePath);
        if (!outFile.exists()) {
            try (InputStream in = getResource(resourcePath)) {
                if (in == null) {
                    getLogger().log(Level.SEVERE, "Resource " + resourcePath + " not found in the plugin JAR!");
                    return;
                }
                outFile.getParentFile().mkdirs();
                Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                getLogger().log(Level.INFO, "Resource " + resourcePath + " successfully copied.");
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Failed to copy resource " + resourcePath, e);
            }
        }
    }

    public void executeCommandAsConsole(String command) {
        getServer().dispatchCommand(getServer().getConsoleSender(), command);
    }

    @Nullable
    public Plugin getPlugin(String pluginName) {
        return getServer().getPluginManager().getPlugin(pluginName);
    }

    public boolean isPluginLoaded(String pluginName) {
        return getPlugin(pluginName) != null;
    }

    public boolean isPluginEnabled(String pluginName) {
        return getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    public BukkitScheduler getScheduler() {
        return getServer().getScheduler();
    }

    public void runAsync(Runnable runnable) {
        getScheduler().runTaskAsynchronously(this, runnable);
    }

    public void runSync(Runnable runnable) {
        getScheduler().runTask(this, runnable);
    }

    public void runDelayed(Runnable runnable, long delay) {
        getScheduler().runTaskLater(this, runnable, delay);
    }

    public BukkitTask runRepeating(Runnable runnable, long delay, long period) {
        return getScheduler().runTaskTimer(this, runnable, delay, period);
    }

    public <T> CompletableFuture<T> completableFuture(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runAsync(() -> {
            try {
                future.complete(callable.call());
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new CompletionException(e);
            }
        });
        return future;
    }

    public static PlayerWarpsPlugin get(){
        return PlayerWarpsPlugin.plugin;
    }

    public static WarpManager getWarpHandler() {
        return warpHandler;
    }

    public static Data getData() {
        return data;
    }

    public static void setWarpHandler(WarpManager warpHandler) {
        PlayerWarpsPlugin.warpHandler = warpHandler;
    }

    public static void setDataManager(Data data) {
        PlayerWarpsPlugin.data = data;
    }

    public static void setPlugin(PlayerWarpsPlugin plugin) {
        PlayerWarpsPlugin.plugin = plugin;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static void setLatestVersion(String latestVersion) {
        PlayerWarpsPlugin.latestVersion = latestVersion;
    }
}
