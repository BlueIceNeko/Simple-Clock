package top.blueicemiaow.clock.config;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and caches images referenced by layout items.
 */
public final class ImageStore {

    private final Path baseDirectory;
    private final Map<String, BufferedImage> cache = new HashMap<>();

    public ImageStore(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public void loadAll(List<LayoutItem> layout) throws ConfigException {
        for (int i = 0; i < layout.size(); i++) {
            LayoutItem item = layout.get(i);
            if (item != null && item.isImage()) {
                try {
                    load(item.getPath());
                } catch (Exception e) {
                    throw new ConfigException("layout[" + i + "].path: " + e.getMessage(), e);
                }
            }
        }
    }

    public BufferedImage get(LayoutItem item) {
        if (item == null || item.getPath() == null) {
            return null;
        }
        return cache.get(item.getPath());
    }

    public BufferedImage load(String rawPath) throws IOException {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IOException("Image path must not be blank");
        }
        String path = rawPath.trim();
        BufferedImage cached = cache.get(path);
        if (cached != null) {
            return cached;
        }

        BufferedImage image = read(path);
        if (image == null) {
            throw new IOException("Unsupported image format: " + path);
        }
        cache.put(path, image);
        return image;
    }

    private BufferedImage read(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            return readResource(path.substring("classpath:".length()));
        }
        if (path.startsWith("file:")) {
            String withoutScheme = path.substring("file:".length());
            try {
                Path uriPath = Path.of(URI.create(path));
                if (Files.isRegularFile(uriPath)) {
                    return readFile(uriPath);
                }
            } catch (Exception ignored) {
                // "file:relative/path.png" is not a valid file URI; fall through.
            }
            if (!withoutScheme.isBlank()) {
                return readRelativeOrResource(withoutScheme);
            }
        }
        return readRelativeOrResource(path);
    }

    private BufferedImage readRelativeOrResource(String path) throws IOException {
        if (baseDirectory != null) {
            Path candidate = baseDirectory.resolve(path).normalize();
            if (Files.isRegularFile(candidate)) {
                return readFile(candidate);
            }
        }
        Path direct = Path.of(path);
        if (Files.isRegularFile(direct)) {
            return readFile(direct);
        }
        return readResource(path);
    }

    private BufferedImage readResource(String resource) throws IOException {
        String name = resource.startsWith("/") ? resource.substring(1) : resource;
        try (InputStream in = ImageStore.class.getResourceAsStream("/" + name)) {
            if (in == null) {
                throw new IOException("Image not found: " + resource
                        + " (looked in the config directory, working directory and classpath)");
            }
            return ImageIO.read(in);
        }
    }

    private static BufferedImage readFile(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return ImageIO.read(in);
        }
    }
}
