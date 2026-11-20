package dev.utherhel.effectification.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized default mappings for easy modification and maintenance.
 * Contains presets for both static (registry aliasing) and dynamic (runtime conversion) mappings.
 */
public class DefaultMappings {
    
    private DefaultMappings() {
        // Utility class - prevent instantiation
    }
    
    // ========== STATIC MAPPINGS (Registry Aliasing) ==========
    // These require restart and provide zero runtime overhead
    
    /**
     * Default static mappings for Fabric platform.
     * Fabric users typically prefer to configure their own mappings.
     */
    public static List<EffectMappingEntry> getFabricStaticDefaults() {
        return new ArrayList<>(); // Empty - let users configure
    }
    
    /**
     * Default static mappings for Forge/NeoForge platform.
     * These use registry aliasing for zero runtime overhead.
     */
    public static List<EffectMappingEntry> getForgeStaticDefaults() {
        return new ArrayList<>(); // Empty - use dynamic mappings instead
    }
    
    // ========== DYNAMIC MAPPINGS (Runtime Conversion) ==========
    // These can be reloaded without restart but have minimal runtime overhead
    
    /**
     * Default dynamic mappings for Fabric platform.
     */
    public static List<EffectMappingEntry> getFabricDynamicDefaults() {
        return new ArrayList<>(); // Empty by default
    }
    
    /**
     * Default dynamic mappings for Forge/NeoForge platform.
     */
    public static List<EffectMappingEntry> getForgeDynamicDefaults() {
        List<EffectMappingEntry> mappings = new ArrayList<>();
        
        // Bleeding effects
        mappings.add(advancedMapping(
            "*:bleeding* !*:bleeding*_immunity !*:bleeding*_resistance",
            "majruszsdifficulty:bleeding, attributeslib:bleeding, valoria:bleeding",
            1.0, false, false, "", ""
        ));
        
        // Stun effects
        mappings.add(advancedMapping(
            "*:stun* !*:stun*_resistance !*:stun*_immunity",
            "relics:stun, alexscaves:stunned",
            1.0, false, false, "", ""
        ));
        
        // Freeze effects
        mappings.add(advancedMapping(
            "*:froz* !*:froz*_resistance !*:froz*_immunity",
            "mowziesmobs:frozen, more_rpg_classes:frozen_solid",
            1.0, false, false, "", ""
        ));
        
        // Chill effects
        mappings.add(advancedMapping(
            "*:chill* !*:chill*_resistance !*:chill*_immunity",
            "irons_spellbooks:chilled",
            1.0, false, false, "", ""
        ));
        
        // Immolate effects
        mappings.add(advancedMapping(
            "*:immolat* !*:immolat*_resistance !*:immolat*_immunity",
            "irons_spellbooks:immolate",
            1.0, false, false, "", ""
        ));
        
        // Confusion effects
        mappings.add(advancedMapping(
            "*:confusion* !*:confusion*_resistance !*:confusion*_immunity",
            "relics:confusion",
            1.0, false, false, "", ""
        ));
        
        // Knowledge effects
        mappings.add(advancedMapping(
            "*:knowledge* !*:knowledge*_resistance !*:knowledge*_immunity",
            "attributeslib:knowledge",
            1.0, false, false, "", ""
        ));
        
        // Sapped effects
        mappings.add(advancedMapping(
            "*:sapped* !*:sapped*_resistance !*:sapped*_immunity",
            "goety:sapped",
            1.0, false, false, "", ""
        ));
        
        // Specific mod cross-mappings
        mappings.add(advancedMapping(
            "more_rpg_classes:frozen_solid",
            "mowziesmobs:frozen",
            1.0, false, false, "", ""
        ));
        
        mappings.add(advancedMapping(
            "more_rpg_classes:frosted",
            "mowziesmobs:frozen, more_rpg_classes:frozen_solid",
            1.0, false, false, "", ""
        ));
        
        mappings.add(advancedMapping(
            "netherdepthsupgrade:lava_vision",
            "alexsmobs:lava_vision",
            1.0, false, false, "", ""
        ));
        
        mappings.add(advancedMapping(
            "alexscaves:stunned",
            "relics:stun",
            1.0, false, false, "", ""
        ));
        
        mappings.add(advancedMapping(
            "attributeslib:bleeding",
            "majruszsdifficulty:bleeding",
            1.0, false, false, "", ""
        ));
        
        mappings.add(advancedMapping(
            "valoria:bleeding",
            "majruszsdifficulty:bleeding, attributeslib:bleeding",
            1.0, false, false, "", ""
        ));
        
        return mappings;
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Creates a mapping with all options.
     */
    private static EffectMappingEntry advancedMapping(
            String source, 
            String target, 
            double multiplier,
            boolean visualOnly, 
            boolean unidirectional,
            String extraTick,
            String discardTick) {
        return new EffectMappingEntry(source, target, multiplier, visualOnly, unidirectional, extraTick, discardTick);
    }
    
    // ========== EXAMPLE PRESETS ==========
    
    /**
     * Example: Bleeding effects with amplifier scaling.
     */
    public static List<EffectMappingEntry> getBleedingPreset() {
        List<EffectMappingEntry> preset = new ArrayList<>();
        
        preset.add(advancedMapping(
            "*:bleeding* !*:bleeding*_immunity",
            "majruszsdifficulty:bleeding, attributeslib:bleeding",
            1.5, false, false, "", ""
        ));
        
        return preset;
    }
    
    /**
     * Example: Stun effects with unidirectional mode to prevent visual conflicts.
     */
    public static List<EffectMappingEntry> getStunPreset() {
        List<EffectMappingEntry> preset = new ArrayList<>();
        
        preset.add(advancedMapping(
            "*:stun*",
            "relics:stun",
            1.0, false, true, "", ""
        ));
        
        return preset;
    }
    
    /**
     * Example: Freeze effects with extra visual ticks.
     */
    public static List<EffectMappingEntry> getFreezePreset() {
        List<EffectMappingEntry> preset = new ArrayList<>();
        
        preset.add(advancedMapping(
            "*:freez*",
            "mowziesmobs:frozen",
            1.0, false, false, "irons_spellbooks:chilled", ""
        ));
        
        return preset;
    }
    
    /**
     * Example: Visual-only mapping that doesn't change mechanics.
     */
    public static List<EffectMappingEntry> getVisualOnlyPreset() {
        List<EffectMappingEntry> preset = new ArrayList<>();
        
        preset.add(advancedMapping(
            "modid:custom_effect",
            "minecraft:poison",
            1.0, true, false, "", ""
        ));
        
        return preset;
    }
}
