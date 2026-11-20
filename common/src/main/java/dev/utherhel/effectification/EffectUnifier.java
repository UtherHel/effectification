package dev.utherhel.effectification;

import dev.utherhel.effectification.config.EffectMappingEntry;
import dev.utherhel.effectification.config.EffectificationConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

public class EffectUnifier {
    private final Map<String, EffectMappingEntry> staticSourceToMapping;
    private final List<EffectMappingEntry> staticWildcardMappings;
    private final Map<String, EffectMappingEntry> dynamicSourceToMapping;
    private final List<EffectMappingEntry> dynamicWildcardMappings;
    
    private final Map<String, EffectMappingEntry> wildcardMatchCache;
    private static final EffectMappingEntry NO_MAPPING = new EffectMappingEntry("", "", 1.0, false, false, "", "");
    
    private final List<String> discardPatterns;
    private final Map<String, Boolean> discardCache;
    private final Map<String, List<MobEffect>> extraEffectCache;

    public EffectUnifier(EffectificationConfig config) {
        this.staticSourceToMapping = new HashMap<>();
        this.staticWildcardMappings = new ArrayList<>();
        this.dynamicSourceToMapping = new HashMap<>();
        this.dynamicWildcardMappings = new ArrayList<>();
        this.wildcardMatchCache = new HashMap<>();
        this.discardPatterns = new ArrayList<>();
        this.discardCache = new HashMap<>();
        this.extraEffectCache = new HashMap<>();

        for (EffectMappingEntry mapping : config.getStaticMappings()) {
            if (mapping.isWildcard()) {
                staticWildcardMappings.add(mapping);
            } else {
                staticSourceToMapping.put(mapping.getSourceEffect(), mapping);
            }
            addDiscardPatterns(mapping);
        }
        
        for (EffectMappingEntry mapping : config.getDynamicMappings()) {
            if (mapping.isWildcard()) {
                dynamicWildcardMappings.add(mapping);
            } else {
                dynamicSourceToMapping.put(mapping.getSourceEffect(), mapping);
            }
            addDiscardPatterns(mapping);
        }
        
        EffectificationLogger.info("Loaded " + config.getStaticMappings().size() + " static mappings");
        EffectificationLogger.info("Loaded " + config.getDynamicMappings().size() + " dynamic mappings");
    }
    
    private void addDiscardPatterns(EffectMappingEntry mapping) {
        if (mapping.getDiscardEffectTick() != null && !mapping.getDiscardEffectTick().isEmpty()) {
            discardPatterns.addAll(mapping.getDiscardEffectTickList());
        }
    }
    
    public Map<String, EffectMappingEntry> getStaticMappings() {
        return new HashMap<>(staticSourceToMapping);
    }
    
    public List<EffectMappingEntry> getStaticWildcardMappings() {
        return new ArrayList<>(staticWildcardMappings);
    }
    
    // Gets all effects matching a wildcard pattern (cached)
    public List<MobEffect> getMatchingEffects(String pattern) {
        List<MobEffect> cached = extraEffectCache.get(pattern);
        if (cached != null) {
            return cached;
        }
        
        List<MobEffect> matching = new ArrayList<>();
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (id != null) {
                String effectId = id.toString();
                if (matchesPattern(effectId, pattern)) {
                    matching.add(effect);
                }
            }
        }
        
        extraEffectCache.put(pattern, matching);
        return matching;
    }
    public boolean shouldDiscardEffectTick(String effectId) {
        Boolean cached = discardCache.get(effectId);
        if (cached != null) {
            return cached;
        }
        
        boolean shouldDiscard = false;
        for (String pattern : discardPatterns) {
            if (matchesPattern(effectId, pattern)) {
                shouldDiscard = true;
                break;
            }
        }
        
        discardCache.put(effectId, shouldDiscard);
        return shouldDiscard;
    }
    public boolean matchesPattern(String effectId, String pattern) {
        if (effectId == null || pattern == null) {
            return false;
        }
        
        if (pattern.equals(effectId)) {
            return true;
        }
        
        if (pattern.indexOf('*') == -1) {
            return false;
        }
        
        String mainPattern = extractMainPattern(pattern);
        if (!matchesWildcardPattern(effectId, mainPattern)) {
            return false;
        }
        
        return !matchesAnyExclusion(effectId, pattern);
    }
    
    private String extractMainPattern(String pattern) {
        int exclamationIndex = pattern.indexOf('!');
        if (exclamationIndex == -1) {
            return pattern;
        }
        return pattern.substring(0, exclamationIndex).trim();
    }
    
    private boolean matchesAnyExclusion(String effectId, String pattern) {
        int exclamationIndex = pattern.indexOf('!');
        if (exclamationIndex == -1) {
            return false;
        }
        
        String exclusionPart = pattern.substring(exclamationIndex + 1);
        int start = 0;
        int nextExclamation;
        
        while ((nextExclamation = exclusionPart.indexOf('!', start)) != -1) {
            if (checkExclusion(effectId, exclusionPart, start, nextExclamation)) {
                return true;
            }
            start = nextExclamation + 1;
        }
        
        return checkExclusion(effectId, exclusionPart, start, exclusionPart.length());
    }
    
    private boolean checkExclusion(String effectId, String exclusionPart, int start, int end) {
        if (start >= end) {
            return false;
        }
        String exclusion = exclusionPart.substring(start, end).trim();
        return !exclusion.isEmpty() && matchesWildcardPattern(effectId, exclusion);
    }
    

    private boolean matchesWildcardPattern(String effectId, String pattern) {
        if (pattern == null || effectId == null || pattern.equals(effectId)) {
            return pattern != null && pattern.equals(effectId);
        }
        
        if (pattern.indexOf('*') == -1) {
            return false;
        }
        
        String[] effectParts = parseEffectId(effectId);
        String[] patternParts = parseEffectId(pattern);
        
        if (effectParts.length == 0 || patternParts.length == 0) {
            return false;
        }
        
        if (isDangerousPattern(patternParts)) {
            return false;
        }
        
        return matchesNamespace(effectParts[0], patternParts[0]) && 
               matchesPath(effectParts[1], patternParts[1]);
    }
    
    private String[] parseEffectId(String id) {
        int colonIndex = id.indexOf(':');
        if (colonIndex == -1 || colonIndex == id.length() - 1) {
            return new String[0];
        }
        return new String[] {
            id.substring(0, colonIndex),
            id.substring(colonIndex + 1)
        };
    }
    
    private boolean isDangerousPattern(String[] patternParts) {
        String namespace = patternParts[0];
        String path = patternParts[1];
        
        if (namespace.equals("*") && path.equals("*")) {
            return true;
        }
        return !namespace.equals("*") && path.equals("*");
    }
    
    private boolean matchesNamespace(String effectNamespace, String patternNamespace) {
        if (patternNamespace.equals("*")) {
            return true;
        }
        if (patternNamespace.indexOf('*') != -1) {
            return simpleWildcardMatch(effectNamespace, patternNamespace);
        }
        return patternNamespace.equals(effectNamespace);
    }
    
    private boolean matchesPath(String effectPath, String patternPath) {
        if (patternPath.equals("*")) {
            return true;
        }
        if (patternPath.indexOf('*') != -1) {
            return simpleWildcardMatch(effectPath, patternPath);
        }
        return patternPath.equals(effectPath);
    }
    
    // Simple wildcard matching without regex
    private boolean simpleWildcardMatch(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }
        
        int textIndex = 0;
        int patternIndex = 0;
        int lastStarIndex = -1;
        int textBacktrack = -1;
        
        while (textIndex < text.length()) {
            if (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
                lastStarIndex = patternIndex;
                textBacktrack = textIndex;
                patternIndex++;
            } else if (patternIndex < pattern.length() && 
                      (pattern.charAt(patternIndex) == text.charAt(textIndex))) {
                textIndex++;
                patternIndex++;
            } else if (lastStarIndex != -1) {
                patternIndex = lastStarIndex + 1;
                textBacktrack++;
                textIndex = textBacktrack;
            } else {
                return false;
            }
        }
        
        while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
            patternIndex++;
        }
        
        return patternIndex == pattern.length();
    }

    public boolean shouldUnify(String effectId) {
        // Check dynamic exact match
        if (dynamicSourceToMapping.containsKey(effectId)) {
            return true;
        }
        
        // Check static exact match
        if (staticSourceToMapping.containsKey(effectId)) {
            return true;
        }
        
        // Use getMapping which has caching for wildcard patterns
        return getMapping(effectId) != null;
    }

    public String getTargetEffect(String sourceEffect) {
        EffectMappingEntry mapping = getMapping(sourceEffect);
        return mapping != null ? mapping.getTargetEffect() : null;
    }

    public List<String> getTargetEffects(String sourceEffect) {
        EffectMappingEntry mapping = getMapping(sourceEffect);
        return mapping != null ? mapping.getTargetEffects() : null;
    }

    public EffectMappingEntry getMapping(String effectId) {
        EffectMappingEntry dynamicExact = dynamicSourceToMapping.get(effectId);
        if (dynamicExact != null) {
            return dynamicExact;
        }
        
        EffectMappingEntry staticExact = staticSourceToMapping.get(effectId);
        if (staticExact != null) {
            return staticExact;
        }
        
        if (wildcardMatchCache.containsKey(effectId)) {
            EffectMappingEntry cachedMapping = wildcardMatchCache.get(effectId);
            return cachedMapping == NO_MAPPING ? null : cachedMapping;
        }
        
        for (int i = dynamicWildcardMappings.size() - 1; i >= 0; i--) {
            EffectMappingEntry mapping = dynamicWildcardMappings.get(i);
            if (mapping.matches(effectId)) {
                wildcardMatchCache.put(effectId, mapping);
                return mapping;
            }
        }
        
        for (int i = staticWildcardMappings.size() - 1; i >= 0; i--) {
            EffectMappingEntry mapping = staticWildcardMappings.get(i);
            if (mapping.matches(effectId)) {
                wildcardMatchCache.put(effectId, mapping);
                return mapping;
            }
        }
        
        wildcardMatchCache.put(effectId, NO_MAPPING);
        return null;
    }

    public int convertAmplifier(String sourceEffect, int amplifier) {
        EffectMappingEntry mapping = getMapping(sourceEffect);
        if (mapping == null || mapping.isOnlyVisualChange()) {
            return amplifier;
        }
        
        int converted = (int) Math.round(amplifier * mapping.getAmplifierMultiplier());
        
        if (converted > 255) {
            EffectificationLogger.warn(
                "Amplifier overflow for effect " + sourceEffect + 
                ": " + amplifier + " * " + mapping.getAmplifierMultiplier() + 
                " = " + converted + " (clamped to 255)"
            );
            return 255;
        }
        
        if (converted < 0) {
            EffectificationLogger.warn(
                "Negative amplifier for effect " + sourceEffect + 
                ": " + amplifier + " * " + mapping.getAmplifierMultiplier() + 
                " = " + converted + " (clamped to 0)"
            );
            return 0;
        }
        
        return converted;
    }

    public List<EffectMappingEntry> getAllMappings() {
        List<EffectMappingEntry> allMappings = new ArrayList<>();
        allMappings.addAll(staticSourceToMapping.values());
        allMappings.addAll(staticWildcardMappings);
        allMappings.addAll(dynamicSourceToMapping.values());
        allMappings.addAll(dynamicWildcardMappings);
        return allMappings;
    }
}

