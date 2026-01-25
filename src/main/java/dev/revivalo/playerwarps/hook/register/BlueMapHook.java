package dev.revivalo.playerwarps.hook.register;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlueMapHook implements Hook<Void> {
    private BlueMapAPI blueMapAPI = null;
    private MarkerSet markerSet;

    @Override
    public @NotNull String getName() {
        return "BlueMap";
    }

    @Override
    public void register() {
        this.blueMapAPI = (getPlugin() != null
                ? BlueMapAPI.getInstance().isPresent()
                    ? BlueMapAPI.getInstance().get()
                    : null
                : null);
        if (isOn()) {
             markerSet = MarkerSet.builder()
                    .label("PlayerWarp's Markers")
                    .build();
        }
    }

    @Override
    public boolean isOn() {
        return false;
    }

    @Override
    public @Nullable Void getApi() {
        return null;
    }

    public void setMarker(Warp warp) {
        if (isOn()) {
            String markerLabel = Config.DYNMAP_MARKER_LABEL.asString()
                    .replace("%warp%", warp.getName())
                    .replace("%owner%", Bukkit.getOfflinePlayer(warp.getOwner()).getName());
            String markerId = warp.getWarpID().toString();
            POIMarker marker = POIMarker.builder()
                    .label(markerLabel)
                    .position(20.0, 65.0, -23.0)
                    .maxDistance(1000)
                    .build();

            markerSet.getMarkers()
                    .put(markerId, marker);

            blueMapAPI.getWorld(warp.getLocation().getWorld()).ifPresent(world -> {
                for (BlueMapMap map : world.getMaps()) {
                    map.getMarkerSets().put("playerwarpmarkers", markerSet);
                }
            });
        }
    }

    public void removeMarker(Warp warp) {
        if (isOn()) {
            String markerId = warp.getWarpID().toString();
            markerSet.remove(markerId);
        }
    }
}