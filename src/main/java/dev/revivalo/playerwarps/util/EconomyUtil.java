package dev.revivalo.playerwarps.util;

import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.VaultHook;
import org.bukkit.OfflinePlayer;

/**
 * Money handling that works with and without Vault. Without an economy every price is free,
 * so nothing is checked, taken or paid out.
 */
public final class EconomyUtil {
    private EconomyUtil() {
        throw new RuntimeException("This class cannot be instantiated");
    }

    public static boolean isEnabled() {
        return HookRegister.mapIfEnabled(VaultHook.class, hook -> true, false);
    }

    public static boolean has(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }

        return HookRegister.mapIfEnabled(VaultHook.class, hook -> hook.has(player, amount), true);
    }

    /**
     * @return true when the amount was taken from the player, or when there is nothing to pay
     */
    public static boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }

        return HookRegister.mapIfEnabled(VaultHook.class, hook -> hook.withdraw(player, amount), true);
    }

    public static void deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return;
        }

        HookRegister.ifEnabled(VaultHook.class, hook -> hook.deposit(player, amount));
    }
}
