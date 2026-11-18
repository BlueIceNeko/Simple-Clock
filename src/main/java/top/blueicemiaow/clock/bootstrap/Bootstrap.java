package top.blueicemiaow.clock.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Launcher for the jar-in-jar layout. The outer jar contains this class plus
 * everything under {@code lib/}. At startup the nested jars (the application
 * and Gson) are extracted into a small cache directory under the system temp
 * directory and loaded with an isolated class loader.
 */
public final class Bootstrap {

    private static final String APP_MAIN = "top.blueicemiaow.clock.Main";
    private static final String NESTED_PREFIX = "META-INF/jars/";

    private Bootstrap() {
    }

    public static void main(String[] args) throws Exception {
        List<URL> nestedJars = extractNestedJars();
        ClassLoader loader;
        if (nestedJars.isEmpty()) {
            // Running from an IDE or exploded classes: everything is already
            // on the application class path.
            loader = Bootstrap.class.getClassLoader();
        } else {
            loader = new URLClassLoader(
                    nestedJars.toArray(new URL[0]),
                    Bootstrap.class.getClassLoader().getParent());
        }

        Thread.currentThread().setContextClassLoader(loader);

        try {
            Class<?> appMain = Class.forName(APP_MAIN, true, loader);
            Method main = appMain.getMethod("main", String[].class);
            main.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }

    private static List<URL> extractNestedJars() throws IOException {
        URL location = Bootstrap.class.getProtectionDomain().getCodeSource().getLocation();
        if (location == null || !"file".equalsIgnoreCase(location.getProtocol())) {
            return List.of();
        }

        Path outerJar;
        try {
            outerJar = Path.of(location.toURI());
        } catch (Exception e) {
            return List.of();
        }
        if (!Files.isRegularFile(outerJar) || !outerJar.getFileName().toString().endsWith(".jar")) {
            return List.of();
        }

        try (JarFile jar = new JarFile(outerJar.toFile())) {
            List<JarEntry> entries = new ArrayList<>();
            Enumeration<JarEntry> all = jar.entries();
            while (all.hasMoreElements()) {
                JarEntry entry = all.nextElement();
                if (!entry.isDirectory()
                        && entry.getName().startsWith(NESTED_PREFIX)
                        && entry.getName().endsWith(".jar")) {
                    entries.add(entry);
                }
            }
            if (entries.isEmpty()) {
                return List.of();
            }
            entries.sort(Comparator.comparing(JarEntry::getName));

            Path cache = cacheDirectory(outerJar);
            try {
                return extract(jar, entries, cache);
            } catch (IOException e) {
                // A stale or locked cache must never prevent startup.
                Path fallback = Files.createTempDirectory("simple_clock_");
                return extract(jar, entries, fallback);
            }
        }
    }

    private static Path cacheDirectory(Path outerJar) throws IOException {
        long size = Files.size(outerJar);
        long modified = Files.getLastModifiedTime(outerJar).toMillis();
        String key = Long.toHexString(size) + "_" + Long.toHexString(modified);
        Path base = Path.of(System.getProperty("java.io.tmpdir", "."));
        return base.resolve("simple_clock").resolve(key);
    }

    private static List<URL> extract(JarFile jar, List<JarEntry> entries, Path directory)
            throws IOException {
        Files.createDirectories(directory);
        List<URL> urls = new ArrayList<>(entries.size());

        for (JarEntry entry : entries) {
            String name = Path.of(entry.getName()).getFileName().toString();
            Path target = directory.resolve(name).normalize();
            if (!target.startsWith(directory)) {
                throw new IOException("Unsafe nested jar entry: " + entry.getName());
            }

            if (!Files.isRegularFile(target) || Files.size(target) != entry.getSize()) {
                Path temporary = Files.createTempFile(directory, name + "_", ".tmp");
                try {
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, temporary, StandardCopyOption.REPLACE_EXISTING);
                    }
                    try {
                        Files.move(temporary, target,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
            urls.add(target.toUri().toURL());
        }
        return urls;
    }
}
