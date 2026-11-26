package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.ItemUtil;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class SetPreviewItemAction implements WarpAction<String> {
    private static volatile Set<Material> bannedItems;
    @Override
    public boolean execute(Player player, Warp warp, String item) {
        try {
            final boolean fromHand = item.equalsIgnoreCase("HAND");
            final ItemStack handItem = player.getInventory().getItemInMainHand();

            // ItemsAdder, Nexo and Oraxen items are rebuilt from their plugin instead of copying the
            // held stack, so its renames, enchantments or contents never end up in data.yml.
            final String customItemId = fromHand ? ItemUtil.findCustomItemId(handItem).orElse(null) : item;
            final Optional<ItemStack> customItem = ItemUtil.findCustomItem(customItemId);

            ItemStack displayItem;
            if (customItem.isPresent()) {
                displayItem = customItem.get();
                displayItem.setAmount(1);
            } else if (fromHand) {
                displayItem = new ItemStack(handItem.getType());
            } else {
                displayItem = new ItemStack(Material.valueOf(item.toUpperCase()));
            }

            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(ItemFlag.values());
                displayItem.setItemMeta(meta);
            }

            final String itemName = customItem.isPresent()
                    ? customItemId
                    : displayItem.getType().name().toLowerCase(Locale.ENGLISH);

            // A custom item only borrows its base material, so the material ban list does not apply to it.
            if (customItem.isEmpty() && getBannedItems().contains(displayItem.getType())) {
                player.sendMessage(Lang.TRIED_TO_SET_BANNED_ITEM.asColoredString());
                return false;
            } else {
                if (!player.hasPermission("playerwarps.icon.*")) {
                    // ItemsAdder IDs are "namespace:id", which becomes playerwarps.icon.namespace.id
                    String iconPermission = "playerwarps.icon." + itemName.toLowerCase(Locale.ENGLISH).replace(':', '.');
                    if (!player.hasPermission(iconPermission)) {
                        player.sendMessage(Lang.INSUFFICIENT_PERMISSIONS.asColoredString().replace("%permission%", iconPermission));
                        return false;
                    }
                }

                warp.setMenuItem(displayItem);
                player.sendMessage(Lang.ITEM_CHANGED.asColoredString().replace("%item%", itemName));
            }
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Lang.INVALID_ITEM.asColoredString());
            return false;
        }

        return true;
    }

    @Override
    public boolean hasInput() {
        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.SET_PREVIEW_ITEM;
    }

    @Override
    public Lang getMessage() {
        return Lang.ITEM_WRITE_MSG;
    }

    @Override
    public int getFee() {
        return Config.SET_PREVIEW_ITEM_FEE.asInteger();
    }

    private static Set<Material> getBannedItems() {
        if (bannedItems == null) {
            reloadBannedItems();
        }

        return bannedItems;
    }

    /**
     * Re-reads banned-items. An unknown material is reported and skipped - failing here would
     * break setting a warp icon until the next restart.
     */
    public static void reloadBannedItems() {
        final Set<Material> materials = EnumSet.of(Material.NETHER_PORTAL, Material.END_PORTAL, Material.AIR);

        final List<String> configured = Config.BANNED_ITEMS.asList();
        if (configured != null) {
            for (String materialName : configured) {
                final Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    PlayerWarpsPlugin.get().getLogger().warning("Unknown material '" + materialName + "' in banned-items, skipping it.");
                    continue;
                }

                materials.add(material);
            }
        }

        bannedItems = materials;
    }
}