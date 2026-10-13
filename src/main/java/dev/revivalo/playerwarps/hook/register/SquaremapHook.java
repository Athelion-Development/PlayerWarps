package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.Hook;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.marker.Icon;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SquaremapHook implements Hook<Squaremap> {
    private static final Key LAYER_KEY = Key.of("playerwarps_athelion");
    private static final Key ICON_KEY = Key.of("playerwarps_athelion_icon");
    private static final String ICON_FILE_NAME = "squaremap-icon.png";
    private static final int ICON_SIZE = 20;

    private Squaremap squaremap = null;
    // One provider per world, kept so markers survive squaremap re-creating its map worlds
    private final Map<String, SimpleLayerProvider> providers = new ConcurrentHashMap<>();

    @Override
    public @NotNull String getName() {
        return "squaremap";
    }

    @Override
    public void register() {
        if (!isPluginEnabled()) return;

        try {
            Squaremap api = SquaremapProvider.get();
            registerIcon(api);
            this.squaremap = api;
        } catch (IllegalStateException ex) {
            PlayerWarpsPlugin.get().getLogger().log(Level.WARNING, "Cannot obtain the squaremap API.", ex);
        }
    }

    private void registerIcon(Squaremap api) {
        Registry<BufferedImage> iconRegistry = api.iconRegistry();
        if (iconRegistry.hasEntry(ICON_KEY)) {
            iconRegistry.unregister(ICON_KEY);
        }
        iconRegistry.register(ICON_KEY, loadIcon());
    }

    /**
     * Uses plugins/PlayerWarps/squaremap-icon.png when present, otherwise draws a default badge.
     * The icon is anchored at its center.
     */
    private BufferedImage loadIcon() {
        File customIcon = new File(PlayerWarpsPlugin.get().getDataFolder(), ICON_FILE_NAME);
        if (customIcon.isFile()) {
            try {
                BufferedImage image = ImageIO.read(customIcon);
                if (image != null) return image;
                PlayerWarpsPlugin.get().getLogger().log(Level.WARNING, ICON_FILE_NAME + " is not a valid image, using the default icon.");
            } catch (IOException ex) {
                PlayerWarpsPlugin.get().getLogger().log(Level.WARNING, "Cannot read " + ICON_FILE_NAME + ", using the default icon.", ex);
            }
        }

        return createDefaultIcon();
    }

    private BufferedImage createDefaultIcon() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(new Color(0x3B82F6));
        graphics.fill(new Ellipse2D.Double(2, 2, 28, 28));
        graphics.setStroke(new BasicStroke(3f));
        graphics.setColor(Color.WHITE);
        graphics.draw(new Ellipse2D.Double(3.5, 3.5, 25, 25));
        graphics.fill(new Ellipse2D.Double(11, 11, 10, 10));
        graphics.dispose();

        return image;
    }

    @Override
    public boolean isOn() {
        return squaremap != null;
    }

    @Override
    public Config getConfigPath() {
        return Config.SQUAREMAP_HOOK_ENABLED;
    }

    @Override
    public @Nullable Squaremap getApi() {
        return squaremap;
    }

    public void setMarker(Warp warp) {
        if (!warp.isAccessible()) return;
        if (!isOn()) return;

        // Warps in deleted worlds keep a null location - creating a marker for them
        // would throw and abort the whole warp loading loop.
        Location location = warp.getLocation();
        if (location == null || location.getWorld() == null) return;

        SimpleLayerProvider provider = getProvider(location.getWorld());
        if (provider == null) return;

        String markerLabel = escapeHtml(Config.DYNMAP_MARKER_LABEL.asString()
                .replace("%warp%", warp.getName())
                .replace("%owner%", warp.getOwnerName()));

        Icon marker = Marker.icon(BukkitAdapter.point(location), ICON_KEY, ICON_SIZE);
        marker.markerOptions(MarkerOptions.builder()
                .hoverTooltip(markerLabel)
                .clickTooltip(markerLabel));

        provider.addMarker(markerKey(warp), marker);
    }

    public void removeMarker(Warp warp) {
        if (!isOn()) return;

        Key markerKey = markerKey(warp);
        providers.values().forEach(provider -> provider.removeMarker(markerKey));
    }

    /**
     * Returns the layer of the given world and makes sure it is registered - squaremap drops
     * registered layers whenever it re-creates its map worlds (e.g. on /squaremap reload).
     */
    private @Nullable SimpleLayerProvider getProvider(World world) {
        MapWorld mapWorld = squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).orElse(null);
        if (mapWorld == null) return null;

        SimpleLayerProvider provider = providers.computeIfAbsent(world.getName(), name ->
                SimpleLayerProvider.builder("Player Warps")
                        .showControls(true)
                        .defaultHidden(false)
                        .layerPriority(5)
                        .zIndex(250)
                        .build());

        Registry<LayerProvider> layerRegistry = mapWorld.layerRegistry();
        if (!layerRegistry.hasEntry(LAYER_KEY)) {
            layerRegistry.register(LAYER_KEY, provider);
        } else if (layerRegistry.get(LAYER_KEY) != provider) {
            // Left behind by a previous instance of this plugin (e.g. after a plugin reload)
            layerRegistry.unregister(LAYER_KEY);
            layerRegistry.register(LAYER_KEY, provider);
        }

        return provider;
    }

    private static Key markerKey(Warp warp) {
        return Key.of("playerwarps_" + warp.getWarpID());
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
