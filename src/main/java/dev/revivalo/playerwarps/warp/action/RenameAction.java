package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

public class RenameAction implements WarpAction<String> {
    @Override
    public boolean execute(Player player, Warp warp, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            player.sendMessage(Lang.INVALID_INPUT.asColoredString());
            return false;
        }

        final String name = newName.trim();

        int limit = Config.WARP_NAME_MAX_LENGTH.asInteger();
        if (name.length() > limit) {
            player.sendMessage(Lang.WARP_NAME_IS_ABOVE_LETTERS_LIMIT.asColoredString().replace("%limit%", String.valueOf(limit)));
            return false;
        }

        if (name.contains(" ")) {
            player.sendMessage(Lang.NAME_CANT_CONTAINS_SPACE.asColoredString());
            return false;
        }

        // Warps are resolved by name (/pwarp <name>, manage, remove, placeholders), so a second
        // warp under the same name would hide one of the two behind the other. The warp being
        // renamed is skipped, which keeps a rename that only changes the casing working.
        final boolean nameTaken = getWarpManager().getWarps().stream()
                .anyMatch(other -> !other.equals(warp) && other.getName().equalsIgnoreCase(name));
        if (nameTaken) {
            player.sendMessage(Lang.WARP_ALREADY_CREATED.asColoredString());
            return false;
        }

        // Read before the rename - afterwards getName() already returns the new one.
        final String oldName = warp.getName();

        warp.setName(name);
        player.sendMessage(Lang.WARP_NAME_CHANGED.asColoredString().replace("%oldName%", oldName).replace("%newName%", name));

        return true;
    }

    @Override
    public boolean hasInput() {
        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.RENAME_WARP;
    }

    @Override
    public Lang getMessage() {
        return Lang.RENAME_WRITE_MSG;
    }

    @Override
    public int getFee() {
        return Config.RENAME_WARP_FEE.asInteger();
    }
}
