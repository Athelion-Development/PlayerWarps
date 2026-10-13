package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.CustomItemHook;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenHook implements CustomItemHook {

    private boolean isHooked;

    @Override
    public @NotNull String getName() {
        return "Oraxen";
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
        return Config.ORAXEN_HOOK_ENABLED;
    }

    @Override
    public @Nullable ItemStack getCustomItem(@NotNull String id) {
        return OraxenItems.exists(id)
                ? OraxenItems.getItemById(id).build()
                : null;
    }

    @Override
    public @Nullable String getCustomItemId(@NotNull ItemStack item) {
        return OraxenItems.getIdByItem(item);
    }
}
