package dev.revivalo.playerwarps.warp.checker;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.register.TownyAdvancedHook;
import org.bukkit.entity.Player;

public class TownyChecker implements Checker {
    private final TownyAPI townyAPI;
    public TownyChecker(TownyAdvancedHook townyAdvancedHook) {
        this.townyAPI = townyAdvancedHook.getApi();
    }

    @Override
    public boolean validate(Player player) {
        Town town = townyAPI.getTown(player.getLocation());
        if (town == null) {
            return true;
        }

        if (!town.hasResident(player.getName())) {
            player.sendMessage(Lang.TRIED_TO_CREATE_WARP_IN_FOREIGN_TOWN.asColoredString());
            return false;
        }

        return true;
    }
}