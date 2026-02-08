package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SuperiorSkyBlockHook implements Hook<Void> {
    private boolean isHooked;

    @Override
    public @NotNull String getName() {
        return "SuperiorSkyblock2";
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
        return Config.SUPERIOR_SKY_BLOCK_HOOK_ENABLED;
    }

    @Nullable
    @Override
    public Void getApi() {
        return null;
    }
}