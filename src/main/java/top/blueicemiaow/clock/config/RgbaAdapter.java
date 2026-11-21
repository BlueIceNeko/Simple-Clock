package top.blueicemiaow.clock.config;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Locale;

/**
 * Accepts RGBA colors as:
 * <ul>
 *     <li>{"r":255,"g":128,"b":0,"a":200}</li>
 *     <li>[255, 128, 0, 200] or [255, 128, 0] (alpha defaults to 255)</li>
 *     <li>"#FF8000C8", "#FF8000", "#F80C", "#F80"</li>
 * </ul>
 */
public final class RgbaAdapter extends TypeAdapter<Rgba> {

    @Override
    public void write(JsonWriter out, Rgba value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginArray();
        out.value(value.red());
        out.value(value.green());
        out.value(value.blue());
        out.value(value.alpha());
        out.endArray();
    }

    @Override
    public Rgba read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        return switch (token) {
            case NULL -> {
                in.nextNull();
                yield null;
            }
            case STRING -> parseHex(in.nextString());
            case BEGIN_ARRAY -> readArray(in);
            case BEGIN_OBJECT -> readObject(in);
            default -> throw new JsonSyntaxException(
                    "Expected a color as an array, object or hex string, but was " + token);
        };
    }

    private static Rgba readArray(JsonReader in) throws IOException {
        in.beginArray();
        Integer r = null;
        Integer g = null;
        Integer b = null;
        Integer a = null;
        int index = 0;
        while (in.hasNext()) {
            int component = readComponent(in, "color[" + index + "]");
            switch (index) {
                case 0 -> r = component;
                case 1 -> g = component;
                case 2 -> b = component;
                case 3 -> a = component;
                default -> throw new JsonSyntaxException("A color array must have 3 or 4 components");
            }
            index++;
        }
        in.endArray();
        if (index < 3) {
            throw new JsonSyntaxException("A color array must have 3 or 4 components");
        }
        return new Rgba(r, g, b, a == null ? 255 : a);
    }

    private static Rgba readObject(JsonReader in) throws IOException {
        in.beginObject();
        Integer r = null;
        Integer g = null;
        Integer b = null;
        Integer a = null;
        while (in.hasNext()) {
            String name = in.nextName().toLowerCase(Locale.ROOT);
            switch (name) {
                case "r", "red" -> r = readComponent(in, "color.r");
                case "g", "green" -> g = readComponent(in, "color.g");
                case "b", "blue" -> b = readComponent(in, "color.b");
                case "a", "alpha" -> a = readComponent(in, "color.a");
                default -> in.skipValue();
            }
        }
        in.endObject();
        if (r == null || g == null || b == null) {
            throw new JsonSyntaxException("A color object needs r, g and b components");
        }
        return new Rgba(r, g, b, a == null ? 255 : a);
    }

    private static int readComponent(JsonReader in, String field) throws IOException {
        if (in.peek() != JsonToken.NUMBER) {
            throw new JsonSyntaxException(field + " must be a number");
        }
        double value = in.nextDouble();
        if (!Double.isFinite(value)) {
            throw new JsonSyntaxException(field + " must be a finite number");
        }
        long rounded = Math.round(value);
        if (rounded < 0 || rounded > 255) {
            throw new JsonSyntaxException(field + " must be between 0 and 255");
        }
        return (int) rounded;
    }

    private static Rgba parseHex(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.startsWith("0x") || value.startsWith("0X")) {
            value = value.substring(2);
        }
        if (value.length() == 3 || value.length() == 4) {
            StringBuilder expanded = new StringBuilder(value.length() * 2);
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                expanded.append(c).append(c);
            }
            value = expanded.toString();
        }
        if (value.length() != 6 && value.length() != 8) {
            throw new JsonSyntaxException(
                    "Invalid color string '" + raw + "'. Use #RRGGBB, #RRGGBBAA, #RGB or #RGBA");
        }
        try {
            int r = Integer.parseInt(value.substring(0, 2), 16);
            int g = Integer.parseInt(value.substring(2, 4), 16);
            int b = Integer.parseInt(value.substring(4, 6), 16);
            int a = value.length() == 8 ? Integer.parseInt(value.substring(6, 8), 16) : 255;
            return new Rgba(r, g, b, a);
        } catch (NumberFormatException e) {
            throw new JsonSyntaxException(
                    "Invalid hex color '" + raw + "'. Use #RRGGBB, #RRGGBBAA, #RGB or #RGBA", e);
        }
    }
}
