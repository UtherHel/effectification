package dev.utherhel.effectification;

import dev.utherhel.effectification.config.LogLevel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EffectificationLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("Effectification");

    /** Keys of the warnings already logged by {@link #warnOnce}; cleared when the mappings are reloaded. */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    
    private EffectificationLogger() {
    }
    
    private static LogLevel getLogLevel() {
        try {
            return Effectification.getConfig().debug.logLevel;
        } catch (Exception e) {
            return LogLevel.WARNINGS;
        }
    }
    
    public static void info(String message) {
        if (getLogLevel().shouldLogInfo()) {
            LOGGER.info(message);
        }
    }
    
    public static void warn(String message) {
        if (getLogLevel().shouldLogWarning()) {
            LOGGER.warn(message);
        }
    }

    /**
     * A warning from code that runs every tick or for every effect applied: logged once per key until the
     * mappings are reloaded, so a misconfigured mapping does not flood the log.
     */
    public static void warnOnce(String key, String message) {
        if (getLogLevel().shouldLogWarning() && WARNED.add(key)) {
            LOGGER.warn(message);
        }
    }

    /** The one-line overview of the loaded mappings: shown at every level except NONE. */
    public static void summary(String message) {
        if (getLogLevel().shouldLogError()) {
            LOGGER.info(message);
        }
    }

    static void resetWarnOnce() {
        WARNED.clear();
    }
    
    public static void error(String message) {
        if (getLogLevel().shouldLogError()) {
            LOGGER.error(message);
        }
    }
    
    public static void error(String message, Throwable throwable) {
        if (getLogLevel().shouldLogError()) {
            LOGGER.error(message, throwable);
        }
    }
}
