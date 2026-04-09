package dev.utherhel.effectification.config;

import dev.utherhel.effectification.Effectification;
import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.util.AllowableStrings;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Translation(prefix = Effectification.MOD_ID + ".config.mapping")
public class EffectMappingEntry extends ConfigSection {
    
    private static final String DEFAULT_SOURCE = "modid:effect_name";
    private static final String DEFAULT_TARGET = "modid:target_effect";
    
    private static final AllowableStrings EFFECT_SUGGESTIONS_WITH_WILDCARDS = EffectSuggestions.create(true);
    private static final AllowableStrings EFFECT_SUGGESTIONS_NO_WILDCARDS = EffectSuggestions.create(false);
    
    // Cached parsed values
    private String cachedSourceEffect;
    private String cachedMainPattern;
    private List<String> cachedExclusionPatterns;
    private Boolean cachedIsWildcard;
    
    private String cachedTargetEffect;
    private List<String> cachedTargetEffects;
    private String cachedExtraApplyEffectTick;
    private List<String> cachedExtraApplyEffectTickList;
    private String cachedDiscardEffectTick;
    private List<String> cachedDiscardEffectTickList;
    
    private Map<String, Boolean> matchCache;
    
    private StatusEffect cachedResolvedTargetEffect;
    private boolean targetEffectResolved = false;
    
    @Comment("Source effect pattern: *:bleeding, *:bleeding*, modid:effect*, *:bleeding* !*:immunity")
    public ValidatedString sourceEffect;
    
    @Comment("Target effect (comma-separated for fallback): primary:effect, fallback:effect")
    public ValidatedString targetEffect;
    
    @Comment("Amplifier multiplier")
    public double amplifierMultiplier = 1.0;
    
    @Comment("Visual change only (no amplifier conversion)")
    public boolean onlyVisualChange = false;
    
    @Comment("Unidirectional (prevents source mod handlers)")
    public boolean unidirectional = false;
    
    @Comment("Extra effects to call applyUpdateEffect() (comma-separated)")
    public ValidatedString extraApplyEffectTick;
    
    @Comment("Effects to block applyUpdateEffect() (comma-separated, supports wildcards)")
    public ValidatedString discardEffectTick;
    
    public EffectMappingEntry() {
        this.sourceEffect = new ValidatedString(DEFAULT_SOURCE, EFFECT_SUGGESTIONS_WITH_WILDCARDS);
        this.targetEffect = new ValidatedString(DEFAULT_TARGET, EFFECT_SUGGESTIONS_NO_WILDCARDS);
        this.amplifierMultiplier = 1.0;
        this.onlyVisualChange = false;
        this.unidirectional = false;
        this.extraApplyEffectTick = new ValidatedString("", EFFECT_SUGGESTIONS_NO_WILDCARDS);
        this.discardEffectTick = new ValidatedString("", EFFECT_SUGGESTIONS_WITH_WILDCARDS);
        this.setTranslatableEntryKey(Effectification.MOD_ID + ".config.mapping");
    }
    
    public EffectMappingEntry(String sourceEffect, String targetEffect) {
        this(sourceEffect, targetEffect, 1.0, false, false, "", "");
    }
    
    public EffectMappingEntry(String sourceEffect, String targetEffect, double amplifierMultiplier,
                             boolean onlyVisualChange, boolean unidirectional) {
        this(sourceEffect, targetEffect, amplifierMultiplier, onlyVisualChange, unidirectional, "", "");
    }
    
    public EffectMappingEntry(String sourceEffect, String targetEffect, double amplifierMultiplier,
                             boolean onlyVisualChange, boolean unidirectional, 
                             String extraApplyEffectTick, String discardEffectTick) {
        this.sourceEffect = new ValidatedString(sourceEffect, EFFECT_SUGGESTIONS_WITH_WILDCARDS);
        this.targetEffect = new ValidatedString(targetEffect, EFFECT_SUGGESTIONS_NO_WILDCARDS);
        this.amplifierMultiplier = amplifierMultiplier;
        this.onlyVisualChange = onlyVisualChange;
        this.unidirectional = unidirectional;
        this.extraApplyEffectTick = new ValidatedString(extraApplyEffectTick != null ? extraApplyEffectTick : "", EFFECT_SUGGESTIONS_NO_WILDCARDS);
        this.discardEffectTick = new ValidatedString(discardEffectTick != null ? discardEffectTick : "", EFFECT_SUGGESTIONS_WITH_WILDCARDS);
        this.setTranslatableEntryKey(Effectification.MOD_ID + ".config.mapping");
    }
    
    @Override
        public String translationKey() {
            String source = getSourceEffect();
            String target = getTargetEffect();

            if (isDefaultMapping(source, target)) {
                return Effectification.MOD_ID + ".config.mapping.new";
            }

            return buildMappingSummary(source, target);
        }

        private boolean isDefaultMapping(String source, String target) {
            return (source == null || source.isEmpty() || source.equals(DEFAULT_SOURCE)) &&
                   (target == null || target.isEmpty() || target.equals(DEFAULT_TARGET));
        }

        private String buildMappingSummary(String source, String target) {
            StringBuilder summary = new StringBuilder();

            summary.append(source != null && !source.isEmpty() && !source.equals(DEFAULT_SOURCE) ? source : "???");
            summary.append(" → ");
            summary.append(formatTarget(target));

            appendModifiers(summary);

            return summary.toString();
        }

        private String formatTarget(String target) {
            if (target == null || target.isEmpty() || target.equals(DEFAULT_TARGET)) {
                return "???";
            }
            return target.length() > 30 ? target.substring(0, 27) + "..." : target;
        }

        private void appendModifiers(StringBuilder summary) {
            if (getAmplifierMultiplier() != 1.0) {
                summary.append(" [×").append(String.format("%.1f", getAmplifierMultiplier())).append("]");
            }
            if (isOnlyVisualChange()) {
                summary.append(" [visual]");
            }
            if (isUnidirectional()) {
                summary.append(" [uni]");
            }
        }

    
    @Override
    public String descriptionKey() {
        return Effectification.MOD_ID + ".config.mapping.desc";
    }
    
    @Override
    public boolean hasDescription() {
        return true;
    }
    
    @Override
    public MutableText description(String fallback) {
        MutableText desc = Text.empty();
        
        desc.append(Text.literal("Source: ").formatted(Formatting.GRAY));
        desc.append(Text.literal(getSourceEffect()).formatted(Formatting.WHITE));
        desc.append("\n");
        
        desc.append(Text.literal("Target: ").formatted(Formatting.GRAY));
        desc.append(Text.literal(getTargetEffect()).formatted(Formatting.WHITE));
        desc.append("\n");
        
        desc.append(Text.literal("Multiplier: ").formatted(Formatting.GRAY));
        desc.append(Text.literal(String.valueOf(getAmplifierMultiplier())).formatted(Formatting.WHITE));
        desc.append("\n");
        
        desc.append(Text.literal("Visual Only: ").formatted(Formatting.GRAY));
        desc.append(Text.literal(String.valueOf(isOnlyVisualChange())).formatted(Formatting.WHITE));
        desc.append("\n");
        
        desc.append(Text.literal("Unidirectional: ").formatted(Formatting.GRAY));
        desc.append(Text.literal(String.valueOf(isUnidirectional())).formatted(Formatting.WHITE));
        
        if (getExtraApplyEffectTick() != null && !getExtraApplyEffectTick().isEmpty()) {
            desc.append("\n");
            desc.append(Text.literal("Extra Tick: ").formatted(Formatting.GRAY));
            desc.append(Text.literal(getExtraApplyEffectTick()).formatted(Formatting.WHITE));
        }
        
        if (getDiscardEffectTick() != null && !getDiscardEffectTick().isEmpty()) {
            desc.append("\n");
            desc.append(Text.literal("Discard Tick: ").formatted(Formatting.GRAY));
            desc.append(Text.literal(getDiscardEffectTick()).formatted(Formatting.WHITE));
        }
        
        return desc;
    }
    
    public String getSourceEffect() {
        return sourceEffect.get();
    }
    
    public String getTargetEffect() {
        return targetEffect.get();
    }
    
    public List<String> getTargetEffects() {
        String targetEffectValue = targetEffect.get();
        if (cachedTargetEffect != null && cachedTargetEffect.equals(targetEffectValue)) {
            return cachedTargetEffects;
        }
        
        cachedTargetEffect = targetEffectValue;
        cachedTargetEffects = new ArrayList<>();
        
        targetEffectResolved = false;
        cachedResolvedTargetEffect = null;
        
        if (targetEffectValue != null && !targetEffectValue.isEmpty()) {
            String[] parts = targetEffectValue.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    cachedTargetEffects.add(trimmed);
                }
            }
        }
        
        return cachedTargetEffects;
    }
    
    public StatusEffect getResolvedTargetEffect() {
        if (targetEffectResolved) {
            return cachedResolvedTargetEffect;
        }
        
        cachedResolvedTargetEffect = dev.utherhel.effectification.util.EffectUtils.getFirstAvailableEffect(getTargetEffects());
        targetEffectResolved = true;
        
        return cachedResolvedTargetEffect;
    }
    
    public double getAmplifierMultiplier() {
        return amplifierMultiplier;
    }
    
    public boolean isOnlyVisualChange() {
        return onlyVisualChange;
    }
    
    public boolean isUnidirectional() {
        return unidirectional;
    }
    
    public String getExtraApplyEffectTick() {
        return extraApplyEffectTick.get();
    }
    
    public List<String> getExtraApplyEffectTickList() {
        String extraApplyEffectTickValue = extraApplyEffectTick.get();
        if (cachedExtraApplyEffectTick != null && cachedExtraApplyEffectTick.equals(extraApplyEffectTickValue)) {
            return cachedExtraApplyEffectTickList;
        }
        
        cachedExtraApplyEffectTick = extraApplyEffectTickValue;
        cachedExtraApplyEffectTickList = new ArrayList<>();
        
        if (extraApplyEffectTickValue != null && !extraApplyEffectTickValue.isEmpty()) {
            String[] parts = extraApplyEffectTickValue.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    cachedExtraApplyEffectTickList.add(trimmed);
                }
            }
        }
        
        return cachedExtraApplyEffectTickList;
    }
    
    public String getDiscardEffectTick() {
        return discardEffectTick.get();
    }
    
    public List<String> getDiscardEffectTickList() {
        String discardEffectTickValue = discardEffectTick.get();
        if (cachedDiscardEffectTick != null && cachedDiscardEffectTick.equals(discardEffectTickValue)) {
            return cachedDiscardEffectTickList;
        }
        
        cachedDiscardEffectTick = discardEffectTickValue;
        cachedDiscardEffectTickList = new ArrayList<>();
        
        if (discardEffectTickValue != null && !discardEffectTickValue.isEmpty()) {
            String[] parts = discardEffectTickValue.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    cachedDiscardEffectTickList.add(trimmed);
                }
            }
        }
        
        return cachedDiscardEffectTickList;
    }
    
    private void updateCacheIfNeeded() {
            String sourceEffectValue = sourceEffect.get();
            if (cachedSourceEffect != null && cachedSourceEffect.equals(sourceEffectValue)) {
                return;
            }

            cachedSourceEffect = sourceEffectValue;

            if (matchCache != null) {
                matchCache.clear();
            }

            parseMainPattern();
            parseExclusionPatterns();

            cachedIsWildcard = cachedMainPattern != null && cachedMainPattern.contains("*");
        }

        private void parseMainPattern() {
            String sourceEffectValue = sourceEffect.get();
            if (sourceEffectValue == null) {
                cachedMainPattern = null;
                return;
            }

            int exclamationIndex = sourceEffectValue.indexOf('!');
            cachedMainPattern = exclamationIndex == -1 
                ? sourceEffectValue.trim() 
                : sourceEffectValue.substring(0, exclamationIndex).trim();
        }

        private void parseExclusionPatterns() {
            cachedExclusionPatterns = new ArrayList<>();

            String sourceEffectValue = sourceEffect.get();
            if (sourceEffectValue == null) {
                return;
            }

            int exclamationIndex = sourceEffectValue.indexOf('!');
            if (exclamationIndex == -1) {
                return;
            }

            String exclusionPart = sourceEffectValue.substring(exclamationIndex + 1);
            String[] parts = exclusionPart.split("!");

            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    cachedExclusionPatterns.add(trimmed);
                }
            }
        }

    
    private String getMainPattern() {
        updateCacheIfNeeded();
        return cachedMainPattern;
    }
    
    private List<String> getExclusionPatterns() {
        updateCacheIfNeeded();
        return cachedExclusionPatterns;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSourceEffect()).append(" -> ").append(getTargetEffect());
        if (getAmplifierMultiplier() != 1.0) {
            sb.append(" (x").append(getAmplifierMultiplier()).append(")");
        }
        if (isOnlyVisualChange()) {
            sb.append(" [visual only]");
        }
        if (isUnidirectional()) {
            sb.append(" [unidirectional]");
        }
        if (getExtraApplyEffectTick() != null && !getExtraApplyEffectTick().isEmpty()) {
            sb.append(" [extra tick: ").append(getExtraApplyEffectTick()).append("]");
        }
        if (getDiscardEffectTick() != null && !getDiscardEffectTick().isEmpty()) {
            sb.append(" [discard tick: ").append(getDiscardEffectTick()).append("]");
        }
        return sb.toString();
    }
    
    public boolean isWildcard() {
        updateCacheIfNeeded();
        return cachedIsWildcard != null && cachedIsWildcard;
    }
    
    public boolean matches(String effectId) {
        if (effectId == null) {
            return false;
        }
        
        Boolean cached = getCachedMatch(effectId);
        if (cached != null) {
            return cached;
        }

        boolean result = performMatch(effectId);
        cacheMatchResult(effectId, result);
        return result;
    }
    
    private Boolean getCachedMatch(String effectId) {
        return matchCache != null ? matchCache.get(effectId) : null;
    }
    
    private boolean performMatch(String effectId) {
        String mainPattern = getMainPattern();
        if (mainPattern == null) {
            return false;
        }

        if (mainPattern.equals(effectId)) {
            return true;
        }

        if (!mainPattern.contains("*")) {
            return false;
        }

        if (!matchesPattern(effectId, mainPattern)) {
            return false;
        }

        return !isExcluded(effectId);
    }
    
    private boolean isExcluded(String effectId) {
        List<String> exclusions = getExclusionPatterns();
        for (String exclusion : exclusions) {
            if (matchesPattern(effectId, exclusion)) {
                return true;
            }
        }
        return false;
    }
    
    private void cacheMatchResult(String effectId, boolean result) {
        if (matchCache == null) {
            matchCache = new java.util.HashMap<>();
        }
        matchCache.put(effectId, result);
    }
    
    @Override
        public boolean deserializedChanged(Object old, Object newValue) {
            if (!isValidComparison(old, newValue)) {
                return true;
            }

            EffectMappingEntry oldEntry = (EffectMappingEntry) old;
            EffectMappingEntry newEntry = (EffectMappingEntry) newValue;

            return hasFieldChanges(oldEntry, newEntry);
        }

        private boolean isValidComparison(Object old, Object newValue) {
            if (old == null || newValue == null) {
                return false;
            }
            return old instanceof EffectMappingEntry && newValue instanceof EffectMappingEntry;
        }

        private boolean hasFieldChanges(EffectMappingEntry oldEntry, EffectMappingEntry newEntry) {
            return !java.util.Objects.equals(oldEntry.sourceEffect.get(), newEntry.sourceEffect.get()) ||
                   !java.util.Objects.equals(oldEntry.targetEffect.get(), newEntry.targetEffect.get()) ||
                   !java.util.Objects.equals(oldEntry.amplifierMultiplier, newEntry.amplifierMultiplier) ||
                   !java.util.Objects.equals(oldEntry.onlyVisualChange, newEntry.onlyVisualChange) ||
                   !java.util.Objects.equals(oldEntry.unidirectional, newEntry.unidirectional) ||
                   !java.util.Objects.equals(oldEntry.extraApplyEffectTick.get(), newEntry.extraApplyEffectTick.get()) ||
                   !java.util.Objects.equals(oldEntry.discardEffectTick.get(), newEntry.discardEffectTick.get());
        }

    
    private boolean matchesPattern(String effectId, String pattern) {
        if (pattern == null || effectId == null) {
            return false;
        }

        if (pattern.equals(effectId)) {
            return true;
        }

        if (!pattern.contains("*")) {
            return false;
        }

        String[] effectParts = splitEffectId(effectId);
        String[] patternParts = splitEffectId(pattern);
        
        if (effectParts.length == 0 || patternParts.length == 0) {
            return false;
        }
        
        if (!isValidWildcardPattern(patternParts)) {
            return false;
        }

        return matchesNamespace(effectParts[0], patternParts[0]) && 
               matchesPath(effectParts[1], patternParts[1]);
    }
    
    private String[] splitEffectId(String id) {
        int colonIndex = id.indexOf(':');
        if (colonIndex == -1 || colonIndex == id.length() - 1) {
            return new String[0];
        }
        return new String[] {
            id.substring(0, colonIndex),
            id.substring(colonIndex + 1)
        };
    }
    
    private boolean isValidWildcardPattern(String[] patternParts) {
        String namespace = patternParts[0];
        String path = patternParts[1];
        
        if (namespace.equals("*") && path.equals("*")) {
            return false;
        }
        if (!namespace.equals("*") && path.equals("*")) {
            return false;
        }
        return namespace.equals("*") || path.contains("*");
    }
    
    private boolean matchesNamespace(String effectNamespace, String patternNamespace) {
        if (patternNamespace.equals("*")) {
            return true;
        }
        if (patternNamespace.contains("*")) {
            return simpleWildcardMatch(effectNamespace, patternNamespace);
        }
        return patternNamespace.equals(effectNamespace);
    }
    
    private boolean matchesPath(String effectPath, String patternPath) {
        if (patternPath.equals("*")) {
            return true;
        }
        if (patternPath.contains("*")) {
            return simpleWildcardMatch(effectPath, patternPath);
        }
        return patternPath.equals(effectPath);
    }
    
    private boolean simpleWildcardMatch(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }

        String[] parts = pattern.split("\\*", -1);
        int textIndex = 0;
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            if (part.isEmpty()) {
                continue;
            }
            
            textIndex = matchPatternPart(text, part, textIndex, i, parts.length);
            if (textIndex == -1) {
                return false;
            }
        }
        
        return true;
    }
    
    private int matchPatternPart(String text, String part, int textIndex, int partIndex, int totalParts) {
        if (partIndex == 0) {
            return text.startsWith(part) ? part.length() : -1;
        }
        
        if (partIndex == totalParts - 1) {
            if (!text.endsWith(part)) {
                return -1;
            }
            int expectedIndex = text.length() - part.length();
            return expectedIndex >= textIndex ? expectedIndex : -1;
        }
        
        int foundIndex = text.indexOf(part, textIndex);
        return foundIndex != -1 ? foundIndex + part.length() : -1;
    }
}
