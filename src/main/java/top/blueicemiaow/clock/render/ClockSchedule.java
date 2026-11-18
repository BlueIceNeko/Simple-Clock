package top.blueicemiaow.clock.render;

import top.blueicemiaow.clock.config.ClockConfig;
import top.blueicemiaow.clock.config.LayoutItem;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Calculates when the clock display actually needs to be repainted.
 * <p>
 * Repainting on a fixed-delay timer makes the visible seconds uneven: a paint
 * shortly before a second boundary shows the old second for only a fraction of
 * a second, while the next paint shows the new second for almost a full second.
 * This class aligns repaints with the boundaries of the smallest time field in
 * the configured formats instead.
 */
public final class ClockSchedule {

    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
    private static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
    private static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

    private final long resolutionMillis;
    private final long periodMillis;

    private ClockSchedule(long resolutionMillis, long periodMillis) {
        this.resolutionMillis = resolutionMillis;
        this.periodMillis = periodMillis;
    }

    /**
     * @return a schedule, or {@code null} when the layout contains no dynamic
     * time at all (for example only static text and images).
     */
    public static ClockSchedule from(ClockConfig config) {
        if (config == null || config.getLayout() == null) {
            return null;
        }

        long resolution = Long.MAX_VALUE;
        boolean dynamic = false;

        for (LayoutItem item : config.getLayout()) {
            if (item == null) {
                continue;
            }
            if (item.isText() && item.getText() != null) {
                String text = item.getText();
                if (text.contains("{time}")) {
                    Long itemResolution = resolutionOf(timePattern(item, config));
                    if (itemResolution != null) {
                        resolution = Math.min(resolution, itemResolution);
                        dynamic = true;
                    }
                }
                if (text.contains("{date}")) {
                    Long itemResolution = resolutionOf(datePattern(item));
                    resolution = Math.min(resolution,
                            itemResolution == null ? MILLIS_PER_DAY : itemResolution);
                    dynamic = true;
                }
            }
        }

        if (!dynamic) {
            return null;
        }

        long requested = Math.max(1, config.getRefreshMillis());
        long period = requested <= resolution
                ? resolution
                : divideRoundingUp(requested, resolution) * resolution;

        return new ClockSchedule(resolution, period);
    }

    /**
     * Delay until the next aligned boundary. The result is always at least one
     * millisecond so a Swing Timer can be restarted safely.
     */
    public long millisUntilNext(ZonedDateTime now) {
        long nextMillis;
        if (periodMillis % MILLIS_PER_DAY == 0) {
            ZonedDateTime next = now.truncatedTo(ChronoUnit.DAYS)
                    .plusDays(periodMillis / MILLIS_PER_DAY);
            nextMillis = next.toInstant().toEpochMilli();
        } else if (periodMillis % MILLIS_PER_HOUR == 0) {
            ZonedDateTime next = now.truncatedTo(ChronoUnit.HOURS)
                    .plusHours(periodMillis / MILLIS_PER_HOUR);
            nextMillis = next.toInstant().toEpochMilli();
        } else if (periodMillis % MILLIS_PER_MINUTE == 0) {
            ZonedDateTime next = now.truncatedTo(ChronoUnit.MINUTES)
                    .plusMinutes(periodMillis / MILLIS_PER_MINUTE);
            nextMillis = next.toInstant().toEpochMilli();
        } else {
            long nowMillis = now.toInstant().toEpochMilli();
            nextMillis = (Math.floorDiv(nowMillis, periodMillis) + 1) * periodMillis;
        }

        long delay = nextMillis - now.toInstant().toEpochMilli();
        if (delay < 1) {
            delay = periodMillis;
        }
        return delay;
    }

    public long resolutionMillis() {
        return resolutionMillis;
    }

    public long periodMillis() {
        return periodMillis;
    }

    /**
     * Finds the finest time unit represented by a DateTimeFormatter pattern.
     * Quoted literal sections are ignored.
     */
    static Long resolutionOf(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return null;
        }

        Long best = null;
        boolean quoted = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                if (quoted && i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                    i++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (quoted) {
                continue;
            }

            long unit = switch (c) {
                case 'S' -> {
                    int digits = 1;
                    while (i + 1 < pattern.length() && pattern.charAt(i + 1) == 'S') {
                        digits++;
                        i++;
                    }
                    yield digits >= 3 ? 1L : (digits == 2 ? 10L : 100L);
                }
                case 'A', 'n', 'N' -> 1L;
                case 's' -> MILLIS_PER_SECOND;
                case 'm' -> MILLIS_PER_MINUTE;
                case 'H', 'h', 'K', 'k' -> MILLIS_PER_HOUR;
                case 'd', 'D', 'e', 'E', 'u', 'w', 'W', 'M', 'L', 'y', 'Y',
                     'Q', 'q', 'G', 'g' -> MILLIS_PER_DAY;
                default -> 0L;
            };
            if (unit > 0) {
                best = best == null ? unit : Math.min(best, unit);
            }
        }
        return best;
    }

    private static long divideRoundingUp(long value, long divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static String timePattern(LayoutItem item, ClockConfig config) {
        return item != null && item.getTimeFormat() != null && !item.getTimeFormat().isBlank()
                ? item.getTimeFormat()
                : config.getFormat();
    }

    private static String datePattern(LayoutItem item) {
        return item != null && item.getDateFormat() != null && !item.getDateFormat().isBlank()
                ? item.getDateFormat()
                : TimeText.DEFAULT_DATE_FORMAT;
    }
}
