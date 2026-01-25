package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.menu.WarpSearch;
import dev.revivalo.playerwarps.menu.page.WarpsMenu;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SearchWarpAction implements WarpAction<String> {
    @Override
    public boolean execute(Player player, Warp warp, String input) {
        final List<Warp> warps = PlayerWarpsPlugin.getWarpHandler().getWarps().stream()
                .filter(Warp::isAccessible).collect(Collectors.toList());

        WarpSearch warpSearch = new WarpSearch(warps);
        CompletableFuture<List<Warp>> future = warpSearch.searchAsync(input);
        List<Warp> warpList = new ArrayList<>();

        future.thenAccept(warpList::addAll).thenRun(warpSearch::shutdown);

        try {
            PlayerWarpsPlugin.get().runSync(() ->
                    new WarpsMenu.DefaultWarpsMenu()
                    .open(player, "all", PlayerWarpsPlugin.getWarpHandler().getSortingManager().getDefaultSortType(), warpList));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.USE;
    }

    @Override
    public Lang getInputText() {
        return Lang.ENTER_WARPS_NAME;
    }

    @Override
    public boolean isPublicAction() {
        return true;
    }
}