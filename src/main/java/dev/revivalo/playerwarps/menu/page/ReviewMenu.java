package dev.revivalo.playerwarps.menu.page;

import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.MenuItem;
import dev.revivalo.playerwarps.user.User;
import dev.revivalo.playerwarps.user.UserHandler;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.Inputable;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ReviewMenu extends Menu implements Inputable {
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

        gui.setCloseGuiAction(event -> {
            final User user = UserHandler.getUser(player);
            if (user.getPreviousMenu() != null && !(user.getPreviousMenu() instanceof ReviewMenu)) {
//                PlayerWarpsPlugin.get().getLogger().info("Oteviram " + user.getPreviousMenu());
//                PlayerWarpsPlugin.get().runDelayed(() -> user.getPreviousMenu().reOpen(), 3);
            }
        });
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
        //PlayerWarpsPlugin.get().getLogger().info("opening " + UserHandler.getUser(player).getData(DataSelectorType.PREVIOUS_MENU));
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public Lang getInputText() {
        return null;
    }
}
