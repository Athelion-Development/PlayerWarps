package dev.revivalo.playerwarps.category;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.YamlFile;
import dev.revivalo.playerwarps.util.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;
import java.util.logging.Level;

public class CategoryManager {
    private static HashMap<String, Category> categoriesMap;

    public static void loadCategories() {
        final YamlConfiguration categoriesData = new YamlFile("categories.yml",
                PlayerWarpsPlugin.get().getDataFolder(),
                YamlFile.UpdateMethod.ON_LOAD)
                .getConfiguration();

        final HashMap<String, Category> categories = new HashMap<>();
        final ConfigurationSection categoriesSection = categoriesData.getConfigurationSection("categories");
        categoriesSection
                .getKeys(false)
                .stream().map(categoriesSection::getConfigurationSection).filter(Objects::nonNull).forEach(categorySection ->
                        categories.put(
                                categorySection.getName().toUpperCase(Locale.ENGLISH),
                                new Category(
                                        categorySection.getName(),
                                        categorySection.getBoolean("default"),
                                        categorySection.getString("name"),
                                        TextUtil.colorize(categorySection.getString("display-name")),
                                        categorySection.getString("permission"),
                                        categorySection.getString("item"),
                                        categorySection.getInt("position"),
                                        TextUtil.colorize(categorySection.getStringList("lore"))
                                )
                        ));

        setCategoriesMap(categories);

        if (getDefaultCategory().isEmpty()) {
            // Keys are stored uppercased, so the fallback has to be looked up that way.
            Category fallback = categories.get("ALL");
            if (fallback == null) {
                fallback = categories.values().stream().findFirst().orElse(null);
            }

            if (fallback == null) {
                PlayerWarpsPlugin.get().getLogger().log(Level.WARNING,
                        "No categories are defined in categories.yml, the warps menu will be empty.");
            } else {
                fallback.setDefaultCategory(true);
            }
        }
    }

    public static Category getCategoryFromName(String categoryName) {
        if (categoriesMap == null) {
            PlayerWarpsPlugin.get().getLogger().log(Level.WARNING,
                    "Category '" + categoryName + "' was requested before categories.yml was loaded.");
            return null;
        }

        final Category category = categoriesMap.get((categoryName != null ? categoryName : "all").toUpperCase(Locale.ENGLISH));

        // A warp can reference a category that has since been removed from categories.yml.
        // Falling back keeps getCategory() non-null instead of failing the whole warp.
        return category != null ? category : getDefaultCategory().orElse(null);
    }

    public static Optional<Category> getDefaultCategory() {
        return categoriesMap.values().stream().filter(Category::isDefaultCategory).findFirst();
    }

    public static Optional<Category> getCategory(String categoryType) {
        return getCategories().stream().filter(category -> category.getType().equalsIgnoreCase(categoryType)).findFirst();
    }

    public static boolean isCategory(String categoryName) {
        return getCategories().stream().anyMatch(category -> category.getType().equalsIgnoreCase(categoryName));
    }

    public static Collection<Category> getCategories() {
        return categoriesMap.values();
    }

    public static void setCategoriesMap(HashMap<String, Category> categoriesMap) {
        CategoryManager.categoriesMap = categoriesMap;
    }
}
