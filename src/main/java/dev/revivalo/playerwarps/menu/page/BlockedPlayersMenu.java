package dev.revivalo.playerwarps.menu.page;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.ItemUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.BlockPlayerAction;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BlockedPlayersMenu extends Menu {
    private final Warp warp;

    private Player player;
    private Gui gui;

    public BlockedPlayersMenu(Warp warp) {
        this.warp = warp;
        this.gui = Gui.gui()
                .disableAllInteractions()
                .rows(getRows())
                .title(Component.text(getMenuTitle().replace("%amount%", String.valueOf(warp.getBlockedPlayers().size()))))
                .create();
    }

    @Override
    public void fill() {
        for (UUID uuid : warp.getBlockedPlayers()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            gui.addItem(ItemBuilder
                    .from(Material.PLAYER_HEAD)
                    .setName(Lang.BLOCKED_PLAYER_MANAGE.asColoredString().replace("%player%", offlinePlayer.getName() == null ? "Unknown" : offlinePlayer.getName()))
                    .setLore(Lang.BLOCKED_PLAYER_MANAGE_LORE.asReplacedList())
                    .asGuiItem(event -> {
                        warp.unblock(offlinePlayer);
                        update();
                    }));
        }

        gui.addItem(ItemBuilder
                .from(Material.CONDUIT)
                .setName(Lang.BLOCKED_PLAYER_ADD.asColoredString())
                .asGuiItem(event -> {
                    BlockPlayerAction blockPlayerAction = new BlockPlayerAction();
                    PlayerWarpsPlugin.getWarpHandler()
                            .waitForPlayerInput(player, warp, blockPlayerAction)
                            .thenAccept(input -> blockPlayerAction.proceed(player, warp, input, new BlockedPlayersMenu(warp)));
                }));

        gui.setItem(18, ItemUtil.getItem(Config.BACK_ITEM.asUppercase())
                .setName(Lang.BACK_NAME.asColoredString())
                .asGuiItem(event -> new ManageMenu(warp).open(player)));

        gui.open(player);
    }

    @Override
    public Player getPlayer() {
        return player;
    }

//    @Override
//    public MenuType getMenuType() {
//        return MenuType.BLOCKED_PLAYERS_MENU;
//    }

    @Override
    public BaseGui getBaseGui() {
        return this.gui;
    }

    @Override
    public short getRows() {
        return 27;
    }

    @Override
    public String getMenuTitle() {
        return Lang.BLOCKED_PLAYERS_TITLE.asColoredString();
    }

    @Override
    public void open(Player player) {
        this.player = player;

        fill();

        gui.open(player);
    }
}