package dev.revivalo.playerwarps.user;

import dev.revivalo.playerwarps.menu.page.Menu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class User {
    private final Player player;
    private Map<DataSelectorType, Object> data;
    private Menu actualMenu;
    private Menu previousMenu;

    public User(Player player, Map<DataSelectorType, Object> data) {
        this.player = player;
        this.data = data;
    }

    public Map<DataSelectorType, Object> getData() {
        return data;
    }

    public Object getData(DataSelectorType selection) {
        return data.get(selection);
    }

    public User addData(DataSelectorType key, Object object) {
        // if (key == WA) TODO: Logic for previous and actual opened menu
        data.put(key, object);
        return this;
    }

    public void setData(DataSelectorType type, Object data) {
        setData(new HashMap<>(){{put(type, data);}});
    }

    public void setData(Map<DataSelectorType, Object> data) {
        this.data = data;
    }

    public Player getPlayer() {
        return player;
    }
}
