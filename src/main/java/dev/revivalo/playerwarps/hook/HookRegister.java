package dev.revivalo.playerwarps.hook;

import dev.revivalo.playerwarps.hook.register.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HookRegister {
    private static final Map<HookName, Hook<?>> hooks = new HashMap<>();

    public static void hook() {
        hooks.put(HookName.PLACEHOLDER_API, new PlaceholderApiHook());
        hooks.put(HookName.ORAXEN, new OraxenHook());
        hooks.put(HookName.ITEMS_ADDER, new ItemsAdderHook());
        hooks.put(HookName.VAULT, new VaultHook());
        hooks.put(HookName.ESSENTIALS, new EssentialsHook());
        hooks.put(HookName.DYNMAP, new DynmapHook());
        hooks.put(HookName.BENTO_BOX, new BentoBoxHook());
        hooks.put(HookName.RESIDENCE, new ResidenceHook());
        hooks.put(HookName.SUPERIOR_SKY_BLOCK, new SuperiorSkyBlockHook());
        hooks.put(HookName.WORLD_GUARD, new WorldGuardHook());
        hooks.put(HookName.ANGESCHOSSEN_LANDS, new AngeschossenLandsHook());
        hooks.put(HookName.BLUEMAP, new BlueMapHook());
        hooks.put(HookName.GRIEF_PREVENTION, new GriefPreventionHook());
        hooks.put(HookName.TERRITORY, new TerritoryHook());
        hooks.put(HookName.HEAD_DATABASE, new HeadDatabaseHook());

        for (Hook<?> hook : hooks.values()) {
            hook.preRegister();
        }
    }

    private HookRegister() {
        throw new RuntimeException("This class cannot be instantiated");
    }

    public static <T> boolean isHookEnabled(Hook<T> hook) {
        return hook != null && hook.isOn();
    }

    public static PlaceholderApiHook getPlaceholderApiHook() {
        return (PlaceholderApiHook) hooks.get(HookName.PLACEHOLDER_API);
    }

    public static OraxenHook getOraxenHook() {
        return (OraxenHook) hooks.get(HookName.ORAXEN);
    }

    public static ItemsAdderHook getItemsAdderHook() {
        return (ItemsAdderHook) hooks.get(HookName.ITEMS_ADDER);
    }

    public static VaultHook getVaultHook() {
        return (VaultHook) hooks.get(HookName.VAULT);
    }

    public static EssentialsHook getEssentialsHook() {
        return (EssentialsHook) hooks.get(HookName.ESSENTIALS);
    }

    public static DynmapHook getDynmapHook() {
        return (DynmapHook) hooks.get(HookName.DYNMAP);
    }

    public static GriefPreventionHook getGriefPreventionHook() {
        return (GriefPreventionHook) hooks.get(HookName.GRIEF_PREVENTION);
    }

    public static BentoBoxHook getBentoBoxHook() {
        return (BentoBoxHook) hooks.get(HookName.BENTO_BOX);
    }

    public static ResidenceHook getResidenceHook() {
        return (ResidenceHook) hooks.get(HookName.RESIDENCE);
    }

    public static WorldGuardHook getWorldGuardHook() {
        return (WorldGuardHook) hooks.get(HookName.WORLD_GUARD);
    }

    public static SuperiorSkyBlockHook getSuperiorSkyBlockHook() {
        return (SuperiorSkyBlockHook) hooks.get(HookName.SUPERIOR_SKY_BLOCK);
    }

    public static AngeschossenLandsHook getAngeschossenLands() {
        return (AngeschossenLandsHook) hooks.get(HookName.ANGESCHOSSEN_LANDS);
    }

    public static BlueMapHook getBlueMapHook() {
        return (BlueMapHook) hooks.get(HookName.BLUEMAP);
    }

    public static TerritoryHook getTerritoryHook() {return (TerritoryHook) hooks.get(HookName.TERRITORY);}

    public static HeadDatabaseHook getHeadDatabaseHook() {return (HeadDatabaseHook) hooks.get(HookName.HEAD_DATABASE);}

    public static Collection<Hook<?>> getHooks() {
        return Collections.unmodifiableCollection(hooks.values());
    }

    private enum HookName {
        PLACEHOLDER_API,
        VAULT,
        ORAXEN,
        ITEMS_ADDER,
        ESSENTIALS,
        BENTO_BOX,
        SUPERIOR_SKY_BLOCK,
        RESIDENCE,
        WORLD_GUARD,
        GRIEF_PREVENTION,
        ANGESCHOSSEN_LANDS,
        HEAD_DATABASE,
        DYNMAP,
        BLUEMAP,
        TERRITORY
    }
}
