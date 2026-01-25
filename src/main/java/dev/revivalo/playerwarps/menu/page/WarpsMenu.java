package dev.revivalo.playerwarps.menu.page;

import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.ActionsExecutor;
import dev.revivalo.playerwarps.menu.MenuItem;
import dev.revivalo.playerwarps.menu.sort.Sortable;
import dev.revivalo.playerwarps.user.DataSelectorType;
import dev.revivalo.playerwarps.user.User;
import dev.revivalo.playerwarps.user.UserHandler;
import dev.revivalo.playerwarps.util.*;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.SaveWarpAction;
import dev.revivalo.playerwarps.warp.action.PreTeleportToWarpAction;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WarpsMenu extends Menu {
    private int page = 1;
    private final PaginatedGui paginatedGui;
    private Player player;
    private String categoryName;
    private Sortable sortType;
    private List<Warp> foundWarps;

    private final Set<Player> sortingCooldowns = new HashSet<>();

    public WarpsMenu() {
        this.paginatedGui = Gui.paginated()
                .pageSize(getTemplate().getPageSize())
                .rows(getTemplate().getRows())
                .title(Component.text(getTemplate().getTitle().replace("%page%", String.valueOf(page))))
                .disableAllInteractions()
                .create();
    }

    @Override
    public void fill(/*List<Warp> foundWarps*/) {
        final User user = UserHandler.getUser(player);
//        user.addData(DataSelectorType.ACTUAL_PAGE, paginatedGui.getCurrentPageNum());
//        user.addData(DataSelectorType.ACTUAL_MENU, this);
//        PlayerWarpsPlugin.get().getLogger().info("Ukladam " + categoryName);
//        if (categoryName != null && !categoryName.equalsIgnoreCase("all")) user.addData(DataSelectorType.SELECTED_CATEGORY, categoryName);
//        user.addData(DataSelectorType.SELECTED_SORT, sortType);

        final Category openedCategory = CategoryManager.getCategoryFromName(categoryName);

        final List<Warp> warps = (foundWarps == null) ? new ArrayList<>() : foundWarps;

        if (warps.isEmpty() && foundWarps == null) {
            switch (this) {
                case DefaultWarpsMenu ignored -> {
                    if (openedCategory == null || openedCategory.isDefaultCategory()) {
                        warps.addAll(getWarpHandler().getWarps().stream()
                                .filter(Warp::isAccessible)
                                .toList());
                    } else {
                        warps.addAll(getWarpHandler().getWarps().stream()
                                .filter(warp -> warp.isAccessible() && (warp.getCategory() == null || warp.getCategory().getType().equalsIgnoreCase(categoryName)))
                                .toList());
                    }

                    getWarpHandler().getSortingManager().sortWarps(warps, sortType); //TODO: Async
                }
                case MyWarpsMenu ignored ->
                        warps.addAll(getWarpHandler().getWarps().stream().filter(warp -> warp.isOwner(player)).toList());
                case FavoriteWarpsMenu ignored ->
                        warps.addAll(getWarpHandler().getPlayerFavoriteWarps(player));
                default -> {
                }
            }
        }

        for (MenuItem<?> item : getTemplate().getItems()) {
            getBaseGui().setItem(
                    item.getSlots(),
                    item.draw()
                            .asGuiItem(event -> {
                                if (!item.getClickActions().isEmpty()) {
                                    ActionsExecutor.executeActions(player, item.getClickActions());
                                }
                            })
            );
        }

        //getWarpHandler().getSortingManager().sortWarps(warps, sortType);
        //new SortWarpAction().execute(player, null, new SortWarpAction.Pair(warps, sortType));

        final Lang warpLore = this instanceof MyWarpsMenu ? Lang.OWN_WARP_LORE : Lang.WARP_LORE;

        GuiItem guiItem;
        if (warps.isEmpty()) { //TODO: When empty - add info that no warps are found
//            if (this instanceof MyWarpsMenu && Config.ENABLE_HINTS.asBoolean()) {
//                guiItem = new GuiItem(
//                        ItemUtil.getItem(Config.HELP_ITEM.asUppercase())
//                                .setName(Lang.HELP_DISPLAY_NAME.asColoredString())
//                                .setLore(Lang.HELP_LORE.asReplacedList())
//                                .build()
//                );
//                paginatedGui.setItem(Config.WARP_LISTING_MENU_SIZE.asInteger() - 32, guiItem);
//            } else {
//                guiItem = new GuiItem(
//                        ItemUtil.getItem(Config.NO_WARP_FOUND_ITEM.asUppercase())
//                                .setName(Lang.NO_WARP_FOUND.asColoredString())
//                                .build()
//                );
//                paginatedGui.setItem(Config.WARP_LISTING_MENU_SIZE.asInteger() - 32, guiItem);
//            }
        } else {
            for (Warp warp : warps) {
                if (warp.getLocation() == null) {
                    guiItem = new GuiItem(ItemBuilder
                            .from(Material.BARRIER)
                            .setName(TextUtil.colorize(warp.getDisplayName()))
                            .setLore(Lang.WARP_IN_DELETED_WORLD.asColoredString())
                            .build());
                } else {
                    List<String> lore = warpLore.asReplacedList(player, new HashMap<>() {{
                                put("%creationDate%", DateUtil.getFormatter().format(warp.getDateCreated()));
                                put("%category%", warp.getCategory().getDisplayName());
                                put("%world%", warp.getLocation().getWorld().getName());
                                put("%voters%", String.valueOf(warp.getReviewers().size()));
                                put("%price%", warp.getAdmission() == 0
                                        ? Lang.FREE_OF_CHARGE.asColoredString()
                                        : NumberUtil.formatNumber(warp.getAdmission()) + " " + Config.CURRENCY_SYMBOL.asString());
                                put("%today%", String.valueOf(warp.getTodayVisits()));
                                put("%status%", warp.getStatus().getText());
                                put("%ratings%", String.valueOf(NumberUtil.round(warp.getConvertedRating(), 1)));
                                put("%stars%", TextUtil.createRatingFormat(warp));
//                                        put("%lore%", warp.getDescription() == null
//                                                ? Lang.NO_DESCRIPTION.asColoredString()
//                                                : TextUtil.splitLoreIntoLines(warp.getDescription(), 5));
                                put("%visits%", String.valueOf(warp.getVisits()));
                                put("%owner-name%", Bukkit.getOfflinePlayer(warp.getOwner()).getName() == null ? "Unknown" : Bukkit.getOfflinePlayer(warp.getOwner()).getName());
                            }}
                    );

                    guiItem = ItemUtil.getItem(warp.getMenuItem() == null
                                    ? Config.DEFAULT_WARP_ITEM.asString()//, warp.getOwner())
                                    : warp.getMenuItem().getType().name())
                            .setName(TextUtil.colorize(Config.WARP_NAME_FORMAT.asString().replace("%warpName%", warp.getDisplayName())))
                            .setLore(TextUtil.colorize(TextUtil.insertListIntoList(
                                            lore,
                                            TextUtil.splitByWords(warp.getDescription() == null ? Lang.NO_DESCRIPTION.asColoredString() : warp.getDescription(), 5, Config.WARP_DESCRIPTION_COLOR.asString())
                                    )
                            )).asGuiItem();

                    guiItem.setAction(event -> {
                        if (getWarpHandler().areWarps()) {
                            switch (event.getClick()) {
                                case LEFT:
                                case SHIFT_LEFT:
                                    player.closeInventory();
                                    new PreTeleportToWarpAction().setMenuToOpen(this).proceed(player, warp);
                                    break;
                                case RIGHT:
                                case SHIFT_RIGHT:
                                    if (this instanceof MyWarpsMenu) {
                                        if (!player.hasPermission("playerwarps.settings")) {
                                            player.sendMessage(Lang.INSUFFICIENT_PERMISSIONS.asColoredString().replace("%permission%", "playerwarps.settings"));
                                            return;
                                        }
                                        new ManageMenu(warp).open(player);
                                    } else {
                                        if (Config.ENABLE_WARP_RATING.asBoolean()) {
                                            user.setData(DataSelectorType.ACTUAL_PAGE, page);
                                            new ReviewMenu(warp).open(player);
                                        }
                                    }
                                    break;
                                case SWAP_OFFHAND:
                                    Menu actualMenu = (Menu) user.getData(DataSelectorType.ACTUAL_MENU);
                                    new SaveWarpAction().proceed(player, warp, null, this instanceof FavoriteWarpsMenu ? actualMenu : null, page);
                                    break;
                            }
                        }
                    });
                }

                paginatedGui.addItem(guiItem);
            }
        }
    }

    private boolean canSort(Player player) {
        return !sortingCooldowns.contains(player);
    }

    @Override
    public BaseGui getBaseGui() {
        return this.paginatedGui;
    }

    @Override
    public short getRows() {
        return (short) getTemplate().getRows();
    }

    @Override
    public String getMenuTitle() {
        return TextUtil.colorize(getTemplate().getTitle());
    }

    @Override
    public void open(Player player) {
        open(player, "all", getWarpHandler().getSortingManager().getDefaultSortType(), null);
    }

    public void open(Player player, String categoryName) {
        open(player, categoryName, getDefaultSortType());
    }

    public void open(Player player, String categoryName, Sortable sortType) {
        open(player, categoryName, sortType, null);
    }

    public void open(Player player, String categoryName, Sortable sortType, List<Warp> foundWarps) {
        this.player = player;
        this.categoryName = categoryName;
        this.sortType = sortType;
        this.foundWarps = foundWarps;

        sortingCooldowns.add(player);

        paginatedGui.clearPageItems();

        CompletableFuture.runAsync(this::fill, MENU_EXECUTOR).thenAccept(unused -> {
            if (getTemplate().getLayout() != null) setLayout(getPlayer(), this);
            sortingCooldowns.remove(player);
            getBaseGui().update();
        });

        paginatedGui.open(player);
    }

    public WarpsMenu setPage(int page) {
        this.page = page;
        return this;
    }

    public int getPage() {
        return page;
    }

    public PaginatedGui getPaginatedGui() {
        return paginatedGui;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Sortable getSortType() {
        return sortType;
    }

    public List<Warp> getFoundWarps() {
        return foundWarps;
    }

    public Set<Player> getSortingCooldowns() {
        return sortingCooldowns;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public static class MyWarpsMenu extends WarpsMenu {

        @Override
        public String getMenuTitle() {
            return TextUtil.colorize(getTemplate().getTitle());
        }

        @Override
        public short getRows() {
            return (short) getTemplate().getRows();
        }
    }

    public static class FavoriteWarpsMenu extends WarpsMenu {

        @Override
        public String getMenuTitle() {
            return TextUtil.colorize(getTemplate().getTitle());
        }

        @Override
        public short getRows() {
            return (short) getTemplate().getRows();
        }
    }

    public static class DefaultWarpsMenu extends WarpsMenu {

        @Override
        public String getMenuTitle() {
            return TextUtil.colorize(getTemplate().getTitle());
        }

        @Override
        public short getRows() {
            return (short) getTemplate().getRows();
        }
    }
}
