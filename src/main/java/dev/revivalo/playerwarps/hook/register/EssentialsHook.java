package dev.revivalo.playerwarps.hook.register;

import com.earth2me.essentials.Essentials;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public class EssentialsHook extends Importable implements Hook<Essentials> {

    private Essentials essentials;
    private boolean isHooked;

    public void getWarps() {
        essentials.getWarps().getList();
    }

    @Override
    public @NotNull String getName() {
        return "Essentials";
    }

    @Override
    public void register() {
        isHooked = isPluginEnabled();
        if (isHooked)
            essentials = (Essentials) getPlugin();
    }

    @Override
    public boolean isOn() {
        return isHooked;
    }

    @Override
    public Config getConfigPath() {
        return Config.ESSENTIALS_HOOK_ENABLED;
    }

    @Override
    public @Nullable Essentials getApi() {
        return essentials;
    }

    @Override
    public void importWarps() {
        for (String warpName : essentials.getWarps().getList()) {
            PlayerWarpsPlugin.get().getLogger().log(Level.INFO, warpName);
        }
    }
}
