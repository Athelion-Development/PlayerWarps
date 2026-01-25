package dev.revivalo.playerwarps.configuration.file;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.YamlFile;
import dev.revivalo.playerwarps.util.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public enum Lang {
    PREFIX,
    PASSWORD_CHANGED,
    PASSWORD_TOO_SHORT,
    INVALID_INPUT,
    ENTER_PASSWORD,
    ENTERED_WRONG_PASSWORD,
    ENTER_WARPS_NAME,
    CATEGORIES_ARE_DISABLED,
    ENTERED_HIGHER_PRICE_THAN_ALLOWED,
    ACCEPT_TELEPORT_WITH_ADMISSION,
    TELEPORT_TO_WARP_WITH_ADMISSION,
    HELP_MESSAGE_LORE,
    HELP_DISPLAY_NAME,
    HELP_LORE,
    OWN_WARP_ITEM_NAME,
    RELOAD_MESSAGE,
    WARP_CREATION_NOTIFICATION,
    WARP_VISIT_NOTIFICATION,
    WAIT_BEFORE_NEXT_ACTION,
    WARP_SUBMITTED_FOR_APPROVAL,
    WARP_APPROVED,
    WARP_DENIED,
    WARP_FEATURED,
    WARP_ALREADY_FEATURED,
    FEATURED_LIST_FULL,

    LIMIT_REACHED,
    LIMIT_REACHED_OTHER,
    NO_WARP_FOUND,
    NO_WARP_AT_POSITION,
    FREE_FEATURE_POSITION,
    FREE_FEATURE_POSITION_LORE,
    ALREADY_OWNING,
    ALPHABETICAL,
    LATEST,
    VISITS,
    RATING,
    NON_EXISTING_WARP,
    INSUFFICIENT_PERMS_FOR_CATEGORY,
    INSUFFICIENT_PERMS_FOR_CATEGORY_LORE,
    WARP_ALREADY_CREATED,
    WARP_CREATED,
    CLICK_TO_CONFIGURE,
    CLICK_TO_CANCEL_INPUT,
    WARP_CREATED_WITH_PRICE,
    INSUFFICIENT_BALANCE_FOR_ACTION,
    INSUFFICIENT_BALANCE_TO_TELEPORT,
    WARP_ACCESS_BLOCKED,
    TELEPORTATION_UNSAFE,
    WARP_REMOVED,
    WARP_REMOVED_WITH_REFUND,
    WARP_IN_DELETED_WORLD,
    TRIED_TO_CREATE_WARP_IN_FOREIGN_ISLAND,
    TRIED_TO_CREATE_WARP_IN_FOREIGN_CLAIM,
    TRIED_TO_CREATE_WARP_IN_FOREIGN_REGION,
    TRIED_TO_CREATE_WARP_IN_FOREIGN_RESIDENCE,
    TRIED_TO_CREATE_WARP_IN_FOREIGN_LAND,
    TELEPORTATION,
    TELEPORTATION_CANCELLED,
    DANGEROUS_POSITION_MSG,
    TELEPORT_TO_WARP,
    NAME_CANT_CONTAINS_SPACE,
    INSUFFICIENT_PERMISSIONS,
    PASSWORD_LENGTH,
    ITEM_CHANGED,
    RENAME_WRITE_MSG,
    SET_DESCRIPTION_MSG,
    ITEM_WRITE_MSG,
    CANCEL_INPUT,
    INPUT_CANCELLED,
    TEXT_SIZE_ERROR,
    DESCRIPTION_CHANGED,
    NOT_OWNING,
    INVALID_ITEM,
    ALREADY_REVIEWED,
    INVALID_REVIEW,
    SELF_REVIEW,
    WARP_REVIEWED,
    TRIED_TO_SET_BANNED_ITEM,
    NOT_A_NUMBER,
    PRICE_WRITE_MSG,
    PRICE_CHANGED,
    INVALID_ENTERED_PRICE,
    WARPS_ITEM_NAME,
    MY_WARPS_ITEM_NAME,
    REVIEW_WARP_TITLE,
    FAVORITE_WARPS_ITEM_NAME,
    MY_WARP_TITLE,
    WARPS_TITLE,
    EDIT_WARP_MENU_TITLE,
    FAVORITES_TITLE,
    CONFIRMATION_MENU_TITLE,
    ACCEPT,
    ACCEPT_WITH_PRICE,
    DENY,
    OPENED_STATUS,
    CLOSED_STATUS,
    PASSWORD_PROTECTED_STATUS,
    SORT_WARPS,
    CLICK_TO_SORT_BY,
    NEXT_PAGE,
    PREVIOUS_PAGE,
    NO_DESCRIPTION,
    BACK_NAME,
    ONE_STARS,
    TWO_STARS,
    THREE_STARS,
    FOUR_STARS,
    FIVE_STARS,
    SEARCH_WARP,
    SEARCH_WARP_LORE,
    WARP_IS_DISABLED,
    ENTERED_INVALID_TYPE_OF_WARP,
    WARP_TYPE_CHANGED,
    BAD_COMMAND_SYNTAX,
    FAVORITE_WARP_ADDED,
    REMOVE_FAVORITE_WARP,
    WARP_NOT_CONTAINS_FAVLIST,
    WARP_ALREADY_CONTAINS_FAVLIST,
    OWNERSHIP_TRANSFER_SUCCESSFUL,
    OWNERSHIP_TRANSFER_INFO,
    UNAVAILABLE_PLAYER,
    OWNER_CHANGE_MSG,
    PASSWORD_CHANGE_MSG,
    WARP_NAME_CHANGED,
    WARPS_STATUS_CHANGED,
    WARP_RELOCATED,
    WARP_RELOCATION,
    WARP_RELOCATION_LORE,
    TRIED_TO_CREATE_WARP_IN_DISABLED_WORLD,
    TRIED_TO_RELOCATE_WARP_TO_DISABLED_WORLD,
    WARP_NAME_IS_ABOVE_LETTERS_LIMIT,
    CATEGORY_TITLE,
    CHANGE_WARP_CATEGORY_TITLE,
    SET_WARP_STATUS_TITLE,
    FREE_OF_CHARGE,
    REMOVE_WARP,
    CHANGE_TYPE,
    CHANGE_ITEM,
    ITEM_FROM_HAND,
    CHANGE_LABEL,
    CANT_BLOCK_YOURSELF,
    BLOCKED_PLAYER_INPUT,
    BLOCKED_PLAYERS_TITLE,
    BLOCKED_PLAYERS,
    BLOCKED_PLAYERS_LORE,
    BLOCKED_PLAYER_MANAGE,
    BLOCKED_PLAYER_MANAGE_LORE,
    BLOCKED_PLAYER_ADD,
    CHANGE_DISPLAY_NAME,
    DISPLAY_NAME_CHANGED,
    CHANGE_DISPLAY_NAME_LORE,
    WRITE_NEW_DISPLAY_NAME,
    PWARP_ACCESSIBILITY,
    SET_PRICE,
    CHANGE_OWNER,
    RENAME_WARP,
    WARP_LORE,
    OWN_WARP_LORE,
    REMOVE_WARP_LORE,
    CHANGE_TYPE_LORE,
    CHANGE_ITEM_LORE,
    CHANGE_LABEL_LORE,
    PWARP_ACCESSIBILITY_LORE,
    SET_PRICE_LORE,
    CHANGE_OWNER_LORE,
    RENAME_WARP_LORE,
    TELEPORTATION_PROGRESS;

    private static final Map<String, String> messages = new HashMap<>();
    private static final Map<String, List<String>> lists = new HashMap<>();

//    public static void reload(Config language) {
//        YamlFile langYamlFile = new YamlFile("lang/" + language.asString() + ".yml",
//                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.EVERYTIME);
//
//        langYamlFile.reload();
//        final YamlConfiguration configuration = langYamlFile.getConfiguration();
//
//        ConfigurationSection langSection = configuration.getConfigurationSection("lang");
//        if (langSection == null) {
//            PlayerWarpsPlugin.get().getLogger().info("Invalid configuration in " + langYamlFile.getFilePath());
//            return;
//        }
//
//        langSection
//                .getKeys(false)
//                .forEach(key -> {
//                    String editedKey = key.toUpperCase(Locale.ENGLISH).replace("-", "_");
//                    if (langSection.isList(key)) {
//                        lists.put(editedKey, langSection.getStringList(key));
//                    } else
//                        messages.put(editedKey, Objects.requireNonNull(langSection.getString(key)).replace("%prefix%", Lang.PREFIX.asColoredString()));
//                });
//    }

    public static void reload(Config language) {
        YamlFile langYamlFile = new YamlFile("lang/" + language.asString() + ".yml",
                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.EVERYTIME);
        langYamlFile.reload();
        final YamlConfiguration configuration = langYamlFile.getConfiguration();

        YamlConfiguration defaultConfig = new YamlConfiguration();
        try (InputStream defaultLangStream = PlayerWarpsPlugin.get().getResource("lang/English.yml")) {
            if (defaultLangStream == null) {
                PlayerWarpsPlugin.get().getLogger().severe("Default language file 'English.yml' not found in jar!");
                return;
            }
            defaultConfig.load(new InputStreamReader(defaultLangStream));
        } catch (Exception e) {
            PlayerWarpsPlugin.get().getLogger().severe("Failed to load default language file from jar!");
            e.printStackTrace();
            return;
        }

        ConfigurationSection defaultLangSection = defaultConfig.getConfigurationSection("lang");
        ConfigurationSection langSection = configuration.getConfigurationSection("lang");

        if (defaultLangSection == null) {
            PlayerWarpsPlugin.get().getLogger().severe("Invalid default language configuration in jar!");
            return;
        }

        if (langSection == null) {
            PlayerWarpsPlugin.get().getLogger().severe("Invalid configuration in " + langYamlFile.getFilePath());
            return;
        }

        defaultLangSection.getKeys(true).forEach(defaultKey -> {
            if (!langSection.contains(defaultKey)) {
                langSection.set(defaultKey, defaultLangSection.get(defaultKey));
            }
        });

        langYamlFile.save();

        langSection.getKeys(false).forEach(key -> {
            String editedKey = key.toUpperCase(Locale.ENGLISH).replace("-", "_");
            if (langSection.isList(key)) {
                lists.put(editedKey, langSection.getStringList(key));
            } else {
                messages.put(editedKey, Objects.requireNonNull(langSection.getString(key))
                        .replace("%prefix%", Lang.PREFIX.asColoredString()));
            }
        });
    }


    public List<String> asReplacedList() {
        return TextUtil.colorize(TextUtil.replaceList(lists.get(this.name()), Collections.emptyMap()));
    }

    public List<String> asReplacedList(final Map<String, String> definitions) {
        return TextUtil.colorize(TextUtil.replaceList(lists.get(this.name()), definitions));
    }

    public List<String> asReplacedList(Player player, final Map<String, String> definitions) {
        return TextUtil.colorize(TextUtil.replaceList(lists.get(this.name()), definitions));
    }

    public String asColoredString() {return asColoredString(null);}

    public String asColoredString(Player player) {
        return TextUtil.colorize(messages.get(this.name()));
    }

    public String asReplacedString(Map<String, String> definitions) {
        return TextUtil.colorize(TextUtil.replaceString(messages.get(this.name()), definitions));
    }

    public String asReplacedString(Player player, Map<String, String> definitions) {
        return TextUtil.colorize(TextUtil.replaceString(messages.get(this.name()), definitions));
    }
}