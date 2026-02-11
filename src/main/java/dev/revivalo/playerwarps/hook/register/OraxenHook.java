package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenHook implements Hook<Void> {

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

    public ItemStack getCustomItem(String name) {
        return OraxenItems.exists(name)
                ? OraxenItems.getItemById(name).build()
                : null;
    }

    @Override
    public @Nullable Void getApi() {
        return null;
    }
}
