package top.blueicemiaow.clock.config;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One entry in the layout array. Two item types are supported:
 * {@code text} and {@code image}.
 */
public final class LayoutItem {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";

    private static final Set<String> TYPES = Set.of(TYPE_TEXT, TYPE_IMAGE);
    private static final Set<String> TEXT_ALIGNS = Set.of("left", "center", "centre", "right");

    private String type;
    private String anchor;
    private Boolean centerX;
    private Boolean centerY;
    private int x;
    private int y;

    private Rgba color;
    private String fontFamily;
    private Integer fontSize;
    private String fontStyle;

    private String text;
    private String textAlign;

    private String path;
    private Integer width;
    private Integer height;
    private Double opacity;

    /** Optional per-item time pattern. Used by text {@code {time}}. */
    private String timeFormat;

    /** Optional per-item date pattern. Used by text {@code {date}}. */
    private String dateFormat;

    public LayoutItem() {
    }

    public static LayoutItem time() {
        LayoutItem item = new LayoutItem();
        item.type = TYPE_TEXT;
        item.text = "{time}";
        item.anchor = "center";
        return item;
    }

    public String getType() {
        return type;
    }

    public String getAnchor() {
        return anchor;
    }

    public Boolean getCenterX() {
        return centerX;
    }

    public Boolean getCenterY() {
        return centerY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Rgba getColor() {
        return color;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public String getFontStyle() {
        return fontStyle;
    }

    public String getText() {
        return text;
    }

    public String getTextAlign() {
        return textAlign;
    }

    public String getPath() {
        return path;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Double getOpacity() {
        return opacity;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String typeOrEmpty() {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isText() {
        return TYPE_TEXT.equals(typeOrEmpty());
    }

    public boolean isImage() {
        return TYPE_IMAGE.equals(typeOrEmpty());
    }

    public double opacityOrOne() {
        return opacity == null ? 1.0 : opacity;
    }

    public String textAlignOrCenter() {
        return textAlign == null || textAlign.isBlank()
                ? "center"
                : textAlign.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves anchor, explicit anchor wins over the centerX/centerY flags.
     * With neither set, the item is horizontally and vertically centered.
     */
    public Anchor resolveAnchor() {
        if (anchor != null && !anchor.isBlank()) {
            return Anchor.parse(anchor);
        }
        boolean cx = centerX == null || centerX;
        boolean cy = centerY == null || centerY;
        return Anchor.forCenterFlags(cx, cy);
    }

    public void validate(int index, List<String> errors) {
        String field = "layout[" + index + "]";
        String parsedType = typeOrEmpty();
        if (parsedType.isEmpty()) {
            errors.add(field + ".type is required (text or image)");
        } else if (!TYPES.contains(parsedType)) {
            errors.add(field + ".type must be text or image (was '" + type + "')");
        }

        if (anchor != null && !anchor.isBlank()) {
            try {
                Anchor.parse(anchor);
            } catch (IllegalArgumentException e) {
                errors.add(field + ".anchor: " + e.getMessage());
            }
        }

        if (fontSize != null && (fontSize < 1 || fontSize > 10_000)) {
            errors.add(field + ".fontSize must be between 1 and 10000");
        }
        if (fontStyle != null && !fontStyle.isBlank()) {
            try {
                FontStyle.parse(fontStyle);
            } catch (IllegalArgumentException e) {
                errors.add(field + ".fontStyle: " + e.getMessage());
            }
        }
        if (width != null && width < 1) {
            errors.add(field + ".width must be greater than 0");
        }
        if (height != null && height < 1) {
            errors.add(field + ".height must be greater than 0");
        }
        if (opacity != null && (opacity < 0.0 || opacity > 1.0)) {
            errors.add(field + ".opacity must be between 0.0 and 1.0");
        }
        if (color != null) {
            color.validate(field + ".color", errors);
        }
        if (textAlign != null && !textAlign.isBlank()
                && !TEXT_ALIGNS.contains(textAlign.trim().toLowerCase(Locale.ROOT))) {
            errors.add(field + ".textAlign must be left, center or right");
        }

        if (TYPE_TEXT.equals(parsedType) && text == null) {
            errors.add(field + ".text is required for text items");
        }
        if (TYPE_IMAGE.equals(parsedType) && (path == null || path.isBlank())) {
            errors.add(field + ".path is required for image items");
        }
        if (timeFormat != null && !timeFormat.isBlank()) {
            try {
                java.time.format.DateTimeFormatter.ofPattern(timeFormat);
            } catch (IllegalArgumentException e) {
                errors.add(field + ".timeFormat is not a valid time pattern: " + e.getMessage());
            }
        }
        if (dateFormat != null && !dateFormat.isBlank()) {
            try {
                java.time.format.DateTimeFormatter.ofPattern(dateFormat);
            } catch (IllegalArgumentException e) {
                errors.add(field + ".dateFormat is not a valid date pattern: " + e.getMessage());
            }
        }
    }
}
