package dev.revivalo.playerwarps.menu.page;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.ActionsExecutor;
import dev.revivalo.playerwarps.menu.MenuItem;
import dev.revivalo.playerwarps.util.DateUtil;
import dev.revivalo.playerwarps.util.NumberUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.OpenCategorySelection;
import dev.revivalo.playerwarps.warp.action.OpenStatusSelection;
import dev.revivalo.playerwarps.warp.action.WarpAction;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class ManageMenu extends Menu {
    private final Warp warp;

    private final Gui gui;
    private Player player;

    public ManageMenu(Warp warp) {
        this.warp = warp;
        this.gui = Gui.gui()
                .title(Component.text(getMenuTitle().replace("%warp%", warp.getName())))
                .rows(getRows())
                .disableAllInteractions()
                .create();
    }

    @Override
    public void fill() {
        for (MenuItem<?> item : getTemplate().getItems()) {
            gui.setItem(
                    item.getSlots(),
                    item.draw(getPlaceholders())
                            .asGuiItem(event -> {
                                if (!item.getClickActions().isEmpty()) {
                                    ActionsExecutor.executeActions(player, item.getClickActions());
                                }

                                if (!item.hasAction()) return;
                                final WarpAction<?> action = item.getAction();
                                if (action.hasInput()) {
                                    PlayerWarpsPlugin.getWarpHandler().waitForPlayerInput(
                                            player,
                                            warp,
                                            action
                                    ).thenAccept(input -> {
                                        PlayerWarpsPlugin.get().runSync(() -> {
                                            WarpAction<Object> actionToProceed = (WarpAction<Object>) item.getAction();
                                            actionToProceed.proceed(player, warp, input, new ManageMenu(warp)); // TODO: Manage action only!
                                        });
                                    });
                                } else if (action.hasToBeConfirmed()) {
                                    new ConfirmationMenu(warp)
                                            .open(player, item.getAction());
                                } else if (action instanceof OpenCategorySelection) {
                                    new ChangeTypeMenu(warp).open(player);
                                } else if (action instanceof OpenStatusSelection) {
                                    new SetStatusMenu(warp).open(player);
                                } else {
                                    item.execute(player, warp);
                                }
                            })
            );
        }

        if (getTemplate().getLayout() != null) setLayout(getPlayer(), this);
    }

    private Map<String, String> getPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%warp%", warp.getName());
        //placeholders.put("%icon%", warp.getMenuItem())
        placeholders.put("%player%", player.getName());
        placeholders.put("%category%", warp.getCategory().getName());
        placeholders.put("%creationDate%", DateUtil.getFormatter().format(warp.getDateCreated()));
        placeholders.put("%world%", warp.getLocation().getWorld().getName());
        placeholders.put("%voters%", String.valueOf(warp.getReviewers().size()));
        placeholders.put("%price%", warp.getAdmission() == 0
                ? Lang.FREE_OF_CHARGE.asColoredString()
                : NumberUtil.formatNumber(warp.getAdmission()) + " " + Config.CURRENCY_SYMBOL.asString());
        placeholders.put("%today%", String.valueOf(warp.getTodayVisits()));
        placeholders.put("%status%", warp.getStatus().getText());
        placeholders.put("%ratings%", String.valueOf(NumberUtil.round(warp.getConvertedRating(), 1)));
        placeholders.put("%stars%", TextUtil.createRatingFormat(warp));
        placeholders.put("%lore%", warp.getDescription() == null
                ? Lang.NO_DESCRIPTION.asColoredString()
                : warp.getDescription());
        placeholders.put("%visits%", String.valueOf(warp.getVisits()));
        OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.getOwner());
        placeholders.put("%owner-name%", owner.getName() == null ? "Unknown" : owner.getName());
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
        return TextUtil.colorize(getTemplate().getTitle());
    }

    @Override
    public void open(Player player) {
        this.player = player;

        fill();
        gui.open(player);
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}