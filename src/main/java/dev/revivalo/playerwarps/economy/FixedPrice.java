package dev.revivalo.playerwarps.economy;


import dev.revivalo.playerwarps.configuration.file.Config;

public class FixedPrice extends EconomyAction {

    @Override
    public long getPrice(int unused) {
        return Config.WARP_FIXED_PRICE.asInteger();
    }
}
