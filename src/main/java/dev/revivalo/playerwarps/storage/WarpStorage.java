package dev.revivalo.playerwarps.storage;

import dev.revivalo.playerwarps.warp.WarpManager;

/**
 * Where warps are kept between restarts. Warps are always used from memory (the
 * {@link WarpManager}); a storage only reads them on startup and writes them back.
 * All methods are called from the main thread.
 */
public interface WarpStorage {

    static WarpStorage create(DatabaseSettings settings) {
        return settings.type() == StorageType.YAML
                ? new YamlWarpStorage()
                : new SqlWarpStorage(settings);
    }

    StorageType getType();

    /**
     * Fills the manager with the stored warps. May finish later, {@code onLoaded} runs on the
     * main thread once the warps are in place.
     */
    void load(WarpManager manager, Runnable onLoaded);

    /**
     * Starts the repeating saves.
     */
    void startTasks(WarpManager manager);

    /**
     * Writes the warps. Only the serialization runs on the calling thread when
     * {@code asynchronously} is set.
     */
    void save(WarpManager manager, boolean asynchronously);

    /**
     * Writes everything not saved yet and releases the storage. The scheduler is no longer
     * available at this point.
     */
    void shutdown(WarpManager manager);
}
