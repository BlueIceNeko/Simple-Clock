package top.blueicemiaow.clock.config;

import java.awt.Font;
import java.util.Locale;

public enum FontStyle {

    PLAIN(Font.PLAIN),
    BOLD(Font.BOLD),
    ITALIC(Font.ITALIC),
    BOLD_ITALIC(Font.BOLD | Font.ITALIC);

    private final int awtStyle;

    FontStyle(int awtStyle) {
        this.awtStyle = awtStyle;
    }

    public int awtStyle() {
        return awtStyle;
    }

    public static FontStyle parse(String raw) {
        String key = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
        key = key.replaceAll("\\s+", " ");
        return switch (key) {
            case "plain", "normal", "regular" -> PLAIN;
            case "bold" -> BOLD;
            case "italic", "oblique" -> ITALIC;
            case "bold italic", "italic bold", "bolditalic" -> BOLD_ITALIC;
            default -> throw new IllegalArgumentException(
                    "Unknown font style '" + raw + "'. Use plain, bold, italic or bold-italic.");
        };
    }
}
