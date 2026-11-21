package dev.utherhel.effectification.config;

import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;

/**
 * List entry handler for {@link EffectMappingEntry}.
 * <p>
 * Fzzy Config 0.5.9 (Minecraft 1.20.4 - 1.20.6) deserializes every ValidatedAny list element into the
 * handler's single stored instance, so all mappings of a list end up as the same object (the last one).
 * There each element is read into a fresh EffectMappingEntry instead.
 */
public final class MappingEntryHandler {
    private MappingEntryHandler() {
    }

    public static ValidatedAny<EffectMappingEntry> create() {
        //? if >=1.20.2 && <1.21 {
        /*return new FreshInstance();
        *///?} else {
        return new ValidatedAny<>(new EffectMappingEntry());
        //?}
    }

    //? if >=1.20.2 && <1.21 {
    /*private static final class FreshInstance extends ValidatedAny<EffectMappingEntry> {
        FreshInstance() {
            super(new EffectMappingEntry());
        }

        @Override
        public me.fzzyhmstrs.fzzy_config.util.ValidationResult<EffectMappingEntry> deserialize(net.peanuuutz.tomlkt.TomlElement toml, String fieldName) {
            return me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.contextualize(
                me.fzzyhmstrs.fzzy_config.api.ConfigApi.deserializeFromToml(new EffectMappingEntry(), toml, new java.util.ArrayList<>()));
        }
    }
    *///?}
}
