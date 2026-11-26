package dev.revivalo.playerwarps.hook.register;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.CustomItemHook;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NexoHook implements CustomItemHook {

    private boolean isHooked;

    @Override
    public @NotNull String getName() {
        return "Nexo";
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
        return Config.NEXO_HOOK_ENABLED;
    }

    @Override
    public @Nullable ItemStack getCustomItem(@NotNull String id) {
        final ItemBuilder builder = NexoItems.itemFromId(id);
        return builder != null ? builder.build() : null;
    }

    @Override
    public @Nullable String getCustomItemId(@NotNull ItemStack item) {
        return NexoItems.idFromItem(item);
    }
}
