package dev.revivalo.playerwarps.util;

import com.cryptomorin.xseries.XMaterial;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.hook.register.HeadDatabaseHook;
import dev.revivalo.playerwarps.hook.register.ItemsAdderHook;
import dev.revivalo.playerwarps.hook.register.OraxenHook;
import dev.triumphteam.gui.builder.item.BaseItemBuilder;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Objects;

public final class ItemUtil {
    private static final String CUSTOM_MODEL_PREFIX = "CUSTOMMODEL";
    private static final String CUSTOM_SKULL_MODEL_PREFIX = "CUSTOMSKULL";
    private static final String HEAD_DATABASE_PREFIX = "HDB";

    private ItemUtil() {
        throw new RuntimeException("This class cannot be instantiated");
    }

    public static BaseItemBuilder<?> getItem(String name) {
        return getItem(name, null);
    }

    public static BaseItemBuilder<?> getItem(ItemStack stack) {
        return getItem(stack.getType().name(), null);
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
        // ItemsAdder
        BaseItemBuilder<?> result = HookRegister.mapIfEnabled(ItemsAdderHook.class, hook -> {
            ItemStack stack = hook.getCustomItem(name);
            return stack != null ? ItemBuilder.from(stack) : null;
        }, null);
        if (result != null) return result;

        // Oraxen
        result = HookRegister.mapIfEnabled(OraxenHook.class, hook -> {
            ItemStack stack = hook.getCustomItem(name);
            return stack != null ? ItemBuilder.from(stack) : null;
        }, null);
        if (result != null) return result;

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