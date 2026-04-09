package dev.utherhel.effectification.util;

/**
 * Interface for objects that have caches that need to be cleared when config changes
 */
public interface CacheCleanable {
    void effectification$clearCache();
}
