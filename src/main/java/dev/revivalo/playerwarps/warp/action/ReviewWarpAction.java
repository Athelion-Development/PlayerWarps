package dev.revivalo.playerwarps.warp.action;

import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.user.UserHandler;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public class ReviewWarpAction implements WarpAction<Integer> {
    @Override
    public boolean execute(Player player, Warp warp, Integer stars) {
        final UUID id = player.getUniqueId();
        boolean result = false;

        if (stars <= 5 && stars >= 1) {
            if (Objects.equals(id, warp.getOwner())) {
                player.sendMessage(Lang.SELF_REVIEW.asColoredString());
            } else if (warp.getReviewers().contains(id)) {
                player.sendMessage(Lang.ALREADY_REVIEWED.asColoredString());
            } else {
                warp.getReviewers().add(id);
                warp.setRating(warp.getRating() + stars);
                warp.setStars(TextUtil.createRatingFormat(warp));
                player.sendMessage(Lang.WARP_REVIEWED.asColoredString()
                        .replace("%warp%", warp.getName())
                        .replace("%stars%", String.valueOf(stars)));
                result = true;
            }
        }

        UserHandler.getUser(player).getPreviousMenu().reOpen();

//        new DefaultWarpsMenu()
//                .setPage((Integer) UserHandler.getUser(player).getData(DataSelectorType.ACTUAL_PAGE))
//                .open(player, "all", getWarpManager().getSortingManager().getDefaultSortType());

        return result;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return PermissionUtil.Permission.REVIEW_WARP;
    }

    @Override
    public boolean isPublicAction() {
        return true;
    }
}
