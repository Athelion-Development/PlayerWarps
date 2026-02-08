package dev.revivalo.playerwarps.hook.register;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlueMapHook implements Hook<BlueMapAPI> {
    private BlueMapAPI blueMapAPI = null;
    private MarkerSet markerSet;

    @Override
    public @NotNull String getName() {
        return "BlueMap";
    }

    @Override
    public void register() {
        if (isPluginEnabled()) {
            initialize();
        }
    }

    private void initialize() {
        BlueMapAPI.onEnable(api -> {
            this.blueMapAPI = api;
            this.markerSet = MarkerSet.builder()
                    .label("Player Warps")
                    .build();
            PlayerWarpsPlugin.getWarpHandler().createMarks();
        });
    }

    @Override
    public boolean isOn() {
        return blueMapAPI != null;
    }

    @Override
    public Config getConfigPath() {
        return Config.BLUEMAP_HOOK_ENABLED;
    }

    @Override
    public @Nullable BlueMapAPI getApi() {
        return blueMapAPI;
    }

    public void setMarker(Warp warp) {
        if (isOn()) {
            String markerLabel = Config.DYNMAP_MARKER_LABEL.asString()
                    .replace("%warp%", warp.getName())
                    .replace("%owner%", warp.getOwnerName());
            String markerId = warp.getWarpID().toString();
            Location warpLocation = warp.getLocation();
            POIMarker marker = POIMarker.builder()
                    .label(markerLabel)
                    .position(warpLocation.getX(), warpLocation.getY(), warpLocation.getZ())
                    .build();

            markerSet.getMarkers().put(markerId, marker);

            blueMapAPI.getWorld(warp.getLocation().getWorld()).ifPresent(world -> {
                for (BlueMapMap map : world.getMaps()) {
                    map.getMarkerSets().put("playerwarpmarkers", markerSet);
                }
            });
        }
    }

    public void removeMarker(Warp warp) {
        if (isOn()) {
            markerSet.getMarkers().remove(warp.getWarpID().toString());
        }
    }
}