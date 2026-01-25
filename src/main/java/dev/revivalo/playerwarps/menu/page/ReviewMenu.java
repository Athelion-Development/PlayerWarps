package dev.revivalo.playerwarps.menu.page;

import dev.revivalo.playerwarps.menu.MenuItem;
import dev.revivalo.playerwarps.warp.Warp;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ReviewMenu extends Menu {
    private final Warp warp;
    private final Gui gui;
    private Player player;

    public ReviewMenu(Warp warp) {
        this.warp = warp;
        this.gui = Gui.gui()
                .disableAllInteractions()
                .rows(getRows())
                .title(Component.text(getMenuTitle().replace("%warp%", warp.getName())))
                .create();
    }

    @Override
    public void fill() {
        for (MenuItem<?> item : getTemplate().getItems()) {
            gui.setItem(
                    item.getSlots(),
                    item.draw()
                            .asGuiItem(event -> {
                                item.execute(player, warp);
                            })
            );
        }
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
    public void open(Player player) {
        this.player = player;
        preFill(this);
        gui.open(player);
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
