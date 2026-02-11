package dev.revivalo.playerwarps.menu.page;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpStatus;
import dev.revivalo.playerwarps.warp.action.SetPasswordAction;
import dev.revivalo.playerwarps.warp.action.SetStatusAction;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SetStatusMenu extends Menu {
    private final Warp warp;
    private final Gui gui;
    private Player player;

    public SetStatusMenu(Warp warp) {
        this.warp = warp;
        this.gui = Gui.gui()
                .disableAllInteractions()
                .rows(getRows())
                .title(Component.text(getMenuTitle().replace("%warp%", warp.getName())))
                .create();

        gui.setDefaultClickAction(event -> {
            PlayerWarpsPlugin.get().runSync(() -> new ManageMenu(warp).openFor(player));
        });
    }

    @Override
    public void fill() {
        gui.setItem(12, ItemBuilder.from(Material.BARRIER).setName(Lang.CLOSED_STATUS.asColoredString()).asGuiItem(event -> {
            new SetStatusAction().proceed(player, warp, WarpStatus.CLOSED);
        }));
        gui.setItem(13, ItemBuilder.from(Material.OAK_DOOR).setName(Lang.OPENED_STATUS.asColoredString()).asGuiItem(event -> {
            new SetStatusAction().proceed(player, warp, WarpStatus.OPENED);
        }));
        gui.setItem(14, ItemBuilder.from(Material.IRON_DOOR).setName(Lang.PASSWORD_PROTECTED_STATUS.asColoredString()).asGuiItem(event -> {
            PlayerWarpsPlugin.get().runSync(() -> {
                new InputMenu(warp)
                        .setWarpAction(new SetPasswordAction())
                        .openFor(player);
            });
        }));
    }

    @Override
    public BaseGui getBaseGui() {
        return this.gui;
    }

    @Override
    public short getRows() {
        return 3;
    }

    @Override
    public String getMenuTitle() {
        return Lang.SET_WARP_STATUS_TITLE.asColoredString();
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