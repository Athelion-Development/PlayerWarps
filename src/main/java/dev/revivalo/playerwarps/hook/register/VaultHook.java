package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VaultHook implements Hook<Economy> {
    private Economy economy;
    private boolean isHooked;

    @Override
    public @NotNull String getName() {
        return "Vault";
    }

    @Override
    public void register() {
        if (!isPluginEnabled()) {
            isHooked = false;
            return;
        }

        RegisteredServiceProvider<Economy> rsp = PlayerWarpsPlugin.get().getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            isHooked = false;
            return;
        }

        economy = rsp.getProvider();
        isHooked = true;
    }

    @Override
    public boolean isOn() {
        return isHooked;
    }

    @Override
    public Config getConfigPath() {
        return null;
    }

    @Override
    public @Nullable Economy getApi() {
        return isHooked ? economy : null;
    }
}
