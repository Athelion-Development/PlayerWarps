package dev.revivalo.playerwarps.hook.register;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.ItemsAdder;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.CustomItemHook;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * ItemsAdder registers its items asynchronously some time after the server has started (and again
 * on every /iazip or /iareload), so items are always looked up when they are drawn, never cached.
 */
public class ItemsAdderHook implements CustomItemHook, Listener {
    private boolean isHooked;
    // ItemsAdder 5 throws from CustomStack until its registry is built, so items are only looked up once the data is loaded
    private boolean isDataLoaded;

    @Override
    public @NotNull String getName() {
        return "ItemsAdder";
    }

    @Override
    public void register() {
        isHooked = isPluginEnabled();
        if (isHooked) {
            isDataLoaded = areItemsLoaded();
            PlayerWarpsPlugin.get().registerListeners(this);
        }
    }

    @Override
    public boolean isOn() {
        return isHooked;
    }

    @Override
    public Config getConfigPath() {
        return Config.ITEMS_ADDER_HOOK_ENABLED;
    }

    /**
     * Returns null when the item does not exist or ItemsAdder has not loaded its items yet.
     */
    @Override
    public @Nullable ItemStack getCustomItem(@NotNull String id) {
        if (!isHooked || !isDataLoaded) {
            return null;
        }

        try {
            if (!CustomStack.isInRegistry(id)) {
                return null;
            }

            final CustomStack customStack = CustomStack.getInstance(id);
            return customStack != null ? customStack.getItemStack() : null;
        } catch (RuntimeException e) {
            PlayerWarpsPlugin.get().getLogger().log(Level.WARNING, "Failed to look up ItemsAdder item '" + id + "'", e);
            return null;
        }
    }

    @Override
    public @Nullable String getCustomItemId(@NotNull ItemStack item) {
        if (!isHooked || !isDataLoaded) {
            return null;
        }

        try {
            final CustomStack customStack = CustomStack.byItemStack(item);
            return customStack != null ? customStack.getNamespacedID() : null;
        } catch (RuntimeException e) {
            PlayerWarpsPlugin.get().getLogger().log(Level.WARNING, "Failed to look up the ItemsAdder ID of " + item.getType(), e);
            return null;
        }
    }

    private boolean areItemsLoaded() {
        try {
            return ItemsAdder.areItemsLoaded();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @EventHandler
    public void onItemsAdderLoad(final ItemsAdderLoadDataEvent event) {
        isDataLoaded = true;
    }
}
