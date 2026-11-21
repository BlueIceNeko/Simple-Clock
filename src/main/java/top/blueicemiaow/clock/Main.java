package top.blueicemiaow.clock;

import top.blueicemiaow.clock.config.ConfigException;
import top.blueicemiaow.clock.config.ConfigGenerator;
import top.blueicemiaow.clock.config.ConfigLoader;
import top.blueicemiaow.clock.config.ConfigSource;
import top.blueicemiaow.clock.config.LayoutItem;
import top.blueicemiaow.clock.config.LoadedConfig;
import top.blueicemiaow.clock.render.TimeText;
import top.blueicemiaow.clock.ui.ClockWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;

public final class Main {

    public static final String version = "1.1.0";

    private Main() {
    }

    public static void main(String[] args) {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println(CliOptions.usage());
            System.exit(2);
            return;
        }

        if (options.help()) {
            System.out.println(CliOptions.usage());
            return;
        }

        if (options.generateConfig() || options.generateFullConfig()) {
            generateConfig(options.generateFullConfig());
            return;
        }

        ConfigSource source = ConfigSource.resolve(options.config());
        LoadedConfig loaded;
        try {
            loaded = ConfigLoader.load(source);
        } catch (ConfigException e) {
            reportError(e.getMessage(), options.check());
            System.exit(1);
            return;
        }

        if (options.check()) {
            System.out.println("Configuration OK: " + source.label());
            System.out.println("Title: " + loaded.config().getTitle());
            System.out.println("Layout items: " + loaded.config().getLayout().size());
            for (int i = 0; i < loaded.config().getLayout().size(); i++) {
                LayoutItem item = loaded.config().getLayout().get(i);
                if (item == null) {
                    continue;
                }
                if (item.isText() && item.getText() != null) {
                    if (item.getText().contains(TimeText.PLACEHOLDER_TIME)) {
                        System.out.println("  [" + i + "] time format: " + loaded.timePattern(item));
                    }
                    if (item.getText().contains(TimeText.PLACEHOLDER_DATE)) {
                        System.out.println("  [" + i + "] date format: " + loaded.datePattern(item));
                    }
                    if (item.getText().contains(TimeText.PLACEHOLDER_WEEK)) {
                        System.out.println("  [" + i + "] week names: "
                                + String.join(", ", loaded.weekDef(item)));
                    }
                }
            }
            System.out.println("Zone: " + loaded.zone());
            return;
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("The clock needs a graphical environment. "
                    + "Use --check to validate a configuration without opening a window.");
            System.exit(1);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                ClockWindow.show(loaded, source);
            } catch (RuntimeException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Unable to open the clock window: " + e.getMessage(),
                        "Clock error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    private static void generateConfig(boolean full) {
        Path destination = Path.of("config.json").toAbsolutePath().normalize();
        try {
            ConfigGenerator.generate(full, destination);
            System.out.println("Created " + destination + " from the bundled "
                    + (full ? "full" : "default") + " configuration.");
        } catch (FileAlreadyExistsException e) {
            System.err.println("Refusing to overwrite existing " + destination);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Unable to write " + destination + ": " + e.getMessage());
            System.exit(1);
        }
    }

    private static void reportError(String message, boolean checkOnly) {
        System.err.println(message);
        if (!checkOnly && !GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, message,
                    "Clock configuration error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
