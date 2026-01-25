package dev.revivalo.playerwarps.economy;

import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.util.NumberUtil;

public class CustomPrice extends EconomyAction {

    @Override
    public long getPrice(int size) {
        return 0;//NumberUtil.getExpression(Config.WARP_CUSTOM_PRICE.asString().replace("{amount}", "" + size));
    }
}
