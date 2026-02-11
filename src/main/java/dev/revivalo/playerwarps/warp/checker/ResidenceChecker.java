package dev.revivalo.playerwarps.warp.checker;

import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidenceManager;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.register.ResidenceHook;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class ResidenceChecker implements Checker {
    private final ResidenceManager residenceManager;
    public ResidenceChecker(ResidenceHook residenceHook) {
        this.residenceManager = residenceHook.getApi().getResidenceManager();
    }

    @Override
    public boolean validate(Player player) {
        Location loc = player.getLocation();
        ClaimedResidence res = residenceManager.getByLoc(loc);
        if (res == null) {
            return true;
        }

        if (!res.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(Lang.TRIED_TO_CREATE_WARP_IN_FOREIGN_RESIDENCE.asColoredString());
            return false;
        }

        return true;
    }
}