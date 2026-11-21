package top.blueicemiaow.clock.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads JSON and turns it into a fully validated {@link LoadedConfig}.
 * <p>
 * Global settings must be valid, but individual layout items are treated
 * leniently: an item with an invalid definition (or an unreadable image) is
 * skipped with a warning instead of aborting startup.
 */
public final class ConfigLoader {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Rgba.class, new RgbaAdapter())
            .create();

    private ConfigLoader() {
    }

    public static LoadedConfig load(ConfigSource source) throws ConfigException {
        JsonObject root;
        try (InputStream in = source.open();
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new ConfigException("Configuration " + source.label()
                        + " must contain a JSON object", null);
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new ConfigException("Invalid JSON in " + source.label() + ": " + cleanMessage(e), e);
        } catch (IOException e) {
            throw new ConfigException("Cannot read configuration " + source.label() + ": " + e.getMessage(), e);
        }

        JsonElement layoutElement = root.get("layout");
        boolean hasLayout = layoutElement != null && !layoutElement.isJsonNull();

        // Parse the global settings without the layout so that one malformed
        // component cannot invalidate the whole file.
        JsonObject settings = root.deepCopy();
        settings.remove("layout");

        ClockConfig config;
        try {
            config = GSON.fromJson(settings, ClockConfig.class);
        } catch (RuntimeException e) {
            throw new ConfigException("Invalid global settings in " + source.label()
                    + ": " + cleanMessage(e), e);
        }
        if (config == null) {
            throw new ConfigException("Configuration " + source.label() + " is empty");
        }

        List<String> errors = config.validate();
        if (!errors.isEmpty()) {
            throw new ConfigException(formatErrors(source, errors), null);
        }

        ZoneId zone;
        try {
            zone = config.resolveZone();
        } catch (Exception e) {
            throw new ConfigException("Invalid zone '" + config.getZone() + "'", e);
        }

        ImageStore images = new ImageStore(source.baseDirectory());

        BufferedImage backgroundImage = null;
        String backgroundImagePath = config.getBackgroundImage();
        if (backgroundImagePath != null) {
            try {
                backgroundImage = images.load(backgroundImagePath);
            } catch (IOException e) {
                throw new ConfigException("backgroundImage: " + cleanMessage(e), e);
            }
        }

        List<LayoutItem> layout;
        if (!hasLayout) {
            layout = List.of(LayoutItem.time());
        } else if (layoutElement.isJsonArray()) {
            layout = parseLayout(layoutElement.getAsJsonArray(), images);
        } else {
            throw new ConfigException("layout in " + source.label() + " must be an array", null);
        }
        config.setLayout(layout);

        return new LoadedConfig(config, source, zone, images, backgroundImage);
    }

    /**
     * Parses and validates each element of the layout array. Elements that
     * cannot be parsed, fail validation or reference an unreadable image are
     * skipped with a warning. Items with {@code enabled: false} are skipped
     * silently. Valid elements keep their original order.
     */
    private static List<LayoutItem> parseLayout(JsonArray array, ImageStore images) {
        List<LayoutItem> kept = new ArrayList<>(array.size());
        int disabled = 0;
        for (int i = 0; i < array.size(); i++) {
            String field = "layout[" + i + "]";
            JsonElement element = array.get(i);
            if (element == null || element.isJsonNull()) {
                warn(field + " is null; component ignored");
                continue;
            }

            LayoutItem item;
            try {
                item = GSON.fromJson(element, LayoutItem.class);
            } catch (RuntimeException e) {
                warn(field + " could not be parsed: " + cleanMessage(e) + "; component ignored");
                continue;
            }
            if (item == null) {
                warn(field + " is empty; component ignored");
                continue;
            }

            if (!item.isEnabled()) {
                // Disabled intentionally: skip without a warning and without
                // validating the remaining fields.
                disabled++;
                continue;
            }

            List<String> itemErrors = new ArrayList<>();
            item.validate(i, itemErrors);
            if (!itemErrors.isEmpty()) {
                for (String error : itemErrors) {
                    warn(error + "; component ignored");
                }
                continue;
            }

            if (item.isImage()) {
                try {
                    images.load(item.getPath());
                } catch (Exception e) {
                    warn(field + ".path: " + cleanMessage(e) + "; component ignored");
                    continue;
                }
            }
            kept.add(item);
        }

        int dropped = array.size() - kept.size() - disabled;
        if (dropped > 0) {
            warn("ignored " + dropped + " invalid layout component(s); "
                    + kept.size() + " remaining");
        }
        return kept;
    }

    private static void warn(String message) {
        System.err.println("Warning: " + message);
    }

    private static String formatErrors(ConfigSource source, List<String> errors) {
        StringBuilder builder = new StringBuilder("Invalid configuration ").append(source.label()).append(':');
        for (String error : errors) {
            builder.append(System.lineSeparator()).append(" - ").append(error);
        }
        return builder.toString();
    }

    private static String cleanMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
