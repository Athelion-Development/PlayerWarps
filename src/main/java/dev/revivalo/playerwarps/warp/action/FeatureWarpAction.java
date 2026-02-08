package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

import java.util.List;

public class FeatureWarpAction implements WarpAction<Long> {
    @Override
    public boolean execute(Player player, Warp warp, Long data) {
        final List<Warp> featuredWarps = getWarpManager().getFeaturedWarps();
        if (featuredWarps.size() >= Config.MAX_FEATURED_WARPS.asInteger()) {
            player.sendMessage(Lang.FEATURED_LIST_FULL.asColoredString());
            return false;
        }

        if (featuredWarps.contains(warp)) {
            player.sendMessage(Lang.WARP_ALREADY_FEATURED.asColoredString());
            return false;
        }

        warp.setFeaturedTimestamp(System.currentTimeMillis() + (Config.FEATURE_WARP_DURATION.asInteger() * 60L * 1000));

        player.sendMessage(Lang.WARP_FEATURED.asColoredString());

        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.FEATURE;
    }

    @Override
    public int getFee() {
        return Config.FEATURE_WARP_FEE.asInteger();
    }
}
