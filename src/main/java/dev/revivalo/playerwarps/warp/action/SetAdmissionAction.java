package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

public class SetAdmissionAction implements WarpAction<String> {
    @Override
    public boolean execute(Player player, Warp warp, String input) {
        int price = parseInt(TextUtil.removeColors(input));
        if (price == -1) {
            player.sendMessage(Lang.NOT_A_NUMBER.asColoredString().replace("%input%", input));
            return false;
        }

        if (price < 0) {
            player.sendMessage(Lang.INVALID_ENTERED_PRICE.asColoredString());
            return false;
        }

        if (price > Config.MAX_WARP_ADMISSION.asInteger()) {
            player.sendMessage(Lang.ENTERED_HIGHER_PRICE_THAN_ALLOWED.asColoredString().replace("%max%", Config.MAX_WARP_ADMISSION.asString()));
            return false;
        }

        warp.setAdmission(price);
        player.sendMessage(Lang.PRICE_CHANGED.asColoredString()
                .replace("%warp%", warp.getName())
                .replace("%price%", price == 0
                        ? Lang.FREE_OF_CHARGE.asColoredString()
                        : String.valueOf(price)));

        return true;
    }

    @Override
    public boolean hasInput() {
        return true;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.SET_ADMISSION;
    }

    @Override
    public Lang getMessage() {
        return Lang.PRICE_WRITE_MSG;
    }

    @Override
    public int getFee() {
        return Config.SET_ADMISSION_FEE.asInteger();
    }

    private int parseInt(String str) {
        int number = -1;
        try {
            number = Integer.parseInt(str);
        } catch (NumberFormatException ignored) {

        }

        return number;
    }
}
