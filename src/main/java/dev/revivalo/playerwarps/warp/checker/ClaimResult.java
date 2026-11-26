package dev.revivalo.playerwarps.warp.checker;

public enum ClaimResult {
    /**
     * The location is not in any claim of this plugin.
     */
    UNCLAIMED,
    /**
     * The player owns the claim, or is trusted in it while trusted players may place warps.
     */
    ALLOWED,
    /**
     * Someone else's claim. The checker has already told the player.
     */
    DENIED
}
