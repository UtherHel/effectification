package dev.utherhel.effectification.config;

import dev.utherhel.effectification.Effectification;
import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;

@Translation(prefix = Effectification.MOD_ID + ".config.debug")
public class DebugSection extends ConfigSection {
    
    @Comment("Logging level: NONE, ERRORS, WARNINGS, ALL")
    public LogLevel logLevel = LogLevel.ERRORS;
}
