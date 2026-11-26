package dev.revivalo.playerwarps.hook;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A hook into a plugin providing custom (resource pack) items, such as ItemsAdder, Nexo or Oraxen.
 */
public interface CustomItemHook extends Hook<Void> {
    /**
     * @return a new stack of the custom item with the given ID, or null if this plugin does not know it
     */
    @Nullable ItemStack getCustomItem(@NotNull String id);

    /**
     * @return the ID of the custom item the stack is an instance of, or null if it is not one of this plugin's items
     */
    @Nullable String getCustomItemId(@NotNull ItemStack item);

    @Override
    default @Nullable Void getApi() {
        return null;
    }
}
