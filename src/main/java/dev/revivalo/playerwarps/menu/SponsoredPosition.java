package dev.revivalo.playerwarps.menu;

import java.util.UUID;

public class SponsoredPosition {
    private final UUID warpId;
    private final long expirationTimeStamp;

    public SponsoredPosition(UUID warpId, long expirationTimeStamp) {
        this.warpId = warpId;
        this.expirationTimeStamp = expirationTimeStamp;
    }

    public UUID getWarpId() {
        return warpId;
    }

    public long getExpirationTimeStamp() {
        return expirationTimeStamp;
    }
}
