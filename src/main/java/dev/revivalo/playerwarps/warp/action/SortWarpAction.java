//package dev.revivalo.playerwarps.warp.action;
//
//import dev.revivalo.playerwarps.PlayerWarpsPlugin;
//import dev.revivalo.playerwarps.menu.page.WarpsMenu;
//import dev.revivalo.playerwarps.menu.sort.Sortable;
//import dev.revivalo.playerwarps.util.PermissionUtil;
//import dev.revivalo.playerwarps.warp.Warp;
//import org.bukkit.entity.Player;
//
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//public class SortWarpAction implements WarpAction<SortWarpAction.Pair> {
//    @Override
//    public boolean execute(Player player, Warp warp, Pair data) {
//        CompletableFuture<List<Warp>> future = PlayerWarpsPlugin.getWarpHandler().getSortingManager().sortWarpsAsync(data.getWarpsToSort(), data.getSortable());
//        future.thenAccept(sortedWarps -> {
//            new WarpsMenu.DefaultWarpsMenu()
//                    .open(player, "default", data.getSortable(), sortedWarps);
//        });
//        return true;
//    }
//
//    @Override
//    public PermissionUtil.Permission getPermission() {
//        return null;
//    }
//
//    public static class Pair {
//        private final List<Warp> warpsToSort;
//        private final Sortable sortable;
//
//        public Pair(List<Warp> warpsToSort, Sortable sortable) {
//            this.warpsToSort = warpsToSort;
//            this.sortable = sortable;
//        }
//
//        public List<Warp> getWarpsToSort() {
//            return warpsToSort;
//        }
//
//        public Sortable getSortable() {
//            return sortable;
//        }
//    }
//}
