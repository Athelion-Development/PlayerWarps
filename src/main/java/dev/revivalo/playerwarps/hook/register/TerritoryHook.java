package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TerritoryHook implements Hook<Void> {
    private boolean isHooked;

    @Override
    public @NotNull String getName() {
        return "Territory";
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
        return Config.TERRITORY_HOOK_ENABLED;
    }

    @Override
    public @Nullable Void getApi() {
        return null;
    }
}
