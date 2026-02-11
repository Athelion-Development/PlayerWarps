package dev.revivalo.playerwarps.warp;

import dev.revivalo.playerwarps.configuration.file.Lang;

public enum WarpStatus {
    OPENED,
    CLOSED,
    PASSWORD_PROTECTED;

    public String getText() {
        return Lang.valueOf(name() + "_STATUS").asColoredString();
    }
}
