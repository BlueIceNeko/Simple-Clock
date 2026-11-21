package top.blueicemiaow.clock.config;

import top.blueicemiaow.clock.render.TimeText;

import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
    private final BufferedImage backgroundImage;
    private final Map<String, DateTimeFormatter> formatters = new HashMap<>();

    public LoadedConfig(ClockConfig config, ConfigSource source, ZoneId zone, ImageStore images,
                        BufferedImage backgroundImage) {
        this.config = config;
        this.source = source;
        this.zone = zone;
        this.images = images;
        this.backgroundImage = backgroundImage;
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

    /** Background image, or null when the configuration uses a background color. */
    public BufferedImage backgroundImage() {
        return backgroundImage;
    }

    public DateTimeFormatter formatter(String pattern) {
        return formatters.computeIfAbsent(pattern,
                value -> DateTimeFormatter.ofPattern(value, Locale.getDefault()));
    }

    /** Pattern used by text {@code {time}}. */
    public String timePattern(LayoutItem item) {
        return item == null ? TimeText.DEFAULT_TIME_FORMAT : item.getTimeFormat();
    }

    /** Pattern used by text {@code {date}}. */
    public String datePattern(LayoutItem item) {
        return item == null ? TimeText.DEFAULT_DATE_FORMAT : item.getDateFormat();
    }

    /** Weekday names used by text {@code {week}}. */
    public List<String> weekDef(LayoutItem item) {
        return item == null ? TimeText.DEFAULT_WEEK_DEF : item.getWeekDef();
    }

    public DateTimeFormatter timeFormatter(LayoutItem item) {
        return formatter(timePattern(item));
    }

    public DateTimeFormatter dateFormatter(LayoutItem item) {
        return formatter(datePattern(item));
    }
}
