package dev.revivalo.playerwarps.menu.page;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.YamlFile;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.menu.ActionsExecutor;
import dev.revivalo.playerwarps.menu.MenuTemplate;
import dev.revivalo.playerwarps.menu.MenuItem;
import dev.revivalo.playerwarps.menu.sort.*;
import dev.revivalo.playerwarps.user.DataSelectorType;
import dev.revivalo.playerwarps.user.User;
import dev.revivalo.playerwarps.user.UserHandler;
import dev.revivalo.playerwarps.warp.WarpManager;
import dev.revivalo.playerwarps.warp.action.Inputable;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Menu {
    protected static final ExecutorService MENU_EXECUTOR = Executors.newFixedThreadPool(1);

    public static final Map<Class<? extends Menu>, MenuTemplate> TEMPLATE_CACHE = new HashMap<>();

    public MenuTemplate getTemplate() {
        return TEMPLATE_CACHE.computeIfAbsent(this.getClass(), clazz -> new MenuTemplate(
                new YamlFile("guis/" + this.getClass().getSimpleName().toLowerCase() + ".yml",
                        PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD)
        )
        );
    }

    public MenuTemplate getLayoutTemplate() {
        return TEMPLATE_CACHE.computeIfAbsent(Menu.class, clazz -> new MenuTemplate(
                new YamlFile("guis/layout.yml",
                        PlayerWarpsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.ON_LOAD)
        )
        );
    }

    public Sortable getNextSortType(Sortable currentSortType) {
        return getWarpHandler().getSortingManager().nextSortType(currentSortType);
    }

    protected WarpManager getWarpHandler() {
        return PlayerWarpsPlugin.getWarpHandler();
    }

    protected Sortable getDefaultSortType() {
        return getWarpHandler().getSortingManager().getDefaultSortType();
    }

    abstract public BaseGui getBaseGui();

    public void openFor(Player player) {
        openFor(player, null);
    }

    public void openFor(Player player, String category) {
        updateMenuHistory(player);

        if (category == null) {
            open(player);
        } else {
            open(player, category);
        }
    }

    private void updateMenuHistory(Player player) {
        //if (this instanceof Inputable) return;

        User user = UserHandler.getUser(player);
        Menu currentMenu = (Menu) user.getData(DataSelectorType.ACTUAL_MENU);

        user.setData(DataSelectorType.PREVIOUS_MENU, currentMenu);
        if (!(this instanceof Inputable)) user.setData(DataSelectorType.ACTUAL_MENU, this);
    }

    public abstract void open(Player player);

    public void open(Player player, String selector) {
        open(player);
    }

    public void reOpen() {
        forceUpdate();
        //open(getPlayer());
        //openFor(getPlayer());
    }

    abstract void fill();

    protected void close() {
        getBaseGui().close(getPlayer());
    }

    protected void clear() {
        getBaseGui().getGuiItems().clear();
    }

    protected void update() {
        fill();
        openFor(getPlayer());
    }

    protected void forceUpdate() {
        clear();
        //getBaseGui().update();
        fill();
        PlayerWarpsPlugin.get().runSync(() -> open(getPlayer()));
    }

    protected void preFill(Menu menu) {
        clear();
        fill();
    }

    protected void setLayout(Player player, Menu menu) {
        if (isPaginated()) {
            final PaginatedGui paginatedGui = (PaginatedGui) menu.getBaseGui();

            for (MenuItem<?> item : getLayoutTemplate().getItems()) {
                paginatedGui.setItem(
                        item.getSlots(),
                        item.draw(getPlaceholders((WarpsMenu) menu))
                                .asGuiItem(event -> {
                                    if (!item.getClickActions().isEmpty()) {
                                        ActionsExecutor.executeActions(player, item.getClickActions(), menu); //TODO: Přidat jako parametr nějaké pole objektů? Pro předávání dat
                                    }

                                })
                );
            }
        }
    }

    private Map<String, String> getPlaceholders(WarpsMenu menu) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%current-alphabetical%", menu.getSortType() instanceof AlphabeticalSort ? Config.SELECTED_SORT.asString() : "");
        placeholders.put("%current-visits%", menu.getSortType() instanceof VisitsSort ? Config.SELECTED_SORT.asString() : "");
        placeholders.put("%current-latest%", menu.getSortType() instanceof LatestSort ? Config.SELECTED_SORT.asString() : "");
        placeholders.put("%current-rating%", menu.getSortType() instanceof RatingSort ? Config.SELECTED_SORT.asString() : "");
        placeholders.put("%next%", getNextSortType(menu.getSortType()).getName().asColoredString());
        return placeholders;
    }

    boolean isPaginated() {
        return this instanceof WarpsMenu;
    }

    abstract short getRows();

    abstract String getMenuTitle();

    abstract Player getPlayer();
}