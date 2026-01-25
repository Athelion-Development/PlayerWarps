package dev.revivalo.playerwarps.menu;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.YamlFile;
import dev.revivalo.playerwarps.menu.page.*;

import java.util.HashMap;
import java.util.Map;

public class MenuRegister {
    private static final Map<Class<? extends Menu>, YamlFile> menuFiles = new HashMap<>();

    public static void registerFiles() {
        Menu.TEMPLATE_CACHE.put(CategoriesMenu.class, new MenuTemplate(new YamlFile("guis/" + CategoriesMenu.class.getSimpleName().toLowerCase() + ".yml",
                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD)));

        Menu.TEMPLATE_CACHE.put(ConfirmationMenu.class, new MenuTemplate(new YamlFile("guis/" + ConfirmationMenu.class.getSimpleName().toLowerCase() + ".yml",
                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD)));

        Menu.TEMPLATE_CACHE.put(ManageMenu.class, new MenuTemplate(new YamlFile("guis/" + ManageMenu.class.getSimpleName().toLowerCase() + ".yml",
                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD)));

//        menuFiles.put(WarpsMenu.class, new YamlFile("guis/" + WarpsMenu.class.getSimpleName().toLowerCase() + ".yml",
//                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD));


        Menu.TEMPLATE_CACHE.put(WarpsMenu.DefaultWarpsMenu.class, new MenuTemplate(new YamlFile("guis/" + WarpsMenu.DefaultWarpsMenu.class.getSimpleName().toLowerCase() + ".yml",
                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD)));

        Menu.TEMPLATE_CACHE.put(WarpsMenu.FavoriteWarpsMenu.class, new MenuTemplate(new YamlFile("guis/" + WarpsMenu.FavoriteWarpsMenu.class.getSimpleName().toLowerCase() + ".yml",
                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD)));

        Menu.TEMPLATE_CACHE.put(WarpsMenu.MyWarpsMenu.class, new MenuTemplate(new YamlFile("guis/" + WarpsMenu.MyWarpsMenu.class.getSimpleName().toLowerCase() + ".yml",
                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD)));

        Menu.TEMPLATE_CACHE.put(ReviewMenu.class, new MenuTemplate(new YamlFile("guis/" + ReviewMenu.class.getSimpleName().toLowerCase() + ".yml",
                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD)));

        Menu.TEMPLATE_CACHE.put(Menu.class, new MenuTemplate(new YamlFile("guis/layout.yml",
                PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD)));
    }
}