package dev.utherhel.effectification.config;

import dev.utherhel.effectification.Effectification;
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jetbrains.annotations.NotNull;

public enum LogLevel implements EnumTranslatable {
    NONE,
    ERRORS,
    WARNINGS,
    ALL;
    
    @NotNull
    @Override
    public String prefix() {
        return Effectification.MOD_ID + ".config.logLevel";
    }
    
    public boolean shouldLogError() {
        return this != NONE;
    }
    
    public boolean shouldLogWarning() {
        return this == WARNINGS || this == ALL;
    }
    
    public boolean shouldLogInfo() {
        return this == ALL;
    }
}
