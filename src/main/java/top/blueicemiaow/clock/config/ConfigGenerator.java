package top.blueicemiaow.clock.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes one of the bundled configurations to disk.
 */
public final class ConfigGenerator {

    private ConfigGenerator() {
    }

    /**
     * Copies the bundled configuration to {@code destination}.
     *
     * @param full        {@code true} for {@link ConfigSource#FULL_RESOURCE},
     *                    {@code false} for {@link ConfigSource#DEFAULT_RESOURCE}
     * @param destination file to create; it must not exist yet
     * @throws java.nio.file.FileAlreadyExistsException when the file exists
     */
    public static void generate(boolean full, Path destination) throws IOException {
        String resource = full ? ConfigSource.FULL_RESOURCE : ConfigSource.DEFAULT_RESOURCE;
        if (Files.exists(destination)) {
            throw new java.nio.file.FileAlreadyExistsException(destination.toString());
        }
        try (InputStream in = ConfigSource.openResource(resource)) {
            Files.copy(in, destination);
        }
    }
}
