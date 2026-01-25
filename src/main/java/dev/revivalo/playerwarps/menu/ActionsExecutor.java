package dev.revivalo.playerwarps.menu;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.menu.page.CategoriesMenu;
import dev.revivalo.playerwarps.menu.page.InputMenu;
import dev.revivalo.playerwarps.menu.page.Menu;
import dev.revivalo.playerwarps.menu.page.WarpsMenu;
import dev.revivalo.playerwarps.util.TextUtil;
import dev.revivalo.playerwarps.warp.action.SearchWarpAction;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.apache.commons.lang.StringUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionsExecutor {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%\\w+%");
    private static final WeakHashMap<UUID, KeyedBossBar> BOSS_BARS = new WeakHashMap<>();

    public static void executeActions(Player player, List<ClickAction> actions) {
        executeActions(player, actions, null);
    }

    public static void executeActions(Player player, List<ClickAction> actions, Menu menu) {
        final Set<String> words = new HashSet<>();
        String[] titleText = new String[]{" ", " "};

        boolean sendTitle = false;
        if (actions != null) {
            for (ClickAction action : actions) {
                String line = action.getStatement();

                String[] parts = line.split(":", 2);

                short chance = 100;

                try {
                    chance = Short.parseShort(parts[0].trim());
                    line = parts[1];
                } catch (NumberFormatException ignored) {

                }

                if (chance != 100) {
                    if (ThreadLocalRandom.current().nextInt(0, 100) > chance) {
                        continue;
                    }
                }

                Matcher matcher = PLACEHOLDER_PATTERN.matcher(line);

                while (matcher.find()) {
                    words.add(matcher.group());
                }

                for (String word : words) {
                    if (word.equals("%player%")) {
                        line = StringUtils.replace(line, word, player.getName());
                    }
                }

                final BaseGui gui = menu.getBaseGui();
                String lineWithPlaceholders = TextUtil.applyPlaceholdersToString(player, line);
                String coloredLine = TextUtil.colorize(lineWithPlaceholders);

                switch (action.getActionType()) {
                    case CONSOLE:
                        PlayerWarpsPlugin.get().executeCommandAsConsole(TextUtil.applyPlaceholdersToString(player, lineWithPlaceholders));
                        break;
                    case PLAYER:
                        player.performCommand(lineWithPlaceholders);
                        break;
                    case SORT:
                        WarpsMenu warpsMenu = (WarpsMenu) menu;
                        ((WarpsMenu) menu).getPaginatedGui().clearPageItems();
                        warpsMenu.open(player, warpsMenu.getCategoryName(), menu.getNextSortType(warpsMenu.getSortType()), warpsMenu.getFoundWarps());
                        break;
                    case NEXT_PAGE:
                        if (!(gui instanceof PaginatedGui paginatedGui)) return;
                        if ((paginatedGui.getCurrentPageNum()) >= paginatedGui.getPagesNum()) return;
                        paginatedGui.next();
                        paginatedGui.updateTitle(menu.getTemplate().getTitle().replace("%page%", String.valueOf(paginatedGui.getCurrentPageNum())));
                        break;
                    case PREVIOUS_PAGE:
                        if (!(gui instanceof PaginatedGui paginatedGui)) return;
                        if ((paginatedGui.getCurrentPageNum() <= 1)) return;
                        paginatedGui.previous();
                        paginatedGui.updateTitle(menu.getTemplate().getTitle().replace("%page%", String.valueOf(paginatedGui.getCurrentPageNum())));
                        break;
                    case SEARCH:
                        new InputMenu(null)
                                .setWarpAction(new SearchWarpAction())
                                .open(player);
                        break;
                    case OPEN:
//                        if (Config.ENABLE_CATEGORIES.asBoolean()) {
//                            new CategoriesMenu().open(player);
//                        } else {
//                            new WarpsMenu.DefaultWarpsMenu().open(player, "all");
//                        }
                        switch (lineWithPlaceholders.toLowerCase()) {
                            case "categories":
                                new CategoriesMenu().open(player);
                                break;
                            case "mywarps":
                                if (!(menu instanceof WarpsMenu.MyWarpsMenu)) {
                                    new WarpsMenu.MyWarpsMenu()
                                            .open(player);
                                }
                                break;
                            case "favorites":
                                if (!(menu instanceof WarpsMenu.FavoriteWarpsMenu)) {
                                    new WarpsMenu.FavoriteWarpsMenu()
                                            .open(player);
                                }
                                break;
                            default:
                                new WarpsMenu.DefaultWarpsMenu()
                                        .open(player, lineWithPlaceholders);
                                break;
                        }
//                        PlayerUtil.sendList(Splitter.on("|").splitToList(coloredLine), player);
//                        break;
//                    case ANNOUNCE:
//                    case BROADCAST:
//                        PlayerUtil.sendList(
//                                Splitter.on("|").splitToList(coloredLine),
//                                PlayerWarpsPlugin.get().getServer().getOnlinePlayers().toArray(new CommandSender[0])
//                        );
//                        break;
//                    case ACTIONBAR:
//                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredLine));
//                        break;
//                    case TITLE:
//                        titleText[0] = coloredLine;
//                        sendTitle = true;
//                        break;
//                    case SUBTITLE:
//                        titleText[1] = coloredLine;
//                        sendTitle = true;
//                        break;
//                    case SOUND:
//                        SoundPlayerBuilder.fromString(line.toUpperCase(Locale.ENGLISH)).play(player);
//                        break;
//                    case OPEN:
//                        PlayerWarpsPlugin.get().getMenuManager().openRewardsMenu(player, coloredLine);
//                        break;
//                    case BOSSBAR:
//                        sendBossBar(player, coloredLine);
//                        break;
//                    case FIREWORK:
//                        CustomFireworkBuilder.fromString(line).launch(player.getLocation());
//                        break;
                }
            }
        }

        if (sendTitle) sendTitle(player, titleText[0], titleText[1]);
    }

    public static Map<Short, List<ClickAction>> getActions(ConfigurationSection configurationSection, String key) {
        final Map<Short, List<ClickAction>> actionsMap = new HashMap<>();

        ConfigurationSection section = configurationSection.getConfigurationSection(key);
        if (section != null) {
            for (String chance : section.getKeys(false)) {
                actionsMap.put(Short.parseShort(chance), TextUtil.findAndReturnActions(configurationSection.getStringList(key + "." + chance)));
            }
        } else {
            actionsMap.put((short) 100, TextUtil.findAndReturnActions(configurationSection.getStringList(key)));
        }

        return actionsMap;
    }

    private static void sendTitle(Player player, String title, String subtitle) {
        try {
            Method sendTitleMethod = Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            sendTitleMethod.invoke(player, title, subtitle, 0, 41, 0);
        } catch (NoSuchMethodException e) {
            player.sendTitle(title, subtitle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendBossBar(Player player, String text) {
        final UUID uuid = player.getUniqueId();
        KeyedBossBar bossBar;
        if (BOSS_BARS.containsKey(uuid)) {
            bossBar = BOSS_BARS.get(uuid);
        } else {
            bossBar = PlayerWarpsPlugin.get().getServer().createBossBar(
                    new NamespacedKey(PlayerWarpsPlugin.get(), "playerwarps_" + player.getName()),
                    text,
                    BarColor.BLUE,
                    BarStyle.SOLID
            );
            bossBar.addPlayer(player);
            BOSS_BARS.put(uuid, bossBar);
        }

        bossBar.setTitle(text);
        bossBar.setVisible(true);

        PlayerWarpsPlugin.get().runDelayed(() -> {
            bossBar.setVisible(false);
        }, 20);
    }
}