package dev.revivalo.playerwarps.menu;

import dev.revivalo.playerwarps.configuration.YamlFile;
import dev.revivalo.playerwarps.warp.action.WarpAction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class MenuTemplate {
    private final YamlFile configurationFile;
    private final Map<String, MenuItem<?>> items = new HashMap<>();

    private String layout;
    private String title;
    private int rows;
    private int pageSize;

    public MenuTemplate(YamlFile configurationFile) {
        this.configurationFile = configurationFile;
        final FileConfiguration config = configurationFile.getConfiguration();

        cache();

        ConfigurationSection contentSection = config.getConfigurationSection("content");
        if (contentSection == null) return;

        for (String key : contentSection.getKeys(false)) {
            ConfigurationSection itemSection = contentSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            MenuActionType type = MenuActionType.fromString(itemSection.isSet("type") ? itemSection.getString("type") : "NONE");
            WarpAction<?> action = type.createAction();

            MenuItem<?> item = new MenuItem(itemSection, action, itemSection.get("data"));
            items.put(key, item);
        }
    }

    public YamlFile getConfigurationFile() {
        return configurationFile;
    }

    public void reload() {
        getConfigurationFile().reload();
        cache();
    }

    private void cache() {
        final FileConfiguration config = configurationFile.getConfiguration();
        this.title = config.getString("title", "&7Menu");
        this.rows = config.getInt("rows", 3);
        this.pageSize = config.getInt("page-size");
        this.layout = config.getString("layout", null);

        ConfigurationSection contentSection = configurationFile.getConfiguration().getConfigurationSection("content");
        if (contentSection == null) return;

        for (String key : contentSection.getKeys(false)) {
            ConfigurationSection itemSection = contentSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            MenuActionType type = MenuActionType.fromString(itemSection.isSet("type") ? itemSection.getString("type") : "NONE");
            WarpAction<?> action = type.createAction();

            MenuItem<?> item = new MenuItem(itemSection, action, itemSection.get("data"));
            items.put(key, item);
        }
    }

    public List<MenuItem<?>> getItems() {
        final List<MenuItem<?>> menuItems = new ArrayList<>(items.values().stream().toList());
        Collections.reverse(menuItems);
        return menuItems;
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getLayout() {
        return layout;
    }
}
