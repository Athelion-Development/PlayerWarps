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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlueMapHook implements Hook<BlueMapAPI> {
    private static final String MARKER_SET_ID = "playerwarpmarkers";

    private volatile BlueMapAPI blueMapAPI = null;

    // One set per world. A set shared by all worlds would show every warp on every map.
    private final Map<String, MarkerSet> markerSets = new ConcurrentHashMap<>();

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
            // BlueMap forgets the sets on a reload, so they are built again.
            this.markerSets.clear();

            createMarks();
        });
    }

    private void createMarks() {
        for (Warp warp : PlayerWarpsPlugin.getWarpHandler().getWarps()) {
            setMarker(warp);
        }
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

    /**
     * Shows the warp on the map, replacing the marker it had before - its location, name or
     * owner may have changed. A warp that is not accessible is taken off the map.
     */
    public void setMarker(Warp warp) {
        if (!isOn()) return;

        removeMarker(warp);

        if (!warp.isAccessible()) return;

        // Warps in deleted worlds keep a null location - creating a marker for them
        // would throw and abort the whole warp loading loop.
        Location warpLocation = warp.getLocation();
        if (warpLocation == null || warpLocation.getWorld() == null) return;

        String markerLabel = Config.DYNMAP_MARKER_LABEL.asString()
                .replace("%warp%", warp.getName())
                .replace("%owner%", warp.getOwnerName());

        // BlueMap escapes the label itself, but shows the detail as HTML and takes it from the
        // label when none is given. The warp name is typed in by a player.
        POIMarker marker = POIMarker.builder()
                .label(markerLabel)
                .detail(escapeHtml(markerLabel))
                .position(warpLocation.getX(), warpLocation.getY(), warpLocation.getZ())
                .build();

        MarkerSet markerSet = markerSets.computeIfAbsent(warpLocation.getWorld().getName(),
                worldName -> MarkerSet.builder().label("Player Warps").build());
        markerSet.getMarkers().put(warp.getWarpID().toString(), marker);

        blueMapAPI.getWorld(warpLocation.getWorld()).ifPresent(world -> {
            for (BlueMapMap map : world.getMaps()) {
                map.getMarkerSets().put(MARKER_SET_ID, markerSet);
            }
        });
    }

    public void removeMarker(Warp warp) {
        if (!isOn()) return;

        String markerId = warp.getWarpID().toString();
        markerSets.values().forEach(markerSet -> markerSet.getMarkers().remove(markerId));
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
