package top.blueicemiaow.clock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal command line parser.
 */
public record CliOptions(Path config, boolean check, boolean help,
                         boolean generateConfig, boolean generateFullConfig) {

    private static final String usage = """
            Simple Clock %s

            Usage:
              java -jar clock.jar [options] [config.json]

            Options:
              -c, --config <file>           Use this JSON config file
              -v, --check                   Validate the config and exit
              -g, --generate-config         Write the bundled slim default
                                            config to ./config.json
              -f, --generate-full-config    Write the bundled full default
                                            config to ./config.json
              -h, --help                    Show this help and exit

            If no file is supplied, ./config.json is used if present,
            otherwise the bundled default config is used.

            While running:
              Esc   exit
              F5    reload the config
              F8    minimize
              F11   toggle full-screen mode
            """.formatted(Main.version);

    public static CliOptions parse(String[] args) {
        Path config = null;
        boolean check = false;
        boolean help = false;
        boolean generateConfig = false;
        boolean generateFullConfig = false;
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h", "--help" -> help = true;
                case "-v", "--check" -> check = true;
                case "-g", "--generate-config" -> generateConfig = true;
                case "-f", "--generate-full-config" -> generateFullConfig = true;
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

        if (generateConfig && generateFullConfig) {
            throw new IllegalArgumentException(
                    "Use either --generate-config or --generate-full-config, not both");
        }
        if (generateConfig || generateFullConfig) {
            if (config != null) {
                throw new IllegalArgumentException(
                        "Generation options cannot be combined with --config");
            }
            if (check) {
                throw new IllegalArgumentException(
                        "Generation options cannot be combined with --check");
            }
        }

        return new CliOptions(config, check, help, generateConfig, generateFullConfig);
    }

    private static Path path(String raw, String option) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Empty path after " + option);
        }
        return Path.of(raw);
    }

    public static String usage() {
        return usage;
    }
}
