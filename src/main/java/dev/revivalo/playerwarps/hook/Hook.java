package dev.revivalo.playerwarps.hook;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public interface Hook<T> {
    default void preRegister() {
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

    @Nullable
    T getApi();
}
