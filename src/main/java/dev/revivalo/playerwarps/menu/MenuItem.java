package dev.revivalo.playerwarps.menu;

import dev.revivalo.playerwarps.util.ItemUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.WarpAction;
import dev.triumphteam.gui.builder.item.BaseItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MenuItem<T> {
    private final List<Integer> slots;
    private final WarpAction<T> action;
    private final List<ClickAction> clickActions;
    private final T data;

    private final String item;
    private final String name;
    private List<String> lore;

    public MenuItem(ConfigurationSection section, WarpAction<T> action, T data) {
        this.slots = section.getIntegerList("slots");
        this.item = section.getString("item");
        this.name = section.getString("name");
        this.lore = section.getStringList("lore");
        this.clickActions = TextUtil.findAndReturnActions(section.getStringList("actions"));
        this.action = action;
        this.data = data;
    }

    public GuiItem drawFor(Player player) {
        return drawFor(player, null);
    }

    public GuiItem drawFor(Player player, Warp warp) {
        GuiItem drawnItem = drawAs(Collections.emptyMap());
        drawnItem.setAction(event -> {
            action.execute(player, warp, null);
        });
        return drawnItem;
    }

    public GuiItem drawAs() {
        return drawAs(Collections.emptyMap());
    }

    public GuiItem drawAs(Map<String, String> definitions) {
        return draw(definitions).asGuiItem();
    }

    public BaseItemBuilder<?> draw() {
        return draw(Collections.emptyMap());
    }

    public BaseItemBuilder<?> draw(Map<String, String> definitions) {
        return draw(this.item, definitions);
    }

    public BaseItemBuilder<?> draw(String item, Map<String, String> definitions) {
        return ItemUtil.getItem(
                        item
                )
                .setName(TextUtil.colorize(TextUtil.replaceString(name, definitions)))
                .setLore(TextUtil.colorize(TextUtil.replaceList(lore, definitions)));
    }

    public List<Integer> getSlots() {
        return slots;
    }

//    public BaseItemBuilder<?> getItem() {
//        return itemStack;
//    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public boolean hasAction() {
        return action != null;
    }

    public WarpAction<T> getAction() {
        return action;
    }

    public List<ClickAction> getClickActions() {
        return clickActions;
    }

    public void execute(Player player, Warp warp) {
        if (action != null) action.execute(player, warp, data);
    }
}
