package dev.revivalo.playerwarps.hook;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public interface Hook<T> {
    default void preRegister() {
        if (getConfigPath() != null && !getConfigPath().asBoolean()) {
            return;
        }

        register();
        if (isOn()) {
            PlayerWarpsPlugin.get().getLogger().log(Level.INFO, this.getClass().getSimpleName() + " has been registered.");
        }
    }

    @NotNull String getName();

    @NotNull
    default String getVersion() {
        Plugin plugin = getPlugin();
        if (plugin == null) return "Unknown";
        return plugin.getDescription().getVersion();
    }

    default boolean isPluginEnabled() {
        return PlayerWarpsPlugin.get().isPluginEnabled(getName());
    }

    default Plugin getPlugin() {
        return PlayerWarpsPlugin.get().getPlugin(getName());
    }

    void register();

    boolean isOn();

    Config getConfigPath();

    @Nullable
    T getApi();
}
