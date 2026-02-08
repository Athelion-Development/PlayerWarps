package dev.revivalo.playerwarps.hook.register;

import com.bekvon.bukkit.residence.Residence;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResidenceHook implements Hook<Residence> {
    private Residence residence;
    private boolean isHooked;

    @Override
    public @NotNull String getName() {
        return "Residence";
    }

    @Override
    public void register() {
        isHooked = isPluginEnabled();
        if (isHooked)
            residence = (Residence) getPlugin();
    }

    @Override
    public boolean isOn() {
        return isHooked;
    }

    @Override
    public Config getConfigPath() {
        return Config.RESIDENCE_HOOK_ENABLED;
    }

    @Nullable
    @Override
    public Residence getApi() {
        return residence;
    }
}
