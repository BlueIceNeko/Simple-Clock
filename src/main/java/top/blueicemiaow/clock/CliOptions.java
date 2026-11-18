package top.blueicemiaow.clock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal command line parser.
 */
public record CliOptions(Path config, boolean check, boolean help) {

    public static CliOptions parse(String[] args) {
        Path config = null;
        boolean check = false;
        boolean help = false;
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h", "--help" -> help = true;
                case "--check" -> check = true;
                case "-c", "--config" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value after " + arg);
                    }
                    config = path(args[++i], arg);
                }
                default -> {
                    if (arg.startsWith("--config=")) {
                        config = path(arg.substring("--config=".length()), "--config");
                    } else if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    } else {
                        positional.add(arg);
                    }
                }
            }
        }

        if (positional.size() > 1) {
            throw new IllegalArgumentException("Only one config file may be supplied");
        }
        if (!positional.isEmpty()) {
            if (config != null) {
                throw new IllegalArgumentException("Use either --config or a positional config path, not both");
            }
            config = path(positional.get(0), "config path");
        }

        return new CliOptions(config, check, help);
    }

    private static Path path(String raw, String option) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Empty path after " + option);
        }
        return Path.of(raw);
    }

    public static String usage() {
        return """
                BlueIceNeko's full-screen clock

                Usage:
                  java -jar clock.jar [options] [config.json]

                Options:
                  -c, --config <file>   Use this JSON configuration file
                      --check          Validate the configuration and exit
                  -h, --help           Show this help

                If no file is supplied, ./config.json is used when present,
                otherwise the bundled default configuration is used.

                While running:
                  Esc   exit
                  F5    reload the configuration
                  F8    minimize
                  F11   toggle borderless full screen / decorated window
                """;
    }
}
