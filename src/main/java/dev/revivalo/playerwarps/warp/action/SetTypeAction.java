package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class SetTypeAction implements WarpAction<Category> {
    @Override
    public boolean execute(Player player, Warp warp, @Nullable Category category) {
        if (Optional.ofNullable(category).isEmpty()) {
            player.sendMessage(Lang.ENTERED_INVALID_TYPE_OF_WARP.asReplacedString(player, new HashMap<String, String>() {{
                put("%types%", CategoryManager.getCategories().stream().map(Category::getType).collect(Collectors.joining(", ")));
            }}));
            return false;
        }

        warp.setCategory(category);
        player.sendMessage(Lang.WARP_TYPE_CHANGED.asColoredString().
                replace("%warp%", warp.getName()).
                replace("%type%", category.getType())
        );

        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.SET_WARP_TYPE;
    }

    @Override
    public int getFee() {
        return Config.SET_TYPE_FEE.asInteger();
    }
}
