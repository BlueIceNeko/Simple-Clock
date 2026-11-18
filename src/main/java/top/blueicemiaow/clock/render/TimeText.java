package top.blueicemiaow.clock.render;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Small text template used by text items. Two placeholders are supported:
 * <ul>
 *     <li>{@code {time}} - formatted with the item's {@code timeFormat}
 *     (or the global fallback)</li>
 *     <li>{@code {date}} - formatted with the item's {@code dateFormat}</li>
 * </ul>
 * Everything else is rendered literally. Newlines create additional lines.
 */
public final class TimeText {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    private TimeText() {
    }

    public static String render(String template, ZonedDateTime now,
                                DateTimeFormatter timeFormatter, DateTimeFormatter dateFormatter) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        String result = template;
        if (result.contains("{time}")) {
            result = result.replace("{time}", timeFormatter.format(now));
        }
        if (result.contains("{date}")) {
            result = result.replace("{date}", dateFormatter.format(now));
        }
        return result;
    }
}
