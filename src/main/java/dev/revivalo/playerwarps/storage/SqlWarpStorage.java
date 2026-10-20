package dev.revivalo.playerwarps.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.Data;
import dev.revivalo.playerwarps.util.Debug;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps warps in an SQL database (SQLite, MySQL, MariaDB or PostgreSQL), one row per warp. The
 * row holds the warp in the same YAML form as data.yml, plus its name and owner for people
 * looking into the table.
 * <p>
 * Warps are still used from memory. Every sync-interval, warps changed or removed on this server
 * are written to the database, and then warps changed or removed by other servers sharing it
 * are read back. A warp changed on two servers between two syncs keeps the version written last.
 * <p>
 * Every query runs on one separate thread, so writes reach the database in the order they were
 * made and the main thread never waits for it - except on shutdown, when the last changes have
 * to be written before the server stops.
 */
public final class SqlWarpStorage implements WarpStorage {
    private static final String PREFIX = "§8[§bPlayerWarps§8] §7";
    private static final String MIGRATED_SUFFIX = ".migrated";
    private static final int FETCH_CHUNK_SIZE = 500;

    private final DatabaseSettings settings;
    private final String table;
    private final Logger logger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "PlayerWarps-Database");
        thread.setDaemon(true);
        return thread;
    });

    private volatile HikariDataSource dataSource;

    // Everything below is only used on the main thread.

    /**
     * What each warp looked like when it was last written to or read from the database, by ID.
     */
    private final Map<String, Map<String, Object>> savedState = new HashMap<>();
    /**
     * Warps removed on this server whose rows are not deleted yet.
     */
    private final Set<String> pendingDeletes = new HashSet<>();
    /**
     * The updated_at of every row as this server last read or wrote it, also of rows that
     * could not be loaded. A different value in the database means another server changed it.
     */
    private final Map<String, Long> knownVersions = new HashMap<>();
    private boolean loaded;
    /**
     * A load or sync is running. They are never run side by side.
     */
    private boolean busy;
    private int failedConnections;
    private BukkitTask syncTask;

    public SqlWarpStorage(DatabaseSettings settings) {
        this.settings = settings;
        this.table = settings.tablePrefix() + "warps";
        this.logger = PlayerWarpsPlugin.get().getLogger();
    }

    @Override
    public StorageType getType() {
        return settings.type();
    }

    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void load(WarpManager manager, Runnable onLoaded) {
        busy = true;

        final LocalWarps localWarps = settings.autoMigrate() ? readLocalWarps() : LocalWarps.NONE;

        runQuery(() -> {
            connect();
            return localWarps.rows().isEmpty() ? null : importWarps(localWarps.rows());
        }, migration -> {
            failedConnections = 0;
            if (migration != null) {
                finishMigration(migration, localWarps, null);
            }

            pull(manager, () -> {
                loaded = true;
                busy = false;
                logger.info(String.format("Loaded %d warps from the %s database.", manager.getWarps().size(), settings.type()));
                onLoaded.run();
            }, () -> busy = false);
        }, ex -> {
            busy = false;
            if (failedConnections++ == 0) {
                logger.log(Level.SEVERE, "Cannot connect to the " + settings.type() + " database. Warps are loaded once it is reachable, "
                        + "retrying every " + settings.syncInterval() + " seconds.", ex);
            } else {
                logger.warning("Still cannot connect to the " + settings.type() + " database: " + ex);
            }
        });
    }

    @Override
    public void startTasks(WarpManager manager) {
        final long intervalInTicks = settings.syncInterval() * 20L;
        syncTask = PlayerWarpsPlugin.get().runRepeating(() -> sync(manager), intervalInTicks, intervalInTicks);
    }

    @Override
    public void save(WarpManager manager, boolean asynchronously) {
        if (asynchronously) {
            sync(manager);
        } else {
            saveNow(manager);
        }
    }

    @Override
    public void shutdown(WarpManager manager) {
        if (syncTask != null) {
            syncTask.cancel();
        }

        saveNow(manager);

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Database work did not finish in time on shutdown.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        if (dataSource != null) {
            dataSource.close();
        }
    }

    /**
     * Writes the changes and waits for it. Only for shutdown, when nothing can be scheduled anymore.
     */
    private void saveNow(WarpManager manager) {
        if (dataSource == null) {
            return;
        }

        final PendingChanges changes = collectChanges(manager);
        if (changes.isEmpty()) {
            return;
        }

        try {
            executor.submit(() -> {
                write(changes);
                return null;
            }).get(30, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            logger.log(Level.SEVERE, "Cannot save warps to the database on shutdown.", ex.getCause());
        } catch (TimeoutException ex) {
            logger.severe("Saving warps to the database on shutdown took too long, the latest changes may be lost.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (RejectedExecutionException ignored) {
        }
    }

    /**
     * Writes what changed on this server, then reads what other servers changed.
     */
    public void sync(WarpManager manager) {
        if (busy) {
            return;
        }

        if (!loaded) {
            // The database was not reachable on startup.
            load(manager, manager::reportReservedNames);
            return;
        }

        busy = true;

        // Nobody else writes to a local SQLite file, so there is nothing to read back.
        final Runnable readChanges = settings.type().isRemote()
                ? () -> pull(manager, () -> busy = false, () -> busy = false)
                : () -> busy = false;

        final PendingChanges changes = collectChanges(manager);
        if (changes.isEmpty()) {
            readChanges.run();
            return;
        }

        runQuery(() -> {
            write(changes);
            return null;
        }, ignored -> {
            changes.deletes().forEach(pendingDeletes::remove);
            Debug.log("Saved %d warps and deleted %d in the database.", changes.upserts().size(), changes.deletes().size());
            readChanges.run();
        }, ex -> {
            logger.log(Level.SEVERE, "Cannot save warps to the database, trying again on the next sync.", ex);
            // Seen as unsaved again, so they are written next time.
            for (WarpRow row : changes.upserts()) {
                savedState.remove(row.id());
                knownVersions.remove(row.id());
            }
            busy = false;
        });
    }

    /**
     * Finds the warps that changed since they were last saved and the ones that were removed.
     * They count as saved right away, so a change made while they are being written is
     * picked up by the next sync.
     */
    private PendingChanges collectChanges(WarpManager manager) {
        final List<WarpRow> upserts = new ArrayList<>();
        final Set<String> present = new HashSet<>();
        final long now = System.currentTimeMillis();

        for (Warp warp : manager.getWarps()) {
            final String id = warp.getWarpID().toString();
            present.add(id);

            try {
                final Map<String, Object> snapshot = WarpCodec.snapshot(warp);
                if (snapshot.equals(savedState.get(id))) {
                    continue;
                }

                upserts.add(WarpRow.of(warp, WarpCodec.encode(warp), now));
                savedState.put(id, snapshot);
                knownVersions.put(id, now);
            } catch (RuntimeException ex) {
                // Keeps the version already in the database.
                logger.log(Level.SEVERE, "Cannot save warp " + warp.getName() + ", keeping its previously saved version.", ex);
            }
        }

        savedState.keySet().removeIf(id -> {
            if (present.contains(id)) {
                return false;
            }

            knownVersions.remove(id);
            pendingDeletes.add(id);
            return true;
        });

        return new PendingChanges(upserts, List.copyOf(pendingDeletes));
    }

    /**
     * Reads the rows other servers changed or deleted since the last time and applies them.
     */
    private void pull(WarpManager manager, Runnable onSuccess, Runnable onFailure) {
        runQuery(this::fetchVersions, versions -> {
            removeDeletedWarps(manager, versions);

            final List<String> changed = versions.entrySet().stream()
                    .filter(entry -> !entry.getValue().equals(knownVersions.get(entry.getKey())))
                    .map(Map.Entry::getKey)
                    .filter(id -> !pendingDeletes.contains(id))
                    .toList();

            if (changed.isEmpty()) {
                onSuccess.run();
                return;
            }

            runQuery(() -> fetchWarps(changed), rows -> {
                applyRows(manager, rows);
                onSuccess.run();
            }, ex -> {
                logger.log(Level.WARNING, "Cannot read warps from the database.", ex);
                onFailure.run();
            });
        }, ex -> {
            logger.log(Level.WARNING, "Cannot read warps from the database.", ex);
            onFailure.run();
        });
    }

    private void removeDeletedWarps(WarpManager manager, Map<String, Long> versions) {
        final Iterator<String> knownIDs = knownVersions.keySet().iterator();
        while (knownIDs.hasNext()) {
            final String id = knownIDs.next();
            if (versions.containsKey(id)) {
                continue;
            }

            knownIDs.remove();
            savedState.remove(id);
            findWarp(manager, id).ifPresent(warp -> {
                manager.removeWarp(warp);
                manager.hideFromMaps(warp);
                Debug.log("Warp %s was removed on another server.", warp.getName());
            });
        }
    }

    private static Optional<Warp> findWarp(WarpManager manager, String id) {
        try {
            return manager.getWarpByID(id);
        } catch (IllegalArgumentException ex) {
            // Not an ID this plugin wrote.
            return Optional.empty();
        }
    }

    private void applyRows(WarpManager manager, List<WarpRow> rows) {
        for (WarpRow row : rows) {
            if (pendingDeletes.contains(row.id())) {
                continue;
            }

            final Optional<Warp> local = findWarp(manager, row.id());
            if (local.isPresent() && isChanged(local.get())) {
                // Changed here as well - this server's version is written on the next sync.
                continue;
            }

            knownVersions.put(row.id(), row.updatedAt());
            final Warp remote = row.data() == null ? null : WarpCodec.decode(row.data());

            if (remote == null) {
                logger.warning("Warp " + row.id() + " (" + row.name() + ") in the database could not be loaded (it is missing its ID, owner or name, or see the errors above). "
                        + "It is kept unchanged in the database.");
                continue;
            }

            final Warp warp;
            if (local.isPresent()) {
                // Updated in place - menus and players may hold on to the warp object.
                warp = local.get();
                warp.updateFrom(remote);
            } else {
                warp = remote;
                manager.getWarpFromName(warp.getName()).ifPresent(sameName -> logger.warning("There are two warps called '"
                        + warp.getName() + "' (" + sameName.getWarpID() + " and " + warp.getWarpID() + "). Rename one of them."));
                manager.addWarp(warp);
            }

            savedState.put(row.id(), WarpCodec.snapshot(warp));
            manager.showOnMaps(warp);
        }
    }

    private boolean isChanged(Warp warp) {
        try {
            return !WarpCodec.snapshot(warp).equals(savedState.get(warp.getWarpID().toString()));
        } catch (RuntimeException ex) {
            return true;
        }
    }

    /**
     * Moves the warps from data.yml into the database, as /pwarp migrate local-to-db.
     */
    public void importLocalWarps(WarpManager manager, CommandSender sender) {
        if (!loaded) {
            sender.sendMessage(PREFIX + "The database is not connected, see the console.");
            return;
        }

        final LocalWarps localWarps = readLocalWarps();
        if (localWarps.rows().isEmpty()) {
            sender.sendMessage(PREFIX + "There are no warps in data.yml to move.");
            return;
        }

        sender.sendMessage(PREFIX + "Moving " + localWarps.rows().size() + " warps to the database...");
        runQuery(() -> importWarps(localWarps.rows()), result -> {
            finishMigration(result, localWarps, sender);
            // Shows the imported warps right away instead of on the next sync.
            sync(manager);
        }, ex -> {
            logger.log(Level.SEVERE, "Cannot move the warps from data.yml to the database.", ex);
            sender.sendMessage(PREFIX + "§cMoving the warps failed, see the console. data.yml was not changed.");
        });
    }

    /**
     * Reads data.yml for moving its warps into the database. Runs on the main thread, as
     * reading creates the warps.
     */
    private LocalWarps readLocalWarps() {
        final File file = getLocalFile();
        if (!file.isFile()) {
            return LocalWarps.NONE;
        }

        final Data data = new Data();
        if (!data.getYamlFile().isLoaded()) {
            logger.severe("data.yml cannot be read, so its warps are not moved to the database. "
                    + "Fix the file, then restart or run /pwarp migrate local-to-db.");
            return LocalWarps.NONE;
        }

        final Map<String, Object> unloadableWarps = new LinkedHashMap<>();
        final List<WarpRow> rows = new ArrayList<>();
        final long now = System.currentTimeMillis();
        int failed = 0;

        for (Warp warp : YamlWarpStorage.readWarps(data, unloadableWarps)) {
            try {
                rows.add(WarpRow.of(warp, WarpCodec.encode(warp), now));
            } catch (RuntimeException ex) {
                logger.log(Level.SEVERE, "Cannot move warp " + warp.getName() + " to the database.", ex);
                failed++;
            }
        }

        return new LocalWarps(rows, unloadableWarps.size() + failed);
    }

    /**
     * Keeps data.yml under another name, so its warps are not moved again on the next start.
     */
    private void finishMigration(MigrationResult result, LocalWarps localWarps, @Nullable CommandSender sender) {
        final List<String> report = new ArrayList<>();
        report.add("Moved " + result.imported() + " warps from data.yml to the database.");

        if (result.alreadyPresent() > 0) {
            report.add(result.alreadyPresent() + " warps were already in the database and were left as they are there.");
        }

        if (!result.nameConflicts().isEmpty()) {
            report.add(result.nameConflicts().size() + " warps were not moved, a warp of the same name is already in the database: "
                    + String.join(", ", result.nameConflicts()));
        }

        if (localWarps.unloadable() > 0) {
            report.add(localWarps.unloadable() + " warps could not be read from data.yml and were not moved.");
        }

        final File file = getLocalFile();
        File renamed = new File(file.getParentFile(), file.getName() + MIGRATED_SUFFIX);
        if (renamed.exists()) {
            renamed = new File(file.getParentFile(), file.getName() + MIGRATED_SUFFIX + "-" + System.currentTimeMillis());
        }

        try {
            Files.move(file.toPath(), renamed.toPath());
            report.add("data.yml was renamed to " + renamed.getName() + " and is kept as a backup.");
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Cannot rename data.yml after moving its warps to the database. "
                    + "Its warps already in the database are skipped next time.", ex);
        }

        report.forEach(logger::info);
        if (sender != null) {
            report.forEach(line -> sender.sendMessage(PREFIX + line));
        }
    }

    private File getLocalFile() {
        return new File(PlayerWarpsPlugin.get().getDataFolder(), "data.yml");
    }

    // ----- Queries, run on the database thread only -----

    private void connect() throws SQLException {
        if (dataSource != null) {
            return;
        }

        final StorageType type = settings.type();
        final HikariConfig config = new HikariConfig();
        config.setPoolName("PlayerWarps");

        switch (type) {
            case SQLITE -> {
                final File file = new File(PlayerWarpsPlugin.get().getDataFolder(), settings.sqliteFile());
                config.setDriverClassName("org.sqlite.JDBC");
                config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
                // SQLite allows one writer at a time anyway.
                config.setMaximumPoolSize(1);
                config.setConnectionInitSql("PRAGMA busy_timeout = 5000");
            }
            case MYSQL -> {
                // Servers ship the MySQL driver. Should one not, the MariaDB driver talks to MySQL as well.
                if (isClassPresent("com.mysql.cj.jdbc.Driver")) {
                    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    config.setJdbcUrl("jdbc:mysql://" + settings.host() + ":" + settings.port() + "/" + settings.database());
                } else {
                    config.setDriverClassName("org.mariadb.jdbc.Driver");
                    config.setJdbcUrl("jdbc:mariadb://" + settings.host() + ":" + settings.port() + "/" + settings.database());
                }
            }
            case MARIADB -> {
                config.setDriverClassName("org.mariadb.jdbc.Driver");
                config.setJdbcUrl("jdbc:mariadb://" + settings.host() + ":" + settings.port() + "/" + settings.database());
            }
            case POSTGRESQL -> {
                config.setDriverClassName("org.postgresql.Driver");
                config.setJdbcUrl("jdbc:postgresql://" + settings.host() + ":" + settings.port() + "/" + settings.database());
            }
            default -> throw new IllegalStateException("Not an SQL storage: " + type);
        }

        if (type.isRemote()) {
            config.setUsername(settings.username());
            config.setPassword(settings.password());
            config.setMaximumPoolSize(settings.maximumPoolSize());
            config.setMinimumIdle(Math.min(settings.minimumIdle(), settings.maximumPoolSize()));
        }

        config.setConnectionTimeout(settings.connectionTimeout());
        config.setMaxLifetime(settings.maxLifetime());
        if (settings.keepaliveTime() > 0) {
            config.setKeepaliveTime(settings.keepaliveTime());
        }
        settings.properties().forEach(config::addDataSourceProperty);

        final HikariDataSource source = new HikariDataSource(config);
        try {
            createTable(source);
        } catch (SQLException | RuntimeException ex) {
            source.close();
            throw ex;
        }

        dataSource = source;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, SqlWarpStorage.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }

    private void createTable(HikariDataSource source) throws SQLException {
        final String sql = switch (settings.type()) {
            case MYSQL, MARIADB -> "CREATE TABLE IF NOT EXISTS " + table + " ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "owner_id VARCHAR(36) NULL, "
                    // TEXT ends at 64 KB, which a popular warp's visitor list can outgrow.
                    + "data MEDIUMTEXT NOT NULL, "
                    + "updated_at BIGINT NOT NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            default -> "CREATE TABLE IF NOT EXISTS " + table + " ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "owner_id VARCHAR(36), "
                    + "data TEXT NOT NULL, "
                    + "updated_at BIGINT NOT NULL"
                    + ")";
        };

        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            if (settings.type() == StorageType.SQLITE) {
                // Readers do not block the writer.
                statement.execute("PRAGMA journal_mode = WAL");
            }
            statement.execute(sql);
        }
    }

    private String upsertStatement() {
        final String insert = "INSERT INTO " + table + " (id, name, owner_id, data, updated_at) VALUES (?, ?, ?, ?, ?) ";
        return switch (settings.type()) {
            case MYSQL, MARIADB -> insert + "ON DUPLICATE KEY UPDATE name = VALUES(name), owner_id = VALUES(owner_id), "
                    + "data = VALUES(data), updated_at = VALUES(updated_at)";
            default -> insert + "ON CONFLICT (id) DO UPDATE SET name = excluded.name, owner_id = excluded.owner_id, "
                    + "data = excluded.data, updated_at = excluded.updated_at";
        };
    }

    private void write(PendingChanges changes) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!changes.upserts().isEmpty()) {
                    try (PreparedStatement statement = connection.prepareStatement(upsertStatement())) {
                        for (WarpRow row : changes.upserts()) {
                            row.bind(statement);
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                }

                if (!changes.deletes().isEmpty()) {
                    try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
                        for (String id : changes.deletes()) {
                            statement.setString(1, id);
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                }

                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private Map<String, Long> fetchVersions() throws SQLException {
        final Map<String, Long> versions = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT id, updated_at FROM " + table)) {
            while (result.next()) {
                versions.put(result.getString(1), result.getLong(2));
            }
        }
        return versions;
    }

    private List<WarpRow> fetchWarps(List<String> ids) throws SQLException {
        final List<WarpRow> rows = new ArrayList<>(ids.size());
        try (Connection connection = dataSource.getConnection()) {
            for (int from = 0; from < ids.size(); from += FETCH_CHUNK_SIZE) {
                final List<String> chunk = ids.subList(from, Math.min(ids.size(), from + FETCH_CHUNK_SIZE));
                final String placeholders = String.join(", ", Collections.nCopies(chunk.size(), "?"));

                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, name, owner_id, data, updated_at FROM " + table + " WHERE id IN (" + placeholders + ")")) {
                    for (int i = 0; i < chunk.size(); i++) {
                        statement.setString(i + 1, chunk.get(i));
                    }

                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            rows.add(new WarpRow(result.getString(1), result.getString(2), result.getString(3),
                                    result.getString(4), result.getLong(5)));
                        }
                    }
                }
            }
        }
        return rows;
    }

    /**
     * Inserts the warps that are not in the database yet. A warp whose ID is already there is
     * left alone, and so is one whose name another warp in the database has.
     */
    private MigrationResult importWarps(List<WarpRow> rows) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                final Set<String> existingIDs = new HashSet<>();
                final Set<String> existingNames = new HashSet<>();
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT id, name FROM " + table)) {
                    while (result.next()) {
                        existingIDs.add(result.getString(1));
                        existingNames.add(result.getString(2).toLowerCase(Locale.ENGLISH));
                    }
                }

                int imported = 0;
                int alreadyPresent = 0;
                final List<String> nameConflicts = new ArrayList<>();

                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO " + table + " (id, name, owner_id, data, updated_at) VALUES (?, ?, ?, ?, ?)")) {
                    for (WarpRow row : rows) {
                        if (existingIDs.contains(row.id())) {
                            alreadyPresent++;
                            continue;
                        }

                        if (!existingNames.add(row.name().toLowerCase(Locale.ENGLISH))) {
                            nameConflicts.add(row.name());
                            continue;
                        }

                        row.bind(statement);
                        statement.addBatch();
                        imported++;
                    }

                    if (imported > 0) {
                        statement.executeBatch();
                    }
                }

                connection.commit();
                return new MigrationResult(imported, alreadyPresent, nameConflicts);
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    /**
     * Runs the query on the database thread and hands its result to the main thread.
     */
    private <T> void runQuery(Callable<T> query, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        try {
            executor.execute(() -> {
                final T result;
                try {
                    result = query.call();
                } catch (Exception ex) {
                    runOnMainThread(() -> onFailure.accept(ex));
                    return;
                } catch (LinkageError error) {
                    // The database libraries are downloaded by the server on startup, which can fail.
                    runOnMainThread(() -> onFailure.accept(new IllegalStateException("A database library is missing: " + error, error)));
                    return;
                }

                runOnMainThread(() -> {
                    try {
                        onSuccess.accept(result);
                    } catch (RuntimeException ex) {
                        // Lets the caller finish up, a sync would never run again otherwise.
                        onFailure.accept(ex);
                    }
                });
            });
        } catch (RejectedExecutionException ignored) {
            // Shutting down.
        }
    }

    private static void runOnMainThread(Runnable task) {
        final PlayerWarpsPlugin plugin = PlayerWarpsPlugin.get();
        if (!plugin.isEnabled()) {
            return;
        }

        try {
            plugin.runSync(task);
        } catch (IllegalPluginAccessException ignored) {
            // Disabled in the meantime. Whatever was left is done again on the next start.
        }
    }

    private record WarpRow(String id, String name, @Nullable String ownerID, String data, long updatedAt) {
        static WarpRow of(Warp warp, String data, long updatedAt) {
            return new WarpRow(warp.getWarpID().toString(), warp.getName(),
                    warp.getOwner() == null ? null : warp.getOwner().toString(), data, updatedAt);
        }

        void bind(PreparedStatement statement) throws SQLException {
            statement.setString(1, id);
            statement.setString(2, name);
            statement.setString(3, ownerID);
            statement.setString(4, data);
            statement.setLong(5, updatedAt);
        }
    }

    private record PendingChanges(List<WarpRow> upserts, List<String> deletes) {
        boolean isEmpty() {
            return upserts.isEmpty() && deletes.isEmpty();
        }
    }

    private record LocalWarps(List<WarpRow> rows, int unloadable) {
        static final LocalWarps NONE = new LocalWarps(Collections.emptyList(), 0);
    }

    private record MigrationResult(int imported, int alreadyPresent, List<String> nameConflicts) {
    }
}
