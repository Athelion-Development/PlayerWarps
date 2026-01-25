package dev.revivalo.playerwarps.menu;


import dev.revivalo.playerwarps.warp.action.*;

import java.util.function.Supplier;

public enum MenuActionType {
    REVIEW(ReviewWarpAction::new),
    CONFIRM(ConfirmAction::new),
    RETURN(ReturnAction::new),

    OPEN_MAIN_MENU(OpenMainMenu::new),

    SEARCH_WARP(SearchWarpAction::new),

    SET_ADMISSION(SetAdmissionAction::new),
    SET_DISPLAYNAME(ChangeDisplayNameAction::new),
    SET_DESCRIPTION(SetDescriptionAction::new),
    SET_PREVIEW_ITEM(SetPreviewItemAction::new),
    OPEN_CATEGORY_SELECTION(OpenCategorySelection::new),
    SET_CATEGORY(SetTypeAction::new),
    OPEN_STATUS_SELECTION(OpenStatusSelection::new),
    SET_STATUS(SetStatusAction::new),
    RELOCATE(RelocateAction::new),
    TRANSFER(TransferOwnershipAction::new),
    REMOVE(RemoveWarpAction::new),
    RENAME(RenameAction::new),
    SHOW_FEATURED(ShowFeaturedWarp::new),
    FEATURE(FeatureWarpAction::new),

    NONE(() -> null);

    private final Supplier<? extends WarpAction<?>> actionSupplier;

    MenuActionType(Supplier<? extends WarpAction<?>> actionSupplier) {
        this.actionSupplier = actionSupplier;
    }

    public WarpAction<?> createAction() {
        return actionSupplier.get();
    }

    public static MenuActionType fromString(String name) {
        try {
            return MenuActionType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}
