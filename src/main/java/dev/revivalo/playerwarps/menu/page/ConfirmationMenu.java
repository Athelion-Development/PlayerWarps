package dev.revivalo.playerwarps.menu.page;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.menu.MenuItem;
import dev.revivalo.playerwarps.util.NumberUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.ConfirmAction;
import dev.revivalo.playerwarps.warp.action.WarpAction;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ConfirmationMenu<T> extends Menu {
    private final Warp warp;
    private final Gui gui;
    private T data = null;
    private Player player;
    private WarpAction<T> action;

    private Menu menuToOpen = null;
    private Menu menuToOpenOnCancel = null;

    public ConfirmationMenu(Warp warp) {
        this.warp = warp;
        this.gui = Gui.gui()
                .disableAllInteractions()
                .rows(getRows())
                .title(Component.text(getMenuTitle().replace("%warp%", warp.getName())))
                .create();
    }

    public ConfirmationMenu(Warp warp, T data) {
        this(warp);
        this.data = data;
    }

    @Override
    public void fill() {
        for (MenuItem<?> item : getTemplate().getItems()) {
            gui.setItem(
                    item.getSlots(),
                    item.draw(getPlaceholders())
                            .asGuiItem(event -> {
                                if (item.getAction() instanceof ConfirmAction) {
                                    if (action.hasFee()) {
                                        if (!HookRegister.getVaultHook().getApi().has(player, action.getFee())) {
                                            player.sendMessage(Lang.INSUFFICIENT_BALANCE_FOR_ACTION.asColoredString().replace("%price%", NumberUtil.formatNumber(action.getFee())));
                                            return;
                                        }
                                    }

                                    action.proceed(player, warp, data, menuToOpen, 1, true);
                                } else {
                                    item.execute(player, warp);
                                    if (menuToOpenOnCancel != null) menuToOpenOnCancel.openFor(player);
                                }

                                close();

                                if (menuToOpen != null) {
                                    PlayerWarpsPlugin.get().runDelayed(() -> menuToOpen.openFor(player), 2);
                                }
                            })
            );
        }
    }

    private Map<String, String> getPlaceholders() {
        Map<String,String> placeholders = new HashMap<>();
        placeholders.put("%warp%", warp.getName());
        placeholders.put("%player%", player.getName());
        if (action != null && action.hasFee()) {
            placeholders.put("%price%", NumberUtil.formatNumber(action.getFee()));
        } else {
            placeholders.put("%price%", Lang.FREE_OF_CHARGE.asColoredString());
        }
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
    public void open(Player player) {
        open(player, (WarpAction<T>) null);
    }

    public void open(Player player, WarpAction<T> action) {
        this.player = player;
        this.action = action;

        fill();

        gui.open(player);
    }

    public ConfirmationMenu<T> setMenuToOpen(Menu menu) {
        this.menuToOpen = menu;
        return this;
    }

    @Override
    public String getMenuTitle() {
        return TextUtil.colorize(getTemplate().getTitle());
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public Menu getMenuToOpenOnCancel() {
        return menuToOpenOnCancel;
    }

    public void setMenuToOpenOnCancel(Menu menuToOpenOnCancel) {
        this.menuToOpenOnCancel = menuToOpenOnCancel;
    }
}