package dev.revivalo.playerwarps.hook.register;

import dev.lone.itemsadder.api.CustomStack;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.CustomItemHook;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ItemsAdder registers its items asynchronously some time after the server has started (and again
 * on every /iazip or /iareload), so items are always looked up when they are drawn, never cached.
 */
public class ItemsAdderHook implements CustomItemHook {
    private boolean isHooked;

    @Override
    public @NotNull String getName() {
        return "ItemsAdder";
    }

    @Override
    public void register() {
        isHooked = isPluginEnabled();
    }

    @Override
    public boolean isOn() {
        return isHooked;
    }

    @Override
    public Config getConfigPath() {
        return Config.ITEMS_ADDER_HOOK_ENABLED;
    }

    @Override
    public @Nullable ItemStack getCustomItem(@NotNull String id) {
        return CustomStack.isInRegistry(id)
                ? CustomStack.getInstance(id).getItemStack()
                : null;
    }

    @Override
    public @Nullable String getCustomItemId(@NotNull ItemStack item) {
        final CustomStack customStack = CustomStack.byItemStack(item);
        return customStack != null ? customStack.getNamespacedID() : null;
    }
}
