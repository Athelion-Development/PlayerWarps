package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import dev.revivalo.playerwarps.warp.Warp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Squaremap;

import java.util.logging.Level;

/**
 * Must not touch the squaremap API itself - this class is loaded even when squaremap is not
 * installed. Everything API related lives in {@link SquaremapMarkers}, which is loaded only
 * after squaremap is confirmed to be enabled.
 */
public class SquaremapHook implements Hook<Squaremap> {
    private SquaremapMarkers markers = null;

    @Override
    public @NotNull String getName() {
        return "squaremap";
    }

    @Override
    public void register() {
        if (!isPluginEnabled()) return;

        try {
            this.markers = new SquaremapMarkers();
        } catch (IllegalStateException ex) {
            PlayerWarpsPlugin.get().getLogger().log(Level.WARNING, "Cannot obtain the squaremap API.", ex);
        }
    }

    @Override
    public boolean isOn() {
        return markers != null;
    }

    @Override
    public Config getConfigPath() {
        return Config.SQUAREMAP_HOOK_ENABLED;
    }

    @Override
    public @Nullable Squaremap getApi() {
        return markers == null ? null : markers.getApi();
    }

    public void setMarker(Warp warp) {
        if (!isOn()) return;
        markers.setMarker(warp);
    }

    public void removeMarker(Warp warp) {
        if (!isOn()) return;
        markers.removeMarker(warp);
    }
}
