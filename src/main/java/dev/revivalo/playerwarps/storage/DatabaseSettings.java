package dev.revivalo.playerwarps.storage;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The "database" section of config.yml. Only read on startup, a change needs a restart.
 */
public record DatabaseSettings(
        StorageType type,
        String sqliteFile,
        String host,
        int port,
        String database,
        String username,
        String password,
        String tablePrefix,
        int syncInterval,
        boolean autoMigrate,
        int maximumPoolSize,
        int minimumIdle,
        long connectionTimeout,
        long maxLifetime,
        long keepaliveTime,
        Map<String, String> properties
) {
    private static final String DEFAULT_TABLE_PREFIX = "pw_";

    public static DatabaseSettings load() {
        final Logger logger = PlayerWarpsPlugin.get().getLogger();

        ConfigurationSection section = Config.getSection("database");
        if (section == null) {
            section = new MemoryConfiguration();
        }

        StorageType type = StorageType.fromName(section.getString("type", "YAML"));
        if (type == null) {
            logger.warning("Unknown database type '" + section.getString("type") + "' in config.yml, using YAML. "
                    + "Valid types: " + Arrays.toString(StorageType.values()));
            type = StorageType.YAML;
        }

        int port = section.getInt("port", 0);
        if (port <= 0) {
            port = type.getDefaultPort();
        }

        // Put into every statement as it is, so it must not contain anything but a plain name.
        String tablePrefix = section.getString("table-prefix", DEFAULT_TABLE_PREFIX);
        if (tablePrefix == null || !tablePrefix.matches("[A-Za-z0-9_]*")) {
            logger.warning("database.table-prefix may only contain letters, digits and '_', using '" + DEFAULT_TABLE_PREFIX + "'.");
            tablePrefix = DEFAULT_TABLE_PREFIX;
        }

        final Map<String, String> properties = new LinkedHashMap<>();
        for (String property : section.getStringList("properties")) {
            final int separator = property.indexOf('=');
            if (separator <= 0) {
                logger.warning("Ignoring database property '" + property + "', it has to be written as key=value.");
                continue;
            }

            properties.put(property.substring(0, separator).trim(), property.substring(separator + 1).trim());
        }

        ConfigurationSection pool = section.getConfigurationSection("pool");
        if (pool == null) {
            pool = new MemoryConfiguration();
        }

        return new DatabaseSettings(
                type,
                section.getString("sqlite-file", "warps.db"),
                section.getString("host", "localhost"),
                port,
                section.getString("database", "playerwarps"),
                section.getString("username", "root"),
                section.getString("password", ""),
                tablePrefix,
                Math.max(1, section.getInt("sync-interval", 10)),
                section.getBoolean("auto-migrate", true),
                Math.max(1, pool.getInt("maximum-pool-size", 10)),
                Math.max(0, pool.getInt("minimum-idle", 2)),
                pool.getLong("connection-timeout", 30000),
                pool.getLong("max-lifetime", 1800000),
                pool.getLong("keepalive-time", 0),
                properties
        );
    }
}
