package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import me.angeschossen.lands.api.LandsIntegration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AngeschossenLandsHook implements Hook<LandsIntegration> {
    private LandsIntegration landsIntegration = null;

    @Override
    public @NotNull String getName() {
        return "Lands";
    }

    @Override
    public void register() {
        if (isPluginEnabled()) {
            landsIntegration = LandsIntegration.of(PlayerWarpsPlugin.get());
        }
    }

    @Override
    public boolean isOn() {
        return landsIntegration != null;
    }

    @Override
    public Config getConfigPath() {
        return Config.ANGESCHOSSEN_LANDS_HOOK_ENABLED;
    }

    @Override
    public @Nullable LandsIntegration getApi() {
        return landsIntegration;
    }
}
