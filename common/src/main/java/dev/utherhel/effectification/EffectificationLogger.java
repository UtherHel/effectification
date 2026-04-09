package dev.utherhel.effectification;

import dev.utherhel.effectification.config.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EffectificationLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("Effectification");
    
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
