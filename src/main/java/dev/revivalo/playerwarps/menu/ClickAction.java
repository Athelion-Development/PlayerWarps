package dev.revivalo.playerwarps.menu;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ClickAction {
    @NotNull
    private final String statement;
    @NotNull
    private final ActionType actionType;

    ClickAction(@NotNull String statement, @NotNull ActionType actionType) {
        this.statement = statement;
        this.actionType = actionType;
    }

    @NotNull
    public static RewardActionBuilder builder() {
        return new RewardActionBuilder();
    }

    @NotNull
    public String getStatement() {
        return this.statement;
    }

    @NotNull
    public ActionType getActionType() {
        return this.actionType;
    }

    public static class RewardActionBuilder {
        @Nullable
        private String statement;
        @Nullable
        private ActionType actionType;

        @NotNull
        public RewardActionBuilder setStatement(@NotNull String statement) {
            this.statement = statement;
            return this;
        }

        @NotNull
        public RewardActionBuilder setAction(@NotNull ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        @NotNull
        public ClickAction build() {
            String statement = Objects.requireNonNull(this.statement, "statement cannot be null");
            ActionType actionType = Objects.requireNonNull(this.actionType, "actionType cannot be null");
            return new ClickAction(statement, actionType);
        }
    }
}
