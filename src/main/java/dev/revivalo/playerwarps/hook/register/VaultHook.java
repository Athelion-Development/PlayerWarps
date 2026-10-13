package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Vault is optional. Every Vault class is only touched from this hook, so the plugin loads
 * without it - use {@link dev.revivalo.playerwarps.util.EconomyUtil} instead of the API directly.
 */
public class VaultHook implements Hook<Economy> {
    private volatile Economy economy;
    private boolean registered;

    @Override
    public @NotNull String getName() {
        return "Vault";
    }

    @Override
    public void register() {
        registered = true;

        if (resolveEconomy() == null && isPluginEnabled()) {
            // Economy plugins that enable after PlayerWarps register their provider later,
            // so it is looked up again on first use. Report it once the server has started.
            PlayerWarpsPlugin.get().runSync(() -> {
                if (resolveEconomy() == null) {
                    PlayerWarpsPlugin.get().getLogger().warning(
                            "Vault is installed, but no economy plugin is registered with it. All prices and fees are disabled.");
                }
            });
        }
    }

    /**
     * The provider is cached once found. Until then it is looked up on every call, because the
     * economy plugin may register itself after PlayerWarps has been enabled.
     */
    @Nullable
    private Economy resolveEconomy() {
        if (economy != null) {
            return economy;
        }

        if (!registered || !isPluginEnabled()) {
            return null;
        }

        final RegisteredServiceProvider<Economy> rsp = PlayerWarpsPlugin.get().getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return null;
        }

        economy = rsp.getProvider();
        PlayerWarpsPlugin.get().getLogger().info("Using " + economy.getName() + " as the economy provider.");
        return economy;
    }

    @Override
    public boolean isOn() {
        return resolveEconomy() != null;
    }

    @Override
    public Config getConfigPath() {
        return Config.VAULT_HOOK_ENABLED;
    }

    @Override
    public @Nullable Economy getApi() {
        return resolveEconomy();
    }

    public boolean has(OfflinePlayer player, double amount) {
        final Economy api = resolveEconomy();
        return api == null || api.has(player, amount);
    }

    /**
     * @return true only when the money has actually been taken from the player
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        final Economy api = resolveEconomy();
        return api != null && api.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        final Economy api = resolveEconomy();
        return api != null && api.depositPlayer(player, amount).transactionSuccess();
    }
}
