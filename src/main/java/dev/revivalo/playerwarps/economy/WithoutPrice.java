package dev.revivalo.playerwarps.economy;

public class WithoutPrice extends EconomyAction {

    @Override
    public long getPrice(int size) {
        return 0;
    }
}
