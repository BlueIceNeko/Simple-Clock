package top.blueicemiaow.clock.config;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Root configuration object. All fields are optional; missing fields keep the
 * defaults below.<p>
 * Defaults that only concern text components live in
 * {@link top.blueicemiaow.clock.render.TimeText}.<p>
 * A complete example is bundled as {@code default_config.json}.
 */
public final class ClockConfig {

    public static final String DEFAULT_TITLE = "Simple Clock";

    private Rgba background = new Rgba(0, 0, 0, 255);

    /** Optional background image, painted over the background color. */
    private String backgroundImage = null;

    /** Background image opacity, 0.0 to 1.0. */
    private Double opacity = 1.0;

    /** Window title. Blank values fall back to {@link #DEFAULT_TITLE}. */
    private String title = DEFAULT_TITLE;

    /** Optional IANA zone id, for example Asia/Shanghai. Defaults to the system zone. */
    private String zone = null;

    /** Repaint interval in milliseconds. */
    private int refreshMillis = 50;

    private boolean alwaysOnTop = false;
    private boolean hideCursor = false;

    /** Zero-based screen index. Out-of-range values fall back to screen 0. */
    private int screen = 0;

    private List<LayoutItem> layout;

    public ClockConfig() {
    }

    public Rgba getBackground() {
        return background;
    }

    public String getTitle() {
        return title == null || title.isBlank() ? DEFAULT_TITLE : title;
    }

    public String getBackgroundImage() {
        return backgroundImage == null || backgroundImage.isBlank() ? null : backgroundImage;
    }

    public double backgroundOpacity() {
        return opacity == null ? 1.0 : opacity;
    }

    public String getZone() {
        return zone;
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

        if (opacity != null && (opacity < 0.0 || opacity > 1.0)) {
            errors.add("opacity must be between 0.0 and 1.0");
        }

        if (zone != null && !zone.isBlank()) {
            try {
                ZoneId.of(zone.trim());
            } catch (Exception e) {
                errors.add("zone is not a valid time zone id: " + zone);
            }
        }

        if (refreshMillis < 16 || refreshMillis > 60_000) {
            errors.add("refreshMillis must be between 16 and 60000");
        }
        if (screen < 0) {
            errors.add("screen must be zero or greater");
        }
        // The layout array is validated item by item in ConfigLoader so that a
        // malformed component does not stop startup.
        return errors;
    }
}
