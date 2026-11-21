package top.blueicemiaow.clock.config;

import java.util.Locale;

/**
 * The nine possible anchoring positions inside the screen. The item is placed so
 * that its anchor point matches the screen's corresponding anchor point. The
 * configured x/y values are then added as an offset.
 */
public enum Anchor {

    TOP_LEFT(0, 0),
    TOP_CENTER(1, 0),
    TOP_RIGHT(2, 0),
    CENTER_LEFT(0, 1),
    CENTER(1, 1),
    CENTER_RIGHT(2, 1),
    BOTTOM_LEFT(0, 2),
    BOTTOM_CENTER(1, 2),
    BOTTOM_RIGHT(2, 2);

    private final int horizontal;
    private final int vertical;

    Anchor(int horizontal, int vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public int placeX(int itemWidth, int screenWidth, int offsetX) {
        int base = switch (horizontal) {
            case 0 -> 0;
            case 2 -> screenWidth - itemWidth;
            default -> (screenWidth - itemWidth) / 2;
        };
        return base + offsetX;
    }

    public int placeY(int itemHeight, int screenHeight, int offsetY) {
        int base = switch (vertical) {
            case 0 -> 0;
            case 2 -> screenHeight - itemHeight;
            default -> (screenHeight - itemHeight) / 2;
        };
        return base + offsetY;
    }

    public static Anchor parse(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-')
                .replaceAll("-+", "-");
        return switch (key) {
            case "top-left", "tl", "nw", "north-west", "left-top" -> TOP_LEFT;
            case "top-center", "tc", "n", "north", "center-top" -> TOP_CENTER;
            case "top-right", "tr", "ne", "north-east", "right-top" -> TOP_RIGHT;
            case "center-left", "cl", "w", "west", "left-center" -> CENTER_LEFT;
            case "center", "c", "middle", "center-center" -> CENTER;
            case "center-right", "cr", "e", "east", "right-center" -> CENTER_RIGHT;
            case "bottom-left", "bl", "sw", "south-west", "left-bottom" -> BOTTOM_LEFT;
            case "bottom-center", "bc", "s", "south", "center-bottom" -> BOTTOM_CENTER;
            case "bottom-right", "br", "se", "south-east", "right-bottom" -> BOTTOM_RIGHT;
            default -> throw new IllegalArgumentException(
                    "Unknown anchor '" + raw + "'. Use top-left, top-center, top-right, "
                            + "center-left, center, center-right, bottom-left, bottom-center or bottom-right.");
        };
    }
}
