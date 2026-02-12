package dev.revivalo.playerwarps.hook.register;

import com.palmergames.bukkit.towny.TownyAPI;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TownyAdvancedHook implements Hook<TownyAPI> {
    private TownyAPI townyAPI;
    private boolean isHooked;

    @Override
    public @NotNull String getName() {
        return "Towny";
    }

    @Override
    public void register() {
        isHooked = isPluginEnabled();
        if (isHooked) {
            townyAPI = TownyAPI.getInstance();
        }
    }

    @Override
    public boolean isOn() {
        return isHooked;
    }

    @Override
    public Config getConfigPath() {
        return Config.GRIEF_PREVENTION_HOOK_ENABLED;
    }

    @Override
    public @Nullable TownyAPI getApi() {
        return townyAPI;
    }
}
