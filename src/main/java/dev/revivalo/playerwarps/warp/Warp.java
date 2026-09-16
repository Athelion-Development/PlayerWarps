package dev.revivalo.playerwarps.warp;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.util.ItemUtil;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Warp implements ConfigurationSerializable {
    public static final String UNKNOWN_OWNER = "Unknown";

    private static final long DAY_IN_MILLIS = 24L * 60 * 60 * 1000;

    /**
     * How many days of per-day visit counts are kept. It caps the size of the map in data.yml
     * and is independent of the configured trending window, which only has to stay below it.
     */
    public static final int RECENT_VISITS_RETENTION_DAYS = 30;

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
    private int rating;
    private int visits;
    private int todayVisits;
    private int admission;
    private long dateCreated;
    private long lastActivity;
    private long featuredTimestamp;
    private Set<UUID> reviewers;
    private Set<UUID> uniqueVisitors = new HashSet<>();
    // Written when a player arrives and read while a menu is built, which need not be the
    // same thread, so iteration must not throw a ConcurrentModificationException.
    private Map<Long, Integer> recentVisits = new ConcurrentHashMap<>();
    private Set<UUID> blockedPlayers = new HashSet<>();
    private Category category;
    private ItemStack menuItem;

    public Warp(Map<String, Object> map) {
        for (String key : map.keySet()) {
            final Object value = map.get(key);
            switch (key){
                case "uuid": warpID = UUID.fromString((String) value); break;
                case "name": setName((String) value); break;
                case "display-name": setDisplayName((String) value); break;
                case "owner-id": setOwner(UUID.fromString((String) value)); break;
                case "owner-name": setOwnerName((String) value); break;
                case "need-verification": setVerificationNeeded((boolean) value); break;
                case "loc": setLocation((Location) value); break;
                case "lore": setDescription((String) value); break;
                case "type": // category in old versions
                case "category": setCategory(CategoryManager.getCategoryFromName((String) value)); break;
                case "item":
                    if (value instanceof String) {
                        setMenuItem(ItemUtil.getItem((String) value).build());
                    } else {
                        setMenuItem((ItemStack) value);
                    }
                    break;
                case "ratings": setRating((int) value); break;
                case "reviewers": setReviewers(((List<String>) value).stream().map(UUID::fromString).collect(Collectors.toCollection(HashSet::new))); break;
                case "blocked-players": setBlockedPlayers(((List<String>) value).stream().map(UUID::fromString).collect(Collectors.toCollection(HashSet::new))); break;
                case "unique-visitors": setUniqueVisitors(((List<String>) value).stream().map(UUID::fromString).collect(Collectors.toCollection(HashSet::new))); break;
                case "visits": setVisits((int) value); break;
                case "recent-visits": setRecentVisits(readRecentVisits(value)); break;
                case "status": setStatus(WarpStatus.valueOf((String) value)); break;
                case "password": setPassword(String.valueOf(value)); break;
                case "admission": setAdmission(Integer.parseInt(String.valueOf(value))); break;
                case "date-created": setDateCreated(Long.parseLong(String.valueOf(value))); break;
                case "last-activity": setLastActivity(Long.parseLong(String.valueOf(value))); break;
                case "featured": setFeaturedTimestamp(Long.parseLong(String.valueOf(value))); break;
            }

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

        if (reviewers != null) stars = TextUtil.createRatingFormat(this);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
            put("uuid", getWarpID().toString());
            put("name", getName());
            put("display-name", getRawDisplayName());
            put("owner-id", getOwner().toString());
            // Never persist the placeholder, so the name is looked up again next time.
            put("owner-name", UNKNOWN_OWNER.equals(ownerName) ? null : ownerName);
            put("loc", getLocation());
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
            put("recent-visits", serializeRecentVisits());
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
        return this.password.equals(passwordToValidate);
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

    public Location getLocation() {
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
        this.reviewers = reviewers;
    }

    public Set<UUID> getBlockedPlayers() {
        return blockedPlayers == null ? Collections.emptySet() : blockedPlayers;
    }

    public void setBlockedPlayers(Set<UUID> blockedPlayers) {
        this.blockedPlayers = blockedPlayers;
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
        this.uniqueVisitors = uniqueVisitors;
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

    public Map<Long, Integer> getRecentVisits() {
        return recentVisits == null ? Collections.emptyMap() : recentVisits;
    }

    public void setRecentVisits(Map<Long, Integer> recentVisits) {
        this.recentVisits = recentVisits == null ? new ConcurrentHashMap<>() : recentVisits;
    }

    /**
     * Adds one visit to today's bucket and drops buckets that fell out of the retention window,
     * so the map stays bounded no matter how long the warp lives.
     */
    public void recordVisit() {
        if (recentVisits == null) {
            recentVisits = new ConcurrentHashMap<>();
        }

        final long today = currentDay();
        recentVisits.merge(today, 1, Integer::sum);
        recentVisits.keySet().removeIf(day -> day <= today - RECENT_VISITS_RETENTION_DAYS);
    }

    /**
     * Number of visits over the last {@code days} days, today included.
     */
    public int getRecentVisits(int days) {
        if (days <= 0) {
            return 0;
        }

        final long oldestDay = currentDay() - (days - 1);
        int sum = 0;
        for (Map.Entry<Long, Integer> entry : getRecentVisits().entrySet()) {
            if (entry.getKey() >= oldestDay) {
                sum += entry.getValue();
            }
        }

        return sum;
    }

    /**
     * Day number the visit counters are bucketed by, counted from the epoch in UTC.
     */
    public static long currentDay() {
        return System.currentTimeMillis() / DAY_IN_MILLIS;
    }

    /**
     * Visit buckets keyed by day, as strings, because YAML mappings are read back with string
     * keys. Returns null when there is nothing to store, to keep data.yml free of empty maps.
     */
    private Map<String, Object> serializeRecentVisits() {
        if (getRecentVisits().isEmpty()) {
            return null;
        }

        // Buckets past the retention window are left out, so a warp nobody visits any more
        // stops taking up room in the file without serialize() having to change state.
        final long oldestDay = currentDay() - RECENT_VISITS_RETENTION_DAYS;

        final Map<String, Object> serialized = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : getRecentVisits().entrySet()) {
            if (entry.getKey() > oldestDay) {
                serialized.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        return serialized.isEmpty() ? null : serialized;
    }

    /**
     * Reads the visit buckets back. The value arrives either as a plain map or as a section,
     * depending on whether the warp was deserialized while loading the file or afterwards.
     */
    private static Map<Long, Integer> readRecentVisits(Object value) {
        final Map<Long, Integer> parsed = new ConcurrentHashMap<>();

        final Map<?, ?> raw;
        if (value instanceof ConfigurationSection section) {
            raw = section.getValues(false);
        } else if (value instanceof Map<?, ?> map) {
            raw = map;
        } else {
            return parsed;
        }

        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            try {
                parsed.put(Long.parseLong(String.valueOf(entry.getKey())), Integer.parseInt(String.valueOf(entry.getValue())));
            } catch (NumberFormatException ignored) {
                // A hand-edited data.yml should not keep the whole warp from loading.
            }
        }

        return parsed;
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
        this.status = status;
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