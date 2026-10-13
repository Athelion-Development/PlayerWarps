package dev.revivalo.playerwarps.warp.action;

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

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SetPreviewItemAction implements WarpAction<String> {
    private static final Set<Material> BANNED_ITEMS;
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
            if (customItem.isEmpty() && BANNED_ITEMS.contains(displayItem.getType())) {
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

    static {
        BANNED_ITEMS = new HashSet<>();
        BANNED_ITEMS.add(Material.NETHER_PORTAL);
        BANNED_ITEMS.add(Material.END_PORTAL);
        BANNED_ITEMS.add(Material.AIR);
        BANNED_ITEMS.addAll(Config.BANNED_ITEMS.asList().stream().map(Material::valueOf).collect(Collectors.toList()));
    }
}