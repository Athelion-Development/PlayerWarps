package dev.revivalo.playerwarps.warp;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.util.ItemUtil;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Warp implements ConfigurationSerializable {
    public static final String UNKNOWN_OWNER = "Unknown";

    private UUID warpID;
    private UUID owner;
    private String ownerName;
    private WarpStatus status;
    private boolean verificationNeeded;
    private String name;
    private String displayName;
    private String password;
    private String description;
    private String stars;
    private Location location;
    /**
     * The stored location of a warp whose world is not loaded, exactly as it was read from
     * data.yml. It is written back unchanged, so the warp keeps its location until the world
     * is loaded again instead of losing it on the next save.
     */
    private Map<String, Object> unresolvedLocation;
    private boolean locationRestoreFailed;
    private int rating;
    private int visits;
    private int todayVisits;
    private int admission;
    private long dateCreated;
    private long lastActivity;
    private long featuredTimestamp;
    private Set<UUID> reviewers = new HashSet<>();
    private Set<UUID> uniqueVisitors = new HashSet<>();
    private Set<UUID> blockedPlayers = new HashSet<>();
    private Category category;
    private ItemStack menuItem;

    public Warp(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            // One unreadable value must not cost the whole warp. It is skipped and reported,
            // and the warp keeps the default for that field.
            try {
                readValue(entry.getKey(), entry.getValue());
            } catch (RuntimeException ex) {
                PlayerWarpsPlugin.get().getLogger().warning(String.format("Warp %s: ignoring invalid value of '%s' (%s): %s",
                        map.getOrDefault("name", map.get("uuid")), entry.getKey(), entry.getValue(), ex));
            }
        }

        if (status == null) {
            status = WarpStatus.OPENED;
        }

        // Resolved once after the whole map is read, not per key. A name stored with the warp
        // wins, because the server's user cache expires after about a month and would otherwise
        // turn every owner who has not logged in recently into "Unknown".
        if (ownerName == null || ownerName.isEmpty()) {
            ownerName = lookupOwnerName();
        }

        // A display name equal to the warp name means the owner never customized it, so it is
        // dropped to let getDisplayName() fall back to the name and follow renames.
        if (Objects.equals(displayName, name)) {
            displayName = null;
        }

        stars = TextUtil.createRatingFormat(this);
    }

    @SuppressWarnings("unchecked")
    private void readValue(String key, Object value) {
        switch (key) {
            case "uuid": warpID = value == null ? null : UUID.fromString(String.valueOf(value)); break;
            case "name": setName(value == null ? null : String.valueOf(value)); break;
            case "display-name": setDisplayName(value == null ? null : String.valueOf(value)); break;
            case "owner-id": setOwner(value == null ? null : UUID.fromString(String.valueOf(value))); break;
            case "owner-name": setOwnerName(value == null ? null : String.valueOf(value)); break;
            case "need-verification": setVerificationNeeded(Boolean.parseBoolean(String.valueOf(value))); break;
            case "loc":
                if (value instanceof Location) {
                    setLocation((Location) value);
                } else if (value instanceof Map) {
                    setUnresolvedLocation((Map<String, Object>) value);
                }
                break;
            case "lore": setDescription(value == null ? null : String.valueOf(value)); break;
            case "type": // category in old versions
            case "category": setCategory(CategoryManager.getCategoryFromName(value == null ? null : String.valueOf(value))); break;
            case "item":
                if (value instanceof String) {
                    setMenuItem(ItemUtil.getItem((String) value).build());
                } else if (value instanceof ItemStack) {
                    setMenuItem((ItemStack) value);
                }
                break;
            case "ratings": setRating(toInt(value)); break;
            case "reviewers": setReviewers(toUuidSet(value)); break;
            case "blocked-players": setBlockedPlayers(toUuidSet(value)); break;
            case "unique-visitors": setUniqueVisitors(toUuidSet(value)); break;
            case "visits": setVisits(toInt(value)); break;
            case "status": setStatus(value == null ? null : WarpStatus.valueOf(String.valueOf(value).toUpperCase(Locale.ENGLISH))); break;
            // A missing password is stored as null and must not turn into the text "null".
            case "password": setPassword(value == null ? null : String.valueOf(value)); break;
            case "admission": setAdmission(toInt(value)); break;
            case "date-created": setDateCreated(toLong(value)); break;
            case "last-activity": setLastActivity(toLong(value)); break;
            case "featured": setFeaturedTimestamp(toLong(value)); break;
        }
    }

    private static int toInt(Object value) {
        if (value == null) return 0;
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value).trim());
    }

    private static long toLong(Object value) {
        if (value == null) return 0;
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value).trim());
    }

    /**
     * Invalid entries are dropped one by one, so a single bad UUID keeps the rest of the list.
     */
    private static Set<UUID> toUuidSet(Object value) {
        final Set<UUID> result = new HashSet<>();
        if (!(value instanceof Collection<?> collection)) {
            return result;
        }

        for (Object entry : collection) {
            try {
                result.add(UUID.fromString(String.valueOf(entry)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return result;
    }

    /**
     * Whether the warp has everything it needs to be used and saved. Warps missing their ID,
     * owner or name are not loaded, but kept in data.yml as they are.
     */
    public boolean isComplete() {
        return warpID != null && owner != null && name != null;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
            put("uuid", getWarpID().toString());
            put("name", getName());
            put("display-name", getRawDisplayName());
            put("owner-id", getOwner() == null ? null : getOwner().toString());
            // Never persist the placeholder, so the name is looked up again next time.
            put("owner-name", UNKNOWN_OWNER.equals(ownerName) ? null : ownerName);
            put("loc", getLocation() != null ? getLocation() : unresolvedLocation);
            put("lore", getDescription());
            put("item", getMenuItem());
            put("ratings", getRating());
            put("need-verification", isVerificationNeeded());
            put("reviewers", getReviewers().stream().map(UUID::toString).collect(Collectors.toList()));
            put("blocked-players", getBlockedPlayers().stream().map(UUID::toString).collect(Collectors.toList()));
            put("unique-visitors", getUniqueVisitors().stream().map(UUID::toString).collect(Collectors.toList()));
            put("category", getCategory() == null ? "all" : getCategory().getType());
            put("password", getPassword());
            put("visits", getVisits());
            put("status", getStatus().name());
            put("admission", getAdmission());
            put("date-created", getDateCreated());
            put("last-activity", getLastActivity());
            put("featured", getFeaturedTimestamp());
        }};
    }

//    public SkullBuilder getItem() {
//        if (tempItem == null) {
//            PlayerWarpsPlugin.get().getLogger().info("Temp item");
//            tempItem = ItemBuilder.skull().owner(PlayerWarpsPlugin.get().getServer().getOfflinePlayer(owner));
//            //tempItem = ItemUtil.getItem(Config.DEFAULT_WARP_ITEM.asString(), owner);
//        }
//
//        return tempItem;
//    }

    public float getConvertedRating() {
        return (float) getRating() / getReviewers().size();
    }

    public boolean isPasswordProtected(){
        return getStatus() == WarpStatus.PASSWORD_PROTECTED;
    }

    public boolean isVerified() {
        return !verificationNeeded;
    }

    public boolean isAccessible(){
        return isVerified() && getStatus() != WarpStatus.CLOSED;
    }

    public boolean canManage(Player player){
        return PermissionUtil.hasPermission(player, PermissionUtil.Permission.MANAGE_OTHERS)
            || Objects.equals(player.getUniqueId(), getOwner());
    }

    public boolean isBlocked(UUID playerUuid) {
        return blockedPlayers.contains(playerUuid);
    }

    public void block(OfflinePlayer player) {
        blockedPlayers.add(player.getUniqueId());
    }

    public void unblock(OfflinePlayer player) {
        blockedPlayers.remove(player.getUniqueId());
    }

    public boolean isBlocked(OfflinePlayer player) {
        return isBlocked(player.getUniqueId());
    }

    public boolean isOwner(Player player) {
        return Objects.equals(owner, player.getUniqueId());
    }

    public UUID getWarpID() {
        return warpID;
    }

    public void setWarpID(UUID warpID) {
        this.warpID = warpID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName == null ? name : TextUtil.colorize(displayName);
    }

    /**
     * Display name as it was entered by the owner (with &amp; color codes).
     * Used for serialization so the stored value survives a save/load cycle unchanged.
     */
    public String getRawDisplayName() {
        return displayName == null ? name : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPassword() {
        return password;
    }

    public boolean validatePassword(String passwordToValidate) {
        return this.password != null && this.password.equals(passwordToValidate);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    /**
     * @return the location, or null while the warp's world is not loaded
     */
    public Location getLocation() {
        if (location == null && unresolvedLocation != null && !locationRestoreFailed) {
            resolveLocation();
        }

        return location;
    }

    public void setLocation(Location location) {
        if (location == null) {
            return;
        }

        if (location.getWorld() == null) {
            return;
        }

        if (PlayerWarpsPlugin.get().getServer().getWorld(location.getWorld().getName()) == null) {
            return;
        }

        this.location = location;
        this.unresolvedLocation = null;
    }

    /**
     * Keeps the serialized location of a warp whose world could not be found. Once the world is
     * loaded, {@link #getLocation()} turns it into a real location.
     */
    public void setUnresolvedLocation(Map<String, Object> serializedLocation) {
        if (serializedLocation == null || location != null) {
            return;
        }

        this.unresolvedLocation = new LinkedHashMap<>(serializedLocation);
        resolveLocation();
    }

    public boolean hasUnresolvedLocation() {
        return location == null && unresolvedLocation != null;
    }

    /**
     * Name of the warp's world, also while that world is not loaded. Null if the warp has no
     * location at all.
     */
    public String getWorldName() {
        final Location resolved = getLocation();
        if (resolved != null && resolved.getWorld() != null) {
            return resolved.getWorld().getName();
        }

        final Object worldName = unresolvedLocation == null ? null : unresolvedLocation.get("world");
        return worldName == null ? null : String.valueOf(worldName);
    }

    private void resolveLocation() {
        final Object worldName = unresolvedLocation.get("world");
        if (worldName == null || PlayerWarpsPlugin.get().getServer().getWorld(String.valueOf(worldName)) == null) {
            return;
        }

        try {
            location = Location.deserialize(unresolvedLocation);
            unresolvedLocation = null;
        } catch (RuntimeException ex) {
            // Still kept and saved as it is, only no longer retried on every access.
            locationRestoreFailed = true;
            PlayerWarpsPlugin.get().getLogger().warning("Cannot restore the location of warp " + name + ": " + ex.getMessage());
        }
    }

    public int getRating() {
        return rating;
    }

    public double getReview() {
        return (double) rating / (getReviewers().isEmpty() ? 1 : getReviewers().size());
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public Set<UUID> getReviewers() {
        return reviewers;
    }

    public void setReviewers(Set<UUID> reviewers) {
        this.reviewers = reviewers == null ? new HashSet<>() : reviewers;
    }

    public Set<UUID> getBlockedPlayers() {
        return blockedPlayers == null ? Collections.emptySet() : blockedPlayers;
    }

    public void setBlockedPlayers(Set<UUID> blockedPlayers) {
        this.blockedPlayers = blockedPlayers == null ? new HashSet<>() : blockedPlayers;
    }

    public int getVisits() {
        return visits;
    }

    public void setVisits(int visits) {
        this.visits = visits;
    }

    public Set<UUID> getUniqueVisitors() {
        return uniqueVisitors == null ? Collections.emptySet() : uniqueVisitors;
    }

    public void setUniqueVisitors(Set<UUID> uniqueVisitors) {
        this.uniqueVisitors = uniqueVisitors == null ? new HashSet<>() : uniqueVisitors;
    }

    /**
     * Number of distinct players that have visited this warp.
     */
    public int getUniqueVisits() {
        return getUniqueVisitors().size();
    }

    /**
     * Records a visitor. Returns true if this was their first visit of this warp.
     */
    public boolean addUniqueVisitor(UUID playerUuid) {
        if (uniqueVisitors == null) {
            uniqueVisitors = new HashSet<>();
        }

        return uniqueVisitors.add(playerUuid);
    }

    public int getTodayVisits() {
        return todayVisits;
    }

    public void setTodayVisits(int todayVisits) {
        this.todayVisits = todayVisits;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public boolean hasAdmission() {
        return admission > 0;
    }

    public int getAdmission() {
        return admission;
    }

    public void setAdmission(int admission) {
        this.admission = admission;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WarpStatus getStatus() {
        return status;
    }

    public void setStatus(WarpStatus status) {
        this.status = status == null ? WarpStatus.OPENED : status;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(long dateCreated) {
        this.dateCreated = dateCreated;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public ItemStack getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(ItemStack menuItem) {
        this.menuItem = menuItem;
    }

    public String getStars() {
        return stars;
    }

    public void setStars(String stars) {
        this.stars = stars;
    }

    @Override
    public String toString(){return warpID.toString();}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Warp warp = (Warp) o;
        return Objects.equals(warpID, warp.warpID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warpID);
    }

    public boolean isVerificationNeeded() {
        return verificationNeeded;
    }

    public void setVerificationNeeded(boolean verificationNeeded) {
        this.verificationNeeded = verificationNeeded;
    }

    public long getFeaturedTimestamp() {
        return featuredTimestamp;
    }

    public void setFeaturedTimestamp(long featuredTimestamp) {
        this.featuredTimestamp = featuredTimestamp;
    }

    public String getOwnerName() {
        return ownerName == null ? UNKNOWN_OWNER : ownerName;
    }

    public boolean hasKnownOwnerName() {
        return ownerName != null && !ownerName.isEmpty() && !ownerName.equals(UNKNOWN_OWNER);
    }

    /**
     * Re-reads the owner name from the server. Call from the main thread only.
     */
    public void refreshOwnerName() {
        final String resolved = lookupOwnerName();
        if (!UNKNOWN_OWNER.equals(resolved)) {
            ownerName = resolved;
        }
    }

    private String lookupOwnerName() {
        if (owner == null) {
            return UNKNOWN_OWNER;
        }

        final Player onlineOwner = PlayerWarpsPlugin.get().getServer().getPlayer(owner);
        if (onlineOwner != null) {
            return onlineOwner.getName();
        }

        final OfflinePlayer offlinePlayer = PlayerWarpsPlugin.get().getServer().getOfflinePlayer(owner);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : UNKNOWN_OWNER;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}