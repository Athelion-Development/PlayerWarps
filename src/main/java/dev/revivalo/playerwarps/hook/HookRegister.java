package dev.revivalo.playerwarps.hook;

import dev.revivalo.playerwarps.hook.register.*;

import java.util.*;
import java.util.function.Consumer;

public final class HookRegister {
    private static final Map<Class<? extends Hook<?>>, Hook<?>> hooks = new HashMap<>();

    private HookRegister() {
        throw new RuntimeException("This class cannot be instantiated");
    }

    public static void hook() {
        register(
                new PlaceholderApiHook(),
                new OraxenHook(),
                new ItemsAdderHook(),
                new VaultHook(),
                new EssentialsHook(),
                new DynmapHook(),
                new BentoBoxHook(),
                new ResidenceHook(),
                new SuperiorSkyBlockHook(),
                new WorldGuardHook(),
                new AngeschossenLandsHook(),
                new GriefPreventionHook(),
                new TerritoryHook(),
                new HeadDatabaseHook(),
                new BlueMapHook()
        );

        hooks.values().forEach(Hook::preRegister);
    }

    private static void register(Hook<?>... hookArray) {
        for (Hook<?> hook : hookArray) {
            hooks.put((Class<? extends Hook<?>>) hook.getClass(), hook);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Hook<?>> Optional<T> getHook(Class<T> hookClass) {
        return Optional.ofNullable((T) hooks.get(hookClass));
    }

    /**
     * Provede akci, pokud je hook zapnutý.
     * Použití: HookRegister.ifEnabled(VaultHook.class, vault -> vault.withdraw(player, 100));
     */
    @SuppressWarnings("unchecked")
    public static <T extends Hook<?>> void ifEnabled(Class<T> hookClass, Consumer<T> action) {
        T hook = (T) hooks.get(hookClass);
        if (hook != null && hook.isOn()) {
            action.accept(hook);
        }
    }

    /**
     * Vrátí výsledek, pokud je hook zapnutý, jinak defaultní hodnotu.
     * Použití: boolean result = HookRegister.mapIfEnabled(WorldGuardHook.class, wg -> wg.canBuild(player, loc), true);
     */
    @SuppressWarnings("unchecked")
    public static <T extends Hook<?>, R> R mapIfEnabled(Class<T> hookClass, java.util.function.Function<T, R> mapper, R defaultValue) {
        T hook = (T) hooks.get(hookClass);
        if (hook != null && hook.isOn()) {
            return mapper.apply(hook);
        }
        return defaultValue;
    }

    public static Collection<Hook<?>> getHooks() {
        return Collections.unmodifiableCollection(hooks.values());
    }
}