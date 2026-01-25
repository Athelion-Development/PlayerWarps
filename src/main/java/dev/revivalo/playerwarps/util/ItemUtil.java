package dev.revivalo.playerwarps.util;

import com.cryptomorin.xseries.XMaterial;
import dev.lone.itemsadder.api.CustomStack;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.triumphteam.gui.builder.item.BaseItemBuilder;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import io.th0rgal.oraxen.api.OraxenItems;
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

    public static BaseItemBuilder<?> getItem(String name) {
        return getItem(name, null);
    }

    public static BaseItemBuilder<?> getItem(ItemStack stack) {
        return getItem(stack.getType().name(), null);
    }

    public static BaseItemBuilder<?> getItem(String name, Player player) {
        if (name == null) {
            return ItemBuilder.from(Material.STONE);
        }

        if (name.equalsIgnoreCase("air")) {
            return ItemBuilder.from(Material.AIR);
        }

        //UltimateRewardsPlugin.get().getLogger().log(Level.INFO, name + " " + CustomStack.isInRegistry(name) + "/" + HookRegister.isHookEnabled(HookRegister.getItemsAdderHook()));

        String itemNameInUppercase = name.toUpperCase(Locale.ENGLISH);
        if (itemNameInUppercase.startsWith(CUSTOM_MODEL_PREFIX)) {
            String materialStr = name.substring(name.indexOf('[') + 1, name.indexOf(']'));
            String dataStr = name.substring(name.indexOf('{') + 1, name.indexOf('}'));

            Material material = Material.valueOf(materialStr.toUpperCase(Locale.ENGLISH));
            int data = Integer.parseInt(dataStr);

            return ItemBuilder.from(material).model(data);
        } else if (itemNameInUppercase.startsWith(CUSTOM_SKULL_MODEL_PREFIX) && player != null) {
            String dataStr = name.substring(name.indexOf('{') + 1, name.indexOf('}'));
            int data = Integer.parseInt(dataStr);

            return ItemBuilder.skull().owner(player).model(data);
        } else if (itemNameInUppercase.startsWith(HEAD_DATABASE_PREFIX) && HookRegister.getHeadDatabaseHook().isOn()) {
            String itemData = name.substring(4); // Strip hdb: from the string
            if (itemData.equalsIgnoreCase("random")) {
                return ItemBuilder.from(HookRegister.getHeadDatabaseHook().getRandomHead());
            } else {
                ItemStack head = HookRegister.getHeadDatabaseHook().getHead(itemData);
                Objects.requireNonNull(head, "Head Database head " + itemData + " is invalid!");
                return ItemBuilder.from(head);
            }
        } else if (name.length() > 64) {
            if (name.startsWith("@")) {
                return ItemBuilder.skull().texture(name.replace("@", "")).model(1);
            } else {
                return ItemBuilder.skull().texture(name);
            }
        } else if (name.equalsIgnoreCase("skullofplayer") && player != null) {
            return ItemBuilder.skull().owner(player); // TODO: Rework
        } else if (HookRegister.isHookEnabled(HookRegister.getItemsAdderHook()) && CustomStack.isInRegistry(name)) {
            return ItemBuilder.from(CustomStack.getInstance(name).getItemStack());
        } else if (HookRegister.getOraxenHook().isOn() && OraxenItems.exists(name)) {
            return ItemBuilder.from(OraxenItems.getItemById(name).build());
//        } else if (HookRegister.getNexoHook().isOn() && NexoI) {
//
//        }
        } else {
            Material material = Material.matchMaterial(name);
            if (material == null) {
                material = Objects.requireNonNull(XMaterial.matchXMaterial(name).orElse(XMaterial.STONE).parseMaterial());
            }
            return ItemBuilder.from(material).flags(ItemFlag.values());
        }
    }
}