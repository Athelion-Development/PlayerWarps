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

    public Sortable nextSortType(Sortable currentSortType) {
        int currentIndex = sortTypes.indexOf(currentSortType);

        if (currentIndex == -1) {
            throw new IllegalArgumentException("Invalid current sort type: " + currentSortType.getName());
        }

        int nextIndex = (currentIndex + 1) % sortTypes.size();
        return sortTypes.get(nextIndex);
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
