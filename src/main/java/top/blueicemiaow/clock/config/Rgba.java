package top.blueicemiaow.clock.config;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

/**
 * An RGBA color. Every component is in the inclusive range 0..255.
 */
public final class Rgba {

    private int r = 255;
    private int g = 255;
    private int b = 255;
    private int a = 255;

    public Rgba() {
    }

    public Rgba(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public static Rgba rgb(int r, int g, int b) {
        return new Rgba(r, g, b, 255);
    }

    public int red() {
        return r;
    }

    public int green() {
        return g;
    }

    public int blue() {
        return b;
    }

    public int alpha() {
        return a;
    }

    public Color toColor() {
        return new Color(r, g, b, a);
    }

    public void validate(String field, List<String> errors) {
        validateComponent(field + ".r", r, errors);
        validateComponent(field + ".g", g, errors);
        validateComponent(field + ".b", b, errors);
        validateComponent(field + ".a", a, errors);
    }

    private static void validateComponent(String field, int value, List<String> errors) {
        if (value < 0 || value > 255) {
            errors.add(field + " must be between 0 and 255 (was " + value + ")");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rgba rgba)) {
            return false;
        }
        return r == rgba.r && g == rgba.g && b == rgba.b && a == rgba.a;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, g, b, a);
    }

    @Override
    public String toString() {
        return String.format("#%02X%02X%02X%02X", r, g, b, a);
    }
}
