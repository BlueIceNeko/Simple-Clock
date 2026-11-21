package top.blueicemiaow.clock.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A configuration can either come from a file or from a classpath resource.
 */
public final class ConfigSource {

    public static final String DEFAULT_RESOURCE = "/default_config.json";
    public static final String FULL_RESOURCE = "/full_config.json";

    private final Path file;
    private final String resource;

    private ConfigSource(Path file, String resource) {
        this.file = file;
        this.resource = resource;
    }

    public static ConfigSource fromFile(Path file) {
        Objects.requireNonNull(file, "file");
        return new ConfigSource(file.toAbsolutePath().normalize(), null);
    }

    public static ConfigSource fromResource(String resource) {
        Objects.requireNonNull(resource, "resource");
        String normalized = resource.startsWith("/") ? resource : "/" + resource;
        return new ConfigSource(null, normalized);
    }

    /**
     * Uses an explicitly supplied file, otherwise ./config.json, otherwise the
     * bundled default configuration.
     */
    public static ConfigSource resolve(Path explicit) {
        if (explicit != null) {
            return fromFile(explicit);
        }
        Path local = Path.of("config.json").toAbsolutePath().normalize();
        if (Files.isRegularFile(local)) {
            return fromFile(local);
        }
        return fromResource(DEFAULT_RESOURCE);
    }

    public InputStream open() throws IOException {
        if (file != null) {
            return Files.newInputStream(file);
        }
        return openResource(resource);
    }

    /** Opens a bundled resource, for example {@link #DEFAULT_RESOURCE}. */
    public static InputStream openResource(String resource) throws IOException {
        InputStream stream = ConfigSource.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new FileNotFoundException("Bundled configuration not found: " + resource);
        }
        return stream;
    }

    public boolean isFile() {
        return file != null;
    }

    public Path file() {
        return file;
    }

    /** Directory used to resolve relative image paths, or null for classpath configs. */
    public Path baseDirectory() {
        return file == null ? null : file.getParent();
    }

    public String label() {
        return file != null ? file.toString() : "classpath:" + resource;
    }
}
