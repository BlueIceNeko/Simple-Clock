package top.blueicemiaow.clock.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.List;

/**
 * Reads JSON and turns it into a fully validated {@link LoadedConfig}.
 */
public final class ConfigLoader {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Rgba.class, new RgbaAdapter())
            .create();

    private ConfigLoader() {
    }

    public static LoadedConfig load(ConfigSource source) throws ConfigException {
        ClockConfig config;
        try (InputStream in = source.open();
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            config = GSON.fromJson(reader, ClockConfig.class);
        } catch (JsonParseException e) {
            throw new ConfigException("Invalid JSON in " + source.label() + ": " + cleanMessage(e), e);
        } catch (IOException e) {
            throw new ConfigException("Cannot read configuration " + source.label() + ": " + e.getMessage(), e);
        }

        if (config == null) {
            throw new ConfigException("Configuration " + source.label() + " is empty");
        }
        if (config.getLayout() == null) {
            config.setLayout(List.of(LayoutItem.time()));
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
        images.loadAll(config.getLayout());

        return new LoadedConfig(config, source, zone, images);
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
