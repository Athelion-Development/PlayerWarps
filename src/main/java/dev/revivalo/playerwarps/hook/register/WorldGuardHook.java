package dev.revivalo.playerwarps.hook.register;

import com.sk89q.worldguard.WorldGuard;
import dev.revivalo.playerwarps.hook.Hook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldGuardHook implements Hook<WorldGuard> {
    private boolean isHooked;

    @Override
    public @NotNull String getName() {
        return "";
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
    public @Nullable WorldGuard getApi() {
        return WorldGuard.getInstance();
    }
}
