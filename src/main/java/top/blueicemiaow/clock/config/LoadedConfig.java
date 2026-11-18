package top.blueicemiaow.clock.config;

import top.blueicemiaow.clock.render.TimeText;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A parsed, validated configuration together with its runtime helpers.
 */
public final class LoadedConfig {

    private final ClockConfig config;
    private final ConfigSource source;
    private final ZoneId zone;
    private final ImageStore images;
    private final Map<String, DateTimeFormatter> formatters = new HashMap<>();

    public LoadedConfig(ClockConfig config, ConfigSource source, ZoneId zone, ImageStore images) {
        this.config = config;
        this.source = source;
        this.zone = zone;
        this.images = images;
    }

    public ClockConfig config() {
        return config;
    }

    public ConfigSource source() {
        return source;
    }

    public ZoneId zone() {
        return zone;
    }

    public ImageStore images() {
        return images;
    }

    public DateTimeFormatter formatter(String pattern) {
        return formatters.computeIfAbsent(pattern,
                value -> DateTimeFormatter.ofPattern(value, Locale.getDefault()));
    }

    /** Pattern used by text {@code {time}}. */
    public String timePattern(LayoutItem item) {
        return item != null && item.getTimeFormat() != null && !item.getTimeFormat().isBlank()
                ? item.getTimeFormat()
                : config.getFormat();
    }

    /** Pattern used by text {@code {date}}. */
    public String datePattern(LayoutItem item) {
        return item != null && item.getDateFormat() != null && !item.getDateFormat().isBlank()
                ? item.getDateFormat()
                : TimeText.DEFAULT_DATE_FORMAT;
    }

    public DateTimeFormatter timeFormatter(LayoutItem item) {
        return formatter(timePattern(item));
    }

    public DateTimeFormatter dateFormatter(LayoutItem item) {
        return formatter(datePattern(item));
    }
}
