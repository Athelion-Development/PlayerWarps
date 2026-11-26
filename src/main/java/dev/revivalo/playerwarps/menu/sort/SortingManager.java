package dev.revivalo.playerwarps.menu.sort;

import dev.revivalo.playerwarps.warp.Warp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SortingManager {
    private final List<Sortable> sortTypes;
    //private final Map<Sortable, List<Warp>> cachedSortedWarps = new HashMap<>();

    public SortingManager(List<Sortable> sortTypes) {
        this.sortTypes = sortTypes;
    }

    public Sortable getDefaultSortType() {
        return sortTypes.getFirst();
    }

    /**
     * Sort types are matched by their kind, not by instance: a menu opened before /pwarp reload
     * still holds a sort type of the previous manager.
     */
    public Sortable nextSortType(Sortable currentSortType) {
        for (int i = 0; i < sortTypes.size(); i++) {
            if (currentSortType != null && sortTypes.get(i).getClass() == currentSortType.getClass()) {
                return sortTypes.get((i + 1) % sortTypes.size());
            }
        }

        // The current type was removed from sort-by in the meantime.
        return getDefaultSortType();
    }

    public void sortWarps(List<Warp> warps, Sortable sortType) {
        sortType.sort(warps);
    }

//    public CompletableFuture<List<Warp>> sortWarpsAsync(List<Warp> warps, Sortable sortType) {
//        return CompletableFuture.supplyAsync(() -> sortWarps(warps, sortType));
//
////        PlayerWarpsPlugin.get().getScheduler().runTaskAsynchronously(PlayerWarpsPlugin.get(), () -> {
////            List<Warp> sorted = new ArrayList<>(warps);
////            sortType.sort(sorted);
////
////            PlayerWarpsPlugin.get().getScheduler().runTask(PlayerWarpsPlugin.get(), () -> {
////                warps.clear();
////                warps.addAll(sorted);
////                callback.run();
////            });
////        });
//    }

//    public void invalidateCache() {
//        cachedSortedWarps.clear();
//    }


    public List<Sortable> getSortTypes() {
        return sortTypes;
    }
}
