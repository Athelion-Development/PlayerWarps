package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import org.jetbrains.annotations.NotNull;
import world.bentobox.bentobox.BentoBox;

public class BentoBoxHook implements Hook<BentoBox> {

    private boolean isHooked;
    private BentoBox bentoBox;

    @Override
    public @NotNull String getName() {
        return "BentoBox";
    }

    @Override
    public void register() {
        isHooked = isPluginEnabled();
        if (isHooked)
            bentoBox = BentoBox.getInstance();
    }

    @Override
    public boolean isOn() {
        return isHooked;
    }

    @Override
    public Config getConfigPath() {
        return Config.BENTOBOX_HOOK_ENABLED;
    }

    @Override
    public BentoBox getApi() {
        return bentoBox;
    }
}
