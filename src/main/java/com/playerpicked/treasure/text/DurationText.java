package com.playerpicked.treasure.text;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationText {
    private static final Pattern PART = Pattern.compile("(\\d+)\\s*([dhms])");
    private static final Pattern FULL = Pattern.compile("(\\s*\\d+\\s*[dhms])+\\s*");

    private DurationText() { }

    public static @NotNull String format(@NotNull Duration duration) {
        long totalSeconds = Math.max(0, (duration.toMillis() + 999) / 1000);
        if (totalSeconds == 0) return "0s";

        long days = totalSeconds / 86_400;
        long hours = totalSeconds % 86_400 / 3_600;
        long minutes = totalSeconds % 3_600 / 60;
        long seconds = totalSeconds % 60;

        StringBuilder text = new StringBuilder();
        append(text, days, "d");
        append(text, hours, "h");
        append(text, minutes, "m");
        append(text, seconds, "s");
        return text.toString();
    }

    public static @NotNull Optional<Duration> parse(@NotNull String input) {
        String text = input.trim().toLowerCase(Locale.ROOT);
        if (text.matches("\\d+")) return parseSeconds(text);
        if (!FULL.matcher(text).matches()) return Optional.empty();

        Duration total = Duration.ZERO;
        Matcher matcher = PART.matcher(text);
        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1));
            total = total.plus(switch (matcher.group(2)) {
                case "d" -> Duration.ofDays(amount);
                case "h" -> Duration.ofHours(amount);
                case "m" -> Duration.ofMinutes(amount);
                default -> Duration.ofSeconds(amount);
            });
        }
        return Optional.of(total);
    }

    private static @NotNull Optional<Duration> parseSeconds(@NotNull String digits) {
        try {
            return Optional.of(Duration.ofSeconds(Long.parseLong(digits)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private static void append(@NotNull StringBuilder text, long amount, @NotNull String unit) {
        if (amount == 0) return;
        if (!text.isEmpty()) text.append(' ');
        text.append(amount).append(unit);
    }
}
