package dev.revivalo.playerwarps.configuration.file;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.YamlFile;
import dev.revivalo.playerwarps.util.TextUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.stream.Collectors;

public enum Config {
    LANGUAGE,
    WARP_NAME_FORMAT,
    BANNED_ITEMS,
    DISABLED_WORLDS,
    ENABLED_REGIONS,
    UPDATE_CHECKER,
    DEMAND_VERIFICATION,
    AUTOSAVE_ENABLED,
    AUTOSAVE_ANNOUNCE,
    AUTOSAVE_INTERVAL,
    WARP_CREATION_FOR_TRUSTED_PLAYERS,
    WARP_DESCRIPTION_COLOR,
    WARP_CREATION_NOTIFICATION,
    WARP_VISIT_NOTIFICATION,
    TELEPORT_DELAY,
    ALLOW_COLORS_IN_WARP_DISPLAY_NAMES,
    FREE_WARPS,
    WARP_PRICING,
    WARP_FIXED_PRICE,
    WARP_CUSTOM_PRICE,
    DATE_FORMAT,
    SORT_BY,
    SELECTED_SORT,
    OTHER_SORT,
    DYNMAP_MARKER_ICON,
    DYNMAP_MARKER_LABEL,
    ENABLE_HINTS,
    DELETE_WARP_REFUND,
    WARP_NAME_MAX_LENGTH,
    MAX_WARP_ADMISSION,
    RELOCATE_WARP_FEE,
    RENAME_WARP_FEE,
    TRANSFER_OWNERSHIP_FEE,
    SET_STATUS_FEE,
    DEFAULT_WARP_STATUS,
    SET_TYPE_FEE,
    SET_PREVIEW_ITEM_FEE,
    SET_DESCRIPTION_FEE,
    SET_DESCRIPTION_CHARACTERS_LIMIT,
    SET_DISPLAY_NAME_FEE,
    SET_ADMISSION_FEE,
    FEATURE_WARP_FEE,
    DEFAULT_WARP_ITEM,
    DEFAULT_LIMIT_SIZE,
    ALLOW_ACCEPT_TELEPORT_MENU,
    WARPS_MENU_SIZE,
    CONFIRM_ITEM,
    DENY_ITEM,
    NO_WARP_FOUND_ITEM,
    INSUFFICIENT_PERMISSIONS_ITEM,
    FEATURE_WARP_DURATION,
    FEATURED_LIST_FULL,
    FAVORITE_WARPS_ITEM,
    FREE_FEATURE_POSITION_ITEM,
    WARP_LIST_ITEM,
    SEARCH_WARP_ITEM,
    SORT_WARPS_ITEM,
    MY_WARPS_ITEM,
    HELP_ITEM,
    STAR_REVIEW_ITEM,
    SET_PRICE_ITEM,
    SET_CATEGORY_ITEM,
    CHANGE_DISPLAY_NAME_ITEM,
    CHANGE_PREVIEW_ITEM,
    CHANGE_DESCRIPTION_ITEM,
    CHANGE_ACCESSIBILITY_ITEM,
    RENAME_WARP_ITEM,
    REMOVE_WARP_ITEM,
    RELOCATE_WARP_ITEM,
    CHANGE_OWNER_ITEM,
    NEXT_PAGE_ITEM,
    PREVIOUS_PAGE_ITEM,
    BLOCKED_PLAYERS_ITEM,
    ENABLE_CATEGORIES,
    ENABLE_WARP_SEARCH,
    ENABLE_WARP_RATING,
    CURRENCY_SYMBOL,
    WARP_OVERVIEW_POSITION,
    SET_PRICE_POSITION,
    SET_CATEGORY_POSITION,
    CHANGE_DISPLAY_NAME_POSITION,
    CHANGE_PREVIEW_ITEM_POSITION,
    CHANGE_DESCRIPTION_POSITION,
    CHANGE_ACCESSIBILITY_POSITION,
    REMOVE_WARP_POSITION,
    RENAME_WARP_POSITION,
    RELOCATE_WARP_POSITION,
    CHANGE_OWNER_POSITION,
    CONFIRM_ITEM_POSITIONS,
    DENY_ITEM_POSITIONS,
    BLOCKED_PLAYERS_POSITION,
    BACK_ITEM;

    private static final YamlFile configYamlFile = new YamlFile(
            "config.yml",
            PlayerWarpsPlugin.get().getDataFolder(),
            YamlFile.UpdateMethod.EVERYTIME);
    private static final Map<String, String> strings = new HashMap<>();
    private static final Map<String, List<String>> lists = new HashMap<>();

    static {
        reload();
    }

    public static void reload() {
        configYamlFile.reload();
        final ConfigurationSection configuration = configYamlFile.getConfiguration().getConfigurationSection("config");

        configuration
                .getKeys(false)
                .forEach(key -> {
                    String editedKey = key.toUpperCase(Locale.ENGLISH).replace("-", "_");
                    if (configuration.isList(key)) {
                        lists.put(editedKey, configuration.getStringList(key));
                    } else
                        strings.put(editedKey, configuration.getString(key));
                });

        Lang.reload(LANGUAGE);
    }

    public YamlFile getConfiguration() {
        return null;
    }

    public int asInteger() {
        return Integer.parseInt(strings.get(this.name()));}

    public short asShort() {
        return Short.parseShort(strings.get(this.name()));
    }

    public String asString() {
        return strings.get(this.name());
    }

    public String asReplacedString(Map<String, String> definitions) {
        return TextUtil.replaceString(strings.get(this.name()), definitions);
    }

    public String asUppercase() {
        return this.asString().toUpperCase();
    }

    public boolean asBoolean() {
        return Boolean.parseBoolean(asString());
    }

    public List<String> asList() {
        return lists.get(this.name());
    }

    public List<Integer> asIntList() throws NumberFormatException{
        return lists.get(this.name()).stream().map(Integer::valueOf).collect(Collectors.toList());
    }

    public long asLong() {
        return Long.parseLong(strings.get(this.name()));
    }
}