package dev.revivalo.playerwarps.util;

import com.cryptomorin.xseries.XMaterial;
import dev.revivalo.playerwarps.hook.CustomItemHook;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.HeadDatabaseHook;
import dev.revivalo.playerwarps.hook.register.ItemsAdderHook;
import dev.revivalo.playerwarps.hook.register.NexoHook;
import dev.revivalo.playerwarps.hook.register.OraxenHook;
import dev.triumphteam.gui.builder.item.BaseItemBuilder;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ItemUtil {
    private static final String CUSTOM_MODEL_PREFIX = "CUSTOMMODEL";
    private static final String CUSTOM_SKULL_MODEL_PREFIX = "CUSTOMSKULL";
    private static final String HEAD_DATABASE_PREFIX = "HDB";

    // Asked in this order, the first plugin knowing an ID wins.
    private static final List<Class<? extends CustomItemHook>> CUSTOM_ITEM_HOOKS =
            List.of(ItemsAdderHook.class, NexoHook.class, OraxenHook.class);

    private ItemUtil() {
        throw new RuntimeException("This class cannot be instantiated");
    }

    public static BaseItemBuilder<?> getItem(String name) {
        return getItem(name, null);
    }

    /**
     * Keeps the whole stack, so custom model data and item models of resource pack items survive.
     * Custom items are rebuilt from their plugin, so a model changed there applies to stored items too.
     */
    public static BaseItemBuilder<?> getItem(ItemStack stack) {
        final ItemStack item = findCustomItemId(stack)
                .flatMap(ItemUtil::findCustomItem)
                .orElse(stack);
        return ItemBuilder.from(item.clone()).flags(ItemFlag.values());
    }

    /**
     * @return a new stack of the ItemsAdder, Nexo or Oraxen item with the given ID
     */
    public static Optional<ItemStack> findCustomItem(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }

        for (Class<? extends CustomItemHook> hookClass : CUSTOM_ITEM_HOOKS) {
            final ItemStack item = HookRegister.mapIfEnabled(hookClass, hook -> hook.getCustomItem(id), null);
            if (item != null) {
                return Optional.of(item.clone());
            }
        }
        return Optional.empty();
    }

    /**
     * @return the ID of the ItemsAdder, Nexo or Oraxen item the stack is an instance of
     */
    public static Optional<String> findCustomItemId(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return Optional.empty();
        }

        for (Class<? extends CustomItemHook> hookClass : CUSTOM_ITEM_HOOKS) {
            final String id = HookRegister.mapIfEnabled(hookClass, hook -> hook.getCustomItemId(stack), null);
            if (id != null) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    public static BaseItemBuilder<?> getItem(String name, Player player) {
        if (name == null || name.equalsIgnoreCase("air")) {
            return ItemBuilder.from(name == null ? Material.STONE : Material.AIR);
        }

        String upper = name.toUpperCase(Locale.ENGLISH);

        if (upper.startsWith(CUSTOM_MODEL_PREFIX)) {
            return parseCustomModel(name);
        }

        if (upper.startsWith(CUSTOM_SKULL_MODEL_PREFIX) && player != null) {
            return parseCustomSkullModel(name, player);
        }

        if (upper.startsWith(HEAD_DATABASE_PREFIX)) {
            return parseHeadDatabase(name);
        }

        if (name.length() > 64) {
            return parseTexture(name);
        }

        if (name.equalsIgnoreCase("skullofplayer") && player != null) {
            return ItemBuilder.skull().owner(player);
        }

        return fromPluginOrVanilla(name);
    }

    private static BaseItemBuilder<?> parseCustomModel(String name) {
        String materialStr = extractBetween(name, '[', ']');
        int data = Integer.parseInt(extractBetween(name, '{', '}'));
        return ItemBuilder.from(Material.valueOf(materialStr.toUpperCase(Locale.ENGLISH))).model(data);
    }

    private static BaseItemBuilder<?> parseCustomSkullModel(String name, Player player) {
        int data = Integer.parseInt(extractBetween(name, '{', '}'));
        return ItemBuilder.skull().owner(player).model(data);
    }

    private static BaseItemBuilder<?> parseHeadDatabase(String name) {
        String itemData = name.substring(4);

        return HookRegister.mapIfEnabled(HeadDatabaseHook.class, hook -> {
            if (itemData.equalsIgnoreCase("random")) {
                return ItemBuilder.from(hook.getRandomHead());
            }

            ItemStack head = hook.getHead(itemData);
            Objects.requireNonNull(head, "HeadDatabase head '" + itemData + "' is invalid!");
            return ItemBuilder.from(head);
        }, ItemBuilder.from(Material.PLAYER_HEAD));
    }

    private static BaseItemBuilder<?> parseTexture(String name) {
        if (name.startsWith("@")) {
            return ItemBuilder.skull().texture(name.substring(1)).model(1);
        }
        return ItemBuilder.skull().texture(name);
    }

    private static BaseItemBuilder<?> fromPluginOrVanilla(String name) {
        // ItemsAdder, Nexo, Oraxen
        Optional<ItemStack> customItem = findCustomItem(name);
        if (customItem.isPresent()) return ItemBuilder.from(customItem.get());

        // Vanilla / XMaterial fallback
        Material material = Material.matchMaterial(name);
        if (material == null) {
            material = XMaterial.matchXMaterial(name)
                    .orElse(XMaterial.STONE)
                    .parseMaterial();
            Objects.requireNonNull(material);
        }
        return ItemBuilder.from(material).flags(ItemFlag.values());
    }

    private static String extractBetween(String str, char open, char close) {
        return str.substring(str.indexOf(open) + 1, str.indexOf(close));
    }
}