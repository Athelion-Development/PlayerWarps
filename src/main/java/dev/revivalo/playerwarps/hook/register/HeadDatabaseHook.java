package dev.revivalo.playerwarps.hook.register;

import dev.revivalo.playerwarps.hook.Hook;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class HeadDatabaseHook implements Hook<Void> {
    private HeadDatabaseAPI headDatabaseAPI;
    private boolean isHooked = false;

    @Override
    public @NotNull String getName() {
        return "HeadDatabase";
    }

    @Override
    public void register() {
        if (!isPluginEnabled()) {
            isHooked = false;
            return;
        }

        this.headDatabaseAPI = new HeadDatabaseAPI();
        isHooked = true;
    }

    @Override
    public boolean isOn() {
        return isHooked;
    }

    @Override
    public @Nullable Void getApi() {
        return null;
    }

    public @Nullable ItemStack getHead(@NotNull String id) {
        Objects.requireNonNull(headDatabaseAPI, "Failed to get head. HeadDatabase hook has failed!");
        return headDatabaseAPI.getItemHead(id);
    }

    public @NotNull ItemStack getRandomHead() {
        Objects.requireNonNull(headDatabaseAPI, "Failed to get random head. HeadDatabase hook has failed!");
        return headDatabaseAPI.getRandomHead();
    }
}
