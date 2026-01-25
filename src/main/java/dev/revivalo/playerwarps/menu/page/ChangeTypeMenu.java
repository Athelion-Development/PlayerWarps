package dev.revivalo.playerwarps.menu.page;

import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.SetTypeAction;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;

public class ChangeTypeMenu extends Menu {
    private final Warp warp;
    private final Gui gui;
    private Player player;

    public ChangeTypeMenu(Warp warp) {
        this.warp = warp;
        this.gui = Gui.gui()
                .disableAllInteractions()
                .rows(getRows())
                .title(Component.text(getMenuTitle().replace("%warp%", warp.getName())))
                .create();
    }

    @Override
    public void fill() {
        Collection<Category> categories = CategoryManager.getCategories();
        if (!categories.isEmpty()) {
            categories
                    .forEach(category -> gui.addItem(ItemBuilder.from(category.getItem()).lore(Collections.emptyList()).setName(StringUtils.capitalize(category.getType())).asGuiItem(event -> {
                        new SetTypeAction().proceed(player, warp, category, new ManageMenu(warp), 1);
                    })));
        } else {
            gui.setItem(13, ItemBuilder.from(Material.BARRIER).setName(Lang.CATEGORIES_ARE_DISABLED.asColoredString()).asGuiItem());
        }
    }

    @Override
    public BaseGui getBaseGui() {
        return this.gui;
    }

    @Override
    public short getRows() {
        return (short) 3;
    }

    @Override
    public String getMenuTitle() {
        return Lang.CHANGE_WARP_CATEGORY_TITLE.asColoredString();
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
