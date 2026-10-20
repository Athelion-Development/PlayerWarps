package dev.revivalo.playerwarps.storage;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum StorageType {
    YAML(0),
    SQLITE(0),
    MYSQL(3306),
    MARIADB(3306),
    POSTGRESQL(5432);

    private final int defaultPort;

    StorageType(int defaultPort) {
        this.defaultPort = defaultPort;
    }

    /**
     * Whether the database runs on its own and can be shared by several servers.
     */
    public boolean isRemote() {
        return defaultPort > 0;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    @Nullable
    public static StorageType fromName(@Nullable String name) {
        if (name == null) {
            return null;
        }

        try {
            return valueOf(name.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
