package dev.revivalo.playerwarps.menu.page;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIBuilder;
import de.rapha149.signgui.exception.SignGUIVersionException;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.SearchWarpAction;
import dev.revivalo.playerwarps.warp.action.WarpAction;
import dev.triumphteam.gui.guis.BaseGui;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collections;

public class InputMenu extends Menu {
    private final Warp warp;
    private final SignGUIBuilder gui;
    private Player player;
    private WarpAction<String> warpAction;

    public InputMenu(Warp warp) {
        this.warp = warp;
        try {
            this.gui = SignGUI.builder()
                    .setType(Material.OAK_SIGN)
                    .setColor(DyeColor.BLACK)
                    .setHandler((p, result) -> {
                        String input = result.getLineWithoutColor(0);

                        warpAction.proceed(player, warp, input);

                        return Collections.emptyList();
                    });
        } catch (SignGUIVersionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fill() {
        // nothing to fill
    }

    @Override
    public BaseGui getBaseGui() {
        return null;
    }


    @Override
    public short getRows() {
        return -1;
    }

    @Override
    public String getMenuTitle() {
        return "";
    }


    @Override
    public void open(Player player) {
        this.player = player;

        gui
                .setLine(1, warpAction instanceof SearchWarpAction
                        ? Lang.ENTER_WARPS_NAME.asColoredString()
                        : Lang.ENTER_PASSWORD.asColoredString())
                .build()
                .open(player);
    }

    public InputMenu setWarpAction(WarpAction<String> warpAction) {
        this.warpAction = warpAction;
        return this;
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
