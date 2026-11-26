package dev.revivalo.playerwarps.input;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.util.Debug;
import dev.revivalo.playerwarps.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Opens a Minecraft dialog (the modal screen added in 1.21.6) with a single text field.
 * <p>
 * Everything is done reflectively on purpose. The plugin compiles against the Spigot API and
 * shades Adventure under a relocated package, so referencing {@code io.papermc.paper.dialog.*}
 * or {@code net.kyori.adventure.text.Component} directly would either not compile or would be
 * rewritten by the shade relocation and break at runtime. Reflection resolves the server's own
 * classes instead, and keeps the plugin loadable on Spigot and on older versions.
 * <p>
 * Every failure path returns {@code false} so the caller can fall back to another input mode.
 */
public final class ModalInput {
    private static final String INPUT_KEY = "playerwarps_input";
    private static final int TEXT_MAX_LENGTH = 128;

    /**
     * The dialog API takes the server's Adventure classes. The plugin's own bundled copy is
     * relocated (see pom.xml) and the shade plugin rewrites matching string constants as well,
     * so a plain "net.kyori..." literal would end up naming the relocated copy, which has no
     * dialog support. Assembled at runtime, the name is out of the shade plugin's reach.
     */
    private static final String ADVENTURE_PACKAGE = String.join(".", "net", "kyori", "adventure");

    private static Boolean available;
    private static boolean failureLogged;

    private ModalInput() {
        throw new RuntimeException("This class cannot be instantiated");
    }

    /**
     * Whether this server can show dialogs: new enough Minecraft and the Paper dialog API present.
     */
    public static boolean isAvailable() {
        if (available == null) {
            available = resolveAvailability();
        }

        return available;
    }

    private static final String[] REQUIRED_CLASSES = {
            "io.papermc.paper.dialog.Dialog",
            "io.papermc.paper.dialog.DialogResponseView",
            "io.papermc.paper.registry.data.dialog.DialogBase",
            "io.papermc.paper.registry.data.dialog.ActionButton",
            "io.papermc.paper.registry.data.dialog.action.DialogAction",
            "io.papermc.paper.registry.data.dialog.action.DialogActionCallback",
            "io.papermc.paper.registry.data.dialog.input.DialogInput",
            "io.papermc.paper.registry.data.dialog.type.DialogType",
            "io.papermc.paper.registry.RegistryBuilderFactory",
            adventureClass("dialog.DialogLike"),
            adventureClass("text.Component"),
            adventureClass("text.event.ClickCallback$Options"),
    };

    private static String adventureClass(String className) {
        return ADVENTURE_PACKAGE + "." + className;
    }

    private static boolean resolveAvailability() {
        if (!VersionUtil.isDialogSupport()) {
            PlayerWarpsPlugin.get().getLogger().warning(String.format(
                    "Input mode MODAL needs Minecraft %s or newer, this server reports %s. Using chat input instead.",
                    VersionUtil.getDialogMinimumVersion(), Bukkit.getBukkitVersion()));
            return false;
        }

        for (String className : REQUIRED_CLASSES) {
            try {
                // Deliberately does not initialize: Dialog's static initializer resolves
                // CUSTOM_OPTIONS/QUICK_ACTIONS/SERVER_LINKS from the registry. Running it from a
                // capability check would fail whenever the registry is not ready yet, and a
                // failed initialization poisons the class for the rest of the JVM's life.
                Class.forName(className, false, ModalInput.class.getClassLoader());
            } catch (ClassNotFoundException | LinkageError ex) {
                PlayerWarpsPlugin.get().getLogger().warning(String.format(
                        "Input mode MODAL needs the Paper dialog API, but %s is missing "
                                + "(is this server Paper or a Paper fork?). Using chat input instead.",
                        className));
                return false;
            }
        }

        Debug.log("Modal dialogs are available (server %s).", Bukkit.getBukkitVersion());
        return true;
    }

    /**
     * Shows the dialog. {@code onSubmit} is called on the thread the dialog callback runs on -
     * callers that touch the world have to hop to the main thread themselves.
     *
     * @return false when the dialog could not be shown and another input mode should be used
     */
    public static boolean open(Player player, String title, String prompt, String submitLabel,
                               Consumer<String> onSubmit) {
        if (!isAvailable()) {
            return false;
        }

        try {
            showDialog(player, title, prompt, submitLabel, onSubmit);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            // The availability flag stays untouched on purpose - a single failure (for example
            // during startup, before the registries are ready) must not disable modals for good.
            if (failureLogged) {
                Debug.log("Modal dialog failed again: %s", ex);
            } else {
                failureLogged = true;
                PlayerWarpsPlugin.get().getLogger().log(Level.WARNING,
                        "Could not open the modal dialog, falling back to the chat input.", ex);
            }

            return false;
        }
    }

    private static void showDialog(Player player, String title, String prompt, String submitLabel,
                                   Consumer<String> onSubmit) throws ReflectiveOperationException {
        final Class<?> componentCls = Class.forName(adventureClass("text.Component"));
        final Class<?> dialogCls = Class.forName("io.papermc.paper.dialog.Dialog");
        final Class<?> dialogBaseCls = Class.forName("io.papermc.paper.registry.data.dialog.DialogBase");
        final Class<?> dialogInputCls = Class.forName("io.papermc.paper.registry.data.dialog.input.DialogInput");
        final Class<?> actionButtonCls = Class.forName("io.papermc.paper.registry.data.dialog.ActionButton");
        final Class<?> dialogActionCls = Class.forName("io.papermc.paper.registry.data.dialog.action.DialogAction");
        final Class<?> callbackCls = Class.forName("io.papermc.paper.registry.data.dialog.action.DialogActionCallback");
        final Class<?> dialogTypeCls = Class.forName("io.papermc.paper.registry.data.dialog.type.DialogType");
        final Class<?> factoryCls = Class.forName("io.papermc.paper.registry.RegistryBuilderFactory");
        final Class<?> dialogLikeCls = Class.forName(adventureClass("dialog.DialogLike"));
        final Class<?> optionsCls = Class.forName(adventureClass("text.event.ClickCallback$Options"));

        final Method componentText = componentCls.getMethod("text", String.class);

        // DialogInput.text(key, label) -> TextDialogInput.Builder -> build()
        final Object inputBuilder = dialogInputCls
                .getMethod("text", String.class, componentCls)
                .invoke(null, INPUT_KEY, componentText.invoke(null, prompt));
        callIfPresent(inputBuilder, "maxLength", int.class, TEXT_MAX_LENGTH);
        final Object input = callByName(inputBuilder, "build");

        // DialogBase.builder(title) -> inputs(List) -> build()
        final Object baseBuilder = dialogBaseCls
                .getMethod("builder", componentCls)
                .invoke(null, componentText.invoke(null, title));
        callByName(baseBuilder, "inputs", List.class, Collections.singletonList(input));
        final Object base = callByName(baseBuilder, "build");

        final Object callback = Proxy.newProxyInstance(
                ModalInput.class.getClassLoader(),
                new Class<?>[]{callbackCls},
                responseHandler(onSubmit));

        // DialogAction.customClick(callback, options)
        final Object action = dialogActionCls
                .getMethod("customClick", callbackCls, optionsCls)
                .invoke(null, callback, defaultClickOptions(optionsCls));

        // ActionButton.create(label, tooltip, width, action)
        final Object button = actionButtonCls
                .getMethod("create", componentCls, componentCls, int.class, dialogActionCls)
                .invoke(null, componentText.invoke(null, submitLabel), null, 150, action);

        final Object type = dialogTypeCls.getMethod("notice", actionButtonCls).invoke(null, button);

        final Method empty = factoryCls.getMethod("empty");
        final Object dialog = dialogCls.getMethod("create", Consumer.class).invoke(null, (Consumer<Object>) factory -> {
            try {
                final Object builder = empty.invoke(factory);
                callByName(builder, "base", base);
                callByName(builder, "type", type);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Unexpected Paper dialog builder", ex);
            }
        });

        player.getClass().getMethod("showDialog", dialogLikeCls).invoke(player, dialog);
    }

    private static InvocationHandler responseHandler(Consumer<String> onSubmit) {
        return (proxy, method, args) -> {
            switch (method.getName()) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "PlayerWarpsDialogCallback";
                default:
                    break;
            }

            if (args != null && args.length > 0 && args[0] != null) {
                final Object value = args[0].getClass()
                        .getMethod("getText", String.class)
                        .invoke(args[0], INPUT_KEY);

                if (value != null) {
                    onSubmit.accept(String.valueOf(value));
                }
            }

            return null;
        };
    }

    /**
     * {@code ClickCallback.Options.builder().build()}, or null when that shape ever changes.
     */
    private static Object defaultClickOptions(Class<?> optionsCls) {
        try {
            return callByName(optionsCls.getMethod("builder").invoke(null), "build");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    /**
     * Builder steps are looked up by name so that an added overload or a moved interface does
     * not break the whole dialog.
     */
    private static Object callByName(Object target, String name, Object... args)
            throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
                continue;
            }

            method.setAccessible(true);
            return method.invoke(target, args);
        }

        throw new NoSuchMethodException(name + " on " + target.getClass().getName());
    }

    private static Object callByName(Object target, String name, Class<?> parameterType, Object arg)
            throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                continue;
            }

            if (!method.getParameterTypes()[0].isAssignableFrom(parameterType)) {
                continue;
            }

            method.setAccessible(true);
            return method.invoke(target, arg);
        }

        throw new NoSuchMethodException(name + " on " + target.getClass().getName());
    }

    private static void callIfPresent(Object target, String name, Class<?> parameterType, Object arg) {
        try {
            callByName(target, name, parameterType, arg);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional refinement - the dialog works without it.
        }
    }
}
