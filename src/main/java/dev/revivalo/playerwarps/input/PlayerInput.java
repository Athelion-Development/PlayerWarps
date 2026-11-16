package dev.revivalo.playerwarps.input;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.util.Debug;
import dev.revivalo.playerwarps.menu.page.Menu;
import dev.revivalo.playerwarps.menu.page.WarpsMenu;
import dev.revivalo.playerwarps.util.TextUtil;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.action.Inputable;
import dev.revivalo.playerwarps.warp.action.WarpAction;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Single entry point for asking a player to type a value in. The mode is picked by the
 * {@code input-mode} config option.
 */
public final class PlayerInput {
    private static final long CHAT_TIMEOUT_TICKS = 60 * 20;

    /**
     * The chat input each player is being asked for. Only touched on the main thread.
     */
    private static final Map<UUID, CompletableFuture<String>> PENDING_CHAT_INPUTS = new HashMap<>();

    private PlayerInput() {
        throw new RuntimeException("This class cannot be instantiated");
    }

    public static InputMode getMode() {
        return InputMode.fromString(Config.INPUT_MODE.asString());
    }

    /**
     * Asks for the input an action needs.
     *
     * @param returnMenu menu to reopen when the player cancels the input, or null
     */
    public static CompletableFuture<String> request(Player player, @Nullable Warp warp, WarpAction<?> action,
                                                    @Nullable Menu returnMenu) {
        return request(player, warp, resolvePrompt(action), returnMenu);
    }

    /**
     * Asks for a plain value that does not belong to a warp action, such as a warp password.
     */
    public static CompletableFuture<String> request(Player player, Lang prompt, @Nullable Menu returnMenu) {
        return request(player, null, prompt, returnMenu);
    }

    private static CompletableFuture<String> request(Player player, @Nullable Warp warp, @Nullable Lang prompt,
                                                     @Nullable Menu returnMenu) {
        final InputMode mode = getMode();

        Debug.log("Input requested for %s: configured input-mode=%s, resolved=%s, prompt=%s",
                player.getName(), Config.INPUT_MODE.asString(), mode, prompt);

        return switch (mode) {
            case SIGN -> requestViaSign(player, prompt, warp, returnMenu);
            case MODAL -> requestViaModal(player, warp, prompt, returnMenu);
            default -> requestViaChat(player, warp, prompt, returnMenu);
        };
    }

    /**
     * Reopens the menu the input was requested from. Warp listings are reopened with the
     * category, sorting and search results they had, which their plain open() would reset.
     */
    private static void reopen(Player player, Menu menu) {
        PlayerWarpsPlugin.get().runSync(() -> {
            if (menu instanceof WarpsMenu warpsMenu) {
                warpsMenu.open(player, warpsMenu.getCategoryName(), warpsMenu.getSortType(), warpsMenu.getFoundWarps());
            } else {
                menu.openFor(player);
            }
        });
    }

    @Nullable
    private static Lang resolvePrompt(WarpAction<?> action) {
        if (action.getMessage() != null) {
            return action.getMessage();
        }

        return action instanceof Inputable inputable ? inputable.getInputText() : null;
    }

    private static String promptText(@Nullable Lang prompt, @Nullable Warp warp) {
        if (prompt == null) {
            return Lang.INVALID_INPUT.asColoredString();
        }

        final String text = prompt.asColoredString();
        return warp == null ? text : text.replace("%warp%", warp.getName());
    }

    private static CompletableFuture<String> requestViaSign(Player player, @Nullable Lang prompt,
                                                            @Nullable Warp warp, @Nullable Menu returnMenu) {
        final CompletableFuture<String> future = new CompletableFuture<>();

        final SignGUI gui;
        try {
            gui = SignGUI.builder()
                    .setType(Material.OAK_SIGN)
                    .setColor(DyeColor.BLACK)
                    .setLine(1, promptText(prompt, null))
                    .setHandler((signPlayer, result) -> {
                        final String input = result.getLineWithoutColor(0);

                        // An empty sign is the sign equivalent of clicking "cancel" in chat.
                        if (input.isEmpty()) {
                            future.cancel(false);
                            if (returnMenu != null) {
                                reopen(player, returnMenu);
                            }

                            return Collections.emptyList();
                        }

                        // Guarantees the same contract as the chat mode: callers continue on
                        // the main thread and may open menus straight away.
                        PlayerWarpsPlugin.get().runSync(() -> future.complete(input));

                        return Collections.emptyList();
                    })
                    .build();
        } catch (SignGUIVersionException ex) {
            PlayerWarpsPlugin.get().getLogger().warning(
                    "Sign input is not supported on this server version, using chat input instead.");
            return requestViaChat(player, warp, prompt, returnMenu);
        }

        gui.open(player);
        return future;
    }

    private static CompletableFuture<String> requestViaModal(Player player, @Nullable Warp warp,
                                                             @Nullable Lang prompt, @Nullable Menu returnMenu) {
        final CompletableFuture<String> future = new CompletableFuture<>();

        // A dialog cannot share the screen with an open container - without this the window is
        // closed again the moment it appears.
        player.closeInventory();

        // Dialog labels are plain components, so legacy color codes would show up literally.
        final boolean opened = ModalInput.open(
                player,
                TextUtil.removeColors(Lang.MODAL_INPUT_TITLE.asColoredString()),
                TextUtil.removeColors(promptText(prompt, warp)),
                TextUtil.removeColors(Lang.MODAL_INPUT_SUBMIT.asColoredString()),
                input -> PlayerWarpsPlugin.get().runSync(() -> future.complete(input)));

        if (!opened) {
            Debug.log("Modal dialog unavailable, using the chat input for %s.", player.getName());
            return requestViaChat(player, warp, prompt, returnMenu);
        }

        Debug.log("Modal dialog opened for %s.", player.getName());
        return future;
    }

    private static CompletableFuture<String> requestViaChat(Player player, @Nullable Warp warp,
                                                            @Nullable Lang prompt, @Nullable Menu returnMenu) {
        final CompletableFuture<String> future = new CompletableFuture<>();

        player.closeInventory();
        player.sendMessage(promptText(prompt, warp));

        final BaseComponent[] cancelMessage = TextComponent.fromLegacyText(Lang.CANCEL_INPUT.asColoredString());
        for (BaseComponent component : cancelMessage) {
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    TextComponent.fromLegacyText(Lang.CLICK_TO_CANCEL_INPUT.asColoredString())));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pwcancel"));
        }

        player.spigot().sendMessage(cancelMessage);

        final UUID playerId = player.getUniqueId();

        final Listener listener = new Listener() {
            // As early as possible: the value may be a warp password, which no other plugin
            // (chat logs, Discord bridges, ...) should get to see.
            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerChat(final AsyncPlayerChatEvent event) {
                if (!event.getPlayer().getUniqueId().equals(playerId)) {
                    return;
                }

                event.setCancelled(true);
                HandlerList.unregisterAll(this);

                // The chat event is asynchronous, so hop back before handing the value over.
                final String message = event.getMessage();
                PlayerWarpsPlugin.get().runSync(() -> {
                    PENDING_CHAT_INPUTS.remove(playerId, future);
                    future.complete(message);
                });
            }

            @EventHandler
            public void onCommand(final PlayerCommandPreprocessEvent event) {
                if (!event.getPlayer().getUniqueId().equals(playerId)) {
                    return;
                }

                if (event.getMessage().equalsIgnoreCase("/pwcancel")) {
                    event.setCancelled(true);
                    HandlerList.unregisterAll(this);
                    PENDING_CHAT_INPUTS.remove(playerId, future);
                    future.cancel(false);
                    player.sendMessage(Lang.INPUT_CANCELLED.asColoredString());

                    if (returnMenu != null) {
                        reopen(player, returnMenu);
                    }
                }
            }

            @EventHandler
            public void onQuit(final PlayerQuitEvent event) {
                if (event.getPlayer().getUniqueId().equals(playerId)) {
                    HandlerList.unregisterAll(this);
                    PENDING_CHAT_INPUTS.remove(playerId, future);
                    future.cancel(false);
                }
            }
        };

        // Only one value can be awaited at a time - otherwise a single chat message would be
        // handed to both actions. The older request is dropped.
        final CompletableFuture<String> previous = PENDING_CHAT_INPUTS.put(playerId, future);
        if (previous != null) {
            previous.cancel(false);
        }

        // Covers every way the request ends, so the listener never outlives it.
        future.whenComplete((input, throwable) -> HandlerList.unregisterAll(listener));

        Bukkit.getPluginManager().registerEvents(listener, PlayerWarpsPlugin.get());

        PlayerWarpsPlugin.get().runDelayed(() -> {
            if (!future.isDone()) {
                PENDING_CHAT_INPUTS.remove(playerId, future);
                future.completeExceptionally(new TimeoutException("Player did not respond in time"));

                // Without this the player would not know that what they type next is public chat again.
                if (player.isOnline()) {
                    player.sendMessage(Lang.INPUT_CANCELLED.asColoredString());
                }
            }
        }, CHAT_TIMEOUT_TICKS);

        return future;
    }
}
