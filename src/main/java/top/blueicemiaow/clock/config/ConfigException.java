package top.blueicemiaow.clock.config;

/**
 * Raised when a configuration cannot be read, parsed or validated.
 */
public final class ConfigException extends Exception {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
