package top.blueicemiaow.clock.config;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Root configuration object. All fields are optional; missing fields keep the
 * defaults below.<p>
 * A complete example is bundled as {@code default_config.json}.
 */
public final class ClockConfig {

    private Rgba background = new Rgba(8, 12, 20, 255);

    /** DateTimeFormatter pattern, for example HH:mm:ss. */
    private String format = "HH:mm:ss";

    /** Optional IANA zone id, for example Asia/Shanghai. Defaults to the system zone. */
    private String zone;

    private String fontFamily = "MicrosoftYaHei";
    private int fontSize = 192;
    private String fontStyle = "plain";

    /** Repaint interval in milliseconds. */
    private int refreshMillis = 100;

    private boolean alwaysOnTop = true;
    private boolean hideCursor = true;

    /** Zero-based screen index. Out-of-range values fall back to screen 0. */
    private int screen = 0;

    private List<LayoutItem> layout;

    public ClockConfig() {
    }

    public Rgba getBackground() {
        return background;
    }

    public String getFormat() {
        return format;
    }

    public String getZone() {
        return zone;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public int getFontSize() {
        return fontSize;
    }

    public String getFontStyle() {
        return fontStyle;
    }

    public int getRefreshMillis() {
        return refreshMillis;
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public boolean isHideCursor() {
        return hideCursor;
    }

    public int getScreen() {
        return screen;
    }

    public List<LayoutItem> getLayout() {
        return layout;
    }

    public void setLayout(List<LayoutItem> layout) {
        this.layout = layout;
    }

    public ZoneId resolveZone() {
        return zone == null || zone.isBlank() ? ZoneId.systemDefault() : ZoneId.of(zone.trim());
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (background == null) {
            errors.add("background must not be null");
        } else {
            background.validate("background", errors);
        }

        if (format == null || format.isBlank()) {
            errors.add("format must not be blank");
        } else {
            try {
                DateTimeFormatter.ofPattern(format);
            } catch (IllegalArgumentException e) {
                errors.add("format is not a valid time pattern: " + e.getMessage());
            }
        }

        if (zone != null && !zone.isBlank()) {
            try {
                ZoneId.of(zone.trim());
            } catch (Exception e) {
                errors.add("zone is not a valid time zone id: " + zone);
            }
        }

        if (fontFamily == null || fontFamily.isBlank()) {
            errors.add("fontFamily must not be blank");
        }
        if (fontSize < 1 || fontSize > 10_000) {
            errors.add("fontSize must be between 1 and 10000");
        }
        if (fontStyle == null || fontStyle.isBlank()) {
            errors.add("fontStyle must not be blank");
        } else {
            try {
                FontStyle.parse(fontStyle);
            } catch (IllegalArgumentException e) {
                errors.add("fontStyle: " + e.getMessage());
            }
        }

        if (refreshMillis < 16 || refreshMillis > 60_000) {
            errors.add("refreshMillis must be between 16 and 60000");
        }
        if (screen < 0) {
            errors.add("screen must be zero or greater");
        }
        if (layout == null) {
            errors.add("layout must not be null");
        } else {
            for (int i = 0; i < layout.size(); i++) {
                LayoutItem item = layout.get(i);
                if (item == null) {
                    errors.add("layout[" + i + "] must not be null");
                } else {
                    item.validate(i, errors);
                }
            }
        }
        return errors;
    }
}
