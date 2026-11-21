package top.blueicemiaow.clock.render;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Text template used by text items, together with the defaults shared by
 * every text component.
 * <p>
 * Three placeholders have the same status and are all optional:
 * <ul>
 *     <li>{@link #PLACEHOLDER_TIME} - formatted with the item's
 *     {@code timeFormat}</li>
 *     <li>{@link #PLACEHOLDER_DATE} - formatted with the item's
 *     {@code dateFormat}</li>
 *     <li>{@link #PLACEHOLDER_WEEK} - the item's {@code weekDef} entry for
 *     today</li>
 * </ul>
 * Everything else is rendered literally. Newlines create additional lines.
 */
public final class TimeText {

    public static final String PLACEHOLDER_TIME = "{time}";
    public static final String PLACEHOLDER_DATE = "{date}";
    public static final String PLACEHOLDER_WEEK = "{week}";

    /** Fallback pattern for {@code {time}}. */
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    /** Fallback pattern for {@code {date}}. */
    public static final String DEFAULT_DATE_FORMAT = "MM/dd/yyyy";

    /** English names, ordered Sunday through Saturday. */
    public static final List<String> DEFAULT_WEEK_DEF = List.of(
            "Sunday", "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday");

    public static final String DEFAULT_FONT_FAMILY = "Noto Sans";
    public static final int DEFAULT_FONT_SIZE = 256;
    public static final String DEFAULT_FONT_STYLE = "plain";

    private TimeText() {
    }

    public static String render(String template, ZonedDateTime now,
                                DateTimeFormatter timeFormatter, DateTimeFormatter dateFormatter,
                                List<String> weekDef) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        String result = template;
        if (result.contains(PLACEHOLDER_TIME)) {
            result = result.replace(PLACEHOLDER_TIME, timeFormatter.format(now));
        }
        if (result.contains(PLACEHOLDER_DATE)) {
            result = result.replace(PLACEHOLDER_DATE, dateFormatter.format(now));
        }
        if (result.contains(PLACEHOLDER_WEEK)) {
            result = result.replace(PLACEHOLDER_WEEK, weekName(weekDef, now));
        }
        return result;
    }

    /**
     * Returns the configured name for today's weekday. The array order is
     * Sunday through Saturday; missing or malformed definitions fall back to
     * the English names.
     */
    public static String weekName(List<String> weekDef, ZonedDateTime now) {
        List<String> names = weekDef != null && weekDef.size() == 7
                ? weekDef
                : DEFAULT_WEEK_DEF;
        return names.get(now.getDayOfWeek().getValue() % 7);
    }
}
