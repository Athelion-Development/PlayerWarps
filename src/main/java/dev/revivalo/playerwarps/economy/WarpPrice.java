package dev.revivalo.playerwarps.economy;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.file.Config;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Objects;

/**
 * What creating a warp costs, from the pricing section of config.yml: {@code warp-pricing}
 * (FREE, FIXED or CUSTOM), {@code free-warps}, {@code warp-fixed-price} and
 * {@code warp-custom-price}.
 */
public final class WarpPrice {
    private static final String AMOUNT_PLACEHOLDER = "{amount}";

    private WarpPrice() {
        throw new RuntimeException("This class cannot be instantiated");
    }

    /**
     * The price of the next warp of the player. The first {@code free-warps} warps cost nothing.
     * In a custom formula {@code {amount}} is the number of warps the player already paid for.
     */
    public static int of(Player player) {
        final String pricing = Config.WARP_PRICING.asString();
        final String mode = pricing == null ? "FIXED" : pricing.trim().toUpperCase(Locale.ENGLISH);
        if (mode.equals("FREE")) {
            return 0;
        }

        final long owned = PlayerWarpsPlugin.getWarpHandler().getWarps().stream()
                .filter(warp -> Objects.equals(warp.getOwner(), player.getUniqueId()))
                .count();
        final int free = readInt(Config.FREE_WARPS, 0);
        if (owned < free) {
            return 0;
        }

        final int fixedPrice = Math.max(0, readInt(Config.WARP_FIXED_PRICE, 0));
        switch (mode) {
            case "FIXED":
                return fixedPrice;
            case "CUSTOM":
                final String formula = Config.WARP_CUSTOM_PRICE.asString();
                try {
                    return toPrice(evaluate(Objects.requireNonNull(formula, "warp-custom-price is not set")
                            .replace(AMOUNT_PLACEHOLDER, String.valueOf(owned - free))));
                } catch (RuntimeException ex) {
                    PlayerWarpsPlugin.get().getLogger().warning("Cannot calculate warp-custom-price '" + formula + "' ("
                            + ex.getMessage() + "), using warp-fixed-price instead.");
                    return fixedPrice;
                }
            default:
                PlayerWarpsPlugin.get().getLogger().warning("Unknown warp-pricing '" + pricing
                        + "', expected FREE, FIXED or CUSTOM. Using FIXED.");
                return fixedPrice;
        }
    }

    private static int readInt(Config option, int fallback) {
        try {
            return option.asInteger();
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static int toPrice(double value) {
        if (Double.isNaN(value) || value <= 0) {
            return 0;
        }

        return (int) Math.min(Integer.MAX_VALUE, Math.round(value));
    }

    /**
     * Calculates a formula made of numbers, + - * / and parentheses.
     *
     * @throws IllegalArgumentException if the formula is not valid
     */
    public static double evaluate(String formula) {
        final Parser parser = new Parser(formula);
        final double result = parser.expression();
        parser.skipSpaces();
        if (parser.position < formula.length()) {
            throw new IllegalArgumentException("unexpected '" + formula.charAt(parser.position) + "' at position " + parser.position);
        }

        return result;
    }

    private static final class Parser {
        private final String text;
        private int position;

        Parser(String text) {
            this.text = text;
        }

        void skipSpaces() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
        }

        boolean take(char expected) {
            skipSpaces();
            if (position < text.length() && text.charAt(position) == expected) {
                position++;
                return true;
            }

            return false;
        }

        double expression() {
            double value = term();
            while (true) {
                if (take('+')) value += term();
                else if (take('-')) value -= term();
                else return value;
            }
        }

        double term() {
            double value = factor();
            while (true) {
                if (take('*')) value *= factor();
                else if (take('/')) value /= factor();
                else return value;
            }
        }

        double factor() {
            if (take('-')) return -factor();
            if (take('+')) return factor();

            if (take('(')) {
                final double value = expression();
                if (!take(')')) {
                    throw new IllegalArgumentException("missing ')'");
                }
                return value;
            }

            skipSpaces();
            final int start = position;
            while (position < text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) {
                position++;
            }

            if (start == position) {
                throw new IllegalArgumentException("a number is expected at position " + position);
            }

            return Double.parseDouble(text.substring(start, position));
        }
    }
}
