package dev.revivalo.playerwarps.menu.page;

import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.ActionsExecutor;
import dev.revivalo.playerwarps.menu.MenuItem;
import dev.revivalo.playerwarps.util.DateUtil;
import dev.revivalo.playerwarps.util.ItemUtil;
import dev.revivalo.playerwarps.util.NumberUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.PreTeleportToWarpAction;
import dev.revivalo.playerwarps.warp.action.SaveWarpAction;
import dev.revivalo.playerwarps.warp.action.ShowFeaturedWarp;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriesMenu extends Menu {
    private final Gui gui;
    private Player player;

    public CategoriesMenu() {
        this.gui = Gui.gui()
                .rows(getRows())
                .title(Component.text(getMenuTitle()))
                .disableAllInteractions()
                .create();
    }

    @Override
    public void fill() {
        int pos = 0;
        for (MenuItem<?> item : getTemplate().getItems()) {
            GuiItem guiItem;

            if (item.getAction() instanceof ShowFeaturedWarp) {
                try {
                    Warp warp = getWarpHandler().getFeaturedWarps().get(pos);
                    List<String> lore = Lang.WARP_LORE.asReplacedList(player, new HashMap<>(getPlaceholders()) {{
                                put("%creationDate%", DateUtil.getFormatter().format(warp.getDateCreated()));
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
                                put("%owner-name%", warp.getOwnerName());
                            }}
                    );

                    guiItem = ItemUtil.getItem(warp.getMenuItem() == null
                                    ? Config.DEFAULT_WARP_ITEM.asString()//, warp.getOwner())
                                    : warp.getMenuItem().getType().name())
                            .setName(TextUtil.colorize(Config.WARP_NAME_FORMAT.asReplacedString(new HashMap<>(getPlaceholders()) {{
                                put("%warpName%", warp.getDisplayName());
                            }})))
                            .setLore(TextUtil.colorize(TextUtil.insertListIntoList(
                                            lore,
                                            TextUtil.splitByWords(warp.getDescription() == null ? Lang.NO_DESCRIPTION.asColoredString() : warp.getDescription(), 5, Config.WARP_DESCRIPTION_COLOR.asString())
                                    )
                            )).asGuiItem(event -> {
                                switch (event.getClick()) {
                                    case LEFT:
                                    case SHIFT_LEFT:
                                        player.closeInventory();
                                        new PreTeleportToWarpAction().setMenuToOpen(this).proceed(player, warp);
                                        break;
                                    case RIGHT:
                                    case SHIFT_RIGHT:
                                        if (Config.ENABLE_WARP_RATING.asBoolean()) {
                                            //user.setData(DataSelectorType.ACTUAL_PAGE, page);
                                            new ReviewMenu(warp).openFor(player);
                                        }
                                        break;
                                    case SWAP_OFFHAND:
                                        //Menu actualMenu = (Menu) user.getData(DataSelectorType.ACTUAL_MENU);
                                        new SaveWarpAction().proceed(player, warp, null);
                                        break;
                                }
                            });
                } catch (IndexOutOfBoundsException ignore) {
                    guiItem = item.draw(getPlaceholders()).asGuiItem(event -> {
                        if (!item.getClickActions().isEmpty()) {
                            ActionsExecutor.executeActions(player, item.getClickActions(), this);
                        }
                    });
                }

                ++pos;
            } else {
                guiItem = item.draw(getPlaceholders()).asGuiItem(event -> {
                    if (!item.getClickActions().isEmpty()) {
                        ActionsExecutor.executeActions(player, item.getClickActions(), this);
                    }
                });
            }

            gui.setItem(item.getSlots(), guiItem);
        }
    }


    @Override
    public void open(Player player) {
        this.player = player;

        fill();

        gui.open(player);
    }

    private Map<String, String> getPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        for (Category category : CategoryManager.getCategories()) {
            placeholders.put("%amount_" + category.getType() + "%", getWarpHandler().getCountOfWarps(category) + "");
        }
        placeholders.put("%amount_saved%", getWarpHandler().getPlayerFavoriteWarps(player).size() + "");
        placeholders.put("%amount_owned%", getWarpHandler().getPlayerWarps(player).size() + "");

        return placeholders;
    }

    @Override
    public BaseGui getBaseGui() {
        return this.gui;
    }

    @Override
    public short getRows() {
        return (short) getTemplate().getRows();
    }

    @Override
    public String getMenuTitle() {
        return getTemplate().getTitle();
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}