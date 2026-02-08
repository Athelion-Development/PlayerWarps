package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GriefPreventionHook implements Hook<GriefPrevention> {
    private GriefPrevention griefPrevention;
    private boolean isHooked;

    @Override
    public @NotNull String getName() {
        return "GriefPrevention";
    }

    @Override
    public void register() {
        isHooked = isPluginEnabled();
        if (isHooked) {
            griefPrevention = GriefPrevention.instance;
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
    public @Nullable GriefPrevention getApi() {
        return griefPrevention;
    }
}
