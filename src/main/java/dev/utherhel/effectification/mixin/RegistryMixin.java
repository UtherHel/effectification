package dev.utherhel.effectification.mixin;

import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectUnifier;
import dev.utherhel.effectification.EffectificationLogger;
import dev.utherhel.effectification.config.EffectMappingEntry;
import dev.utherhel.effectification.util.EffectUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

@Mixin(MappedRegistry.class)
public abstract class RegistryMixin<T> {
    
    @Shadow @Final
    private Map<ResourceLocation, Holder.Reference<T>> byLocation;
    
    @Shadow @Final
    private Map<ResourceKey<T>, Holder.Reference<T>> byKey;
    
    @Shadow @Final
    private ResourceKey<? extends Registry<T>> key;
    
    @Inject(method = "freeze", at = @At("HEAD"))
    private void injectEffectAliases(CallbackInfoReturnable<Registry<T>> cir) {
        if (!isStatusEffectRegistry()) {
            return;
        }
        
        EffectUnifier unifier = Effectification.getEffectUnifier();
        if (unifier == null) {
            EffectificationLogger.warn("[Registry] EffectUnifier not initialized, skipping alias injection");
            return;
        }
        
        int[] counts = processAllMappings(unifier);
        logResults(counts[0], counts[1]);
    }
    
    private boolean isStatusEffectRegistry() {
        return key.location().toString().equals("minecraft:mob_effect");
    }
    
    private int[] processAllMappings(EffectUnifier unifier) {
        int injected = 0;
        int failed = 0;
        
        Map<String, EffectMappingEntry> staticMappings = unifier.getStaticMappings();
        List<EffectMappingEntry> staticWildcards = unifier.getStaticWildcardMappings();
        
        for (EffectMappingEntry mapping : staticMappings.values()) {
            if (mapping.isOnlyVisualChange()) {
                continue;
            }
            
            int[] result = processSingleMapping(mapping, unifier);
            injected += result[0];
            failed += result[1];
        }
        
        for (EffectMappingEntry mapping : staticWildcards) {
            if (mapping.isOnlyVisualChange()) {
                continue;
            }
            
            int[] result = processSingleMapping(mapping, unifier);
            injected += result[0];
            failed += result[1];
        }
        
        return new int[]{injected, failed};
    }
    
    private int[] processSingleMapping(EffectMappingEntry mapping, EffectUnifier unifier) {
        MobEffect targetEffect = mapping.getResolvedTargetEffect();
        if (targetEffect == null) {
            EffectificationLogger.warn("[Registry] Target effect not found for mapping: " + 
                mapping.getSourceEffect() + " -> " + mapping.getTargetEffect());
            return new int[]{0, 1};
        }
        
        String targetId = EffectUtils.getEffectId(targetEffect);
        if (targetId == null) {
            EffectificationLogger.warn("[Registry] Cannot get ID for target effect: " + targetEffect);
            return new int[]{0, 1};
        }
        
        ResourceLocation targetIdentifier = ResourceLocation.parse(targetId);
        Holder.Reference<T> targetHolder = byLocation.get(targetIdentifier);
        
        if (targetHolder == null) {
            EffectificationLogger.warn("[Registry] Target holder not found for: " + targetId);
            return new int[]{0, 1};
        }
        
        return processSourcePattern(mapping.getSourceEffect(), targetHolder, targetId, unifier);
    }
    
    private int[] processSourcePattern(String sourcePattern, Holder.Reference<T> targetHolder, 
                                       String targetId, EffectUnifier unifier) {
        if (!sourcePattern.contains("*")) {
            boolean success = injectSingleAlias(sourcePattern, targetHolder);
            return new int[]{success ? 1 : 0, success ? 0 : 1};
        }
        
        int injected = injectWildcardAliases(sourcePattern, targetHolder, targetId, unifier);
        return new int[]{injected, 0};
    }
    
    private void logResults(int injected, int failed) {
        if (injected > 0) {
            EffectificationLogger.info("[Registry] Injected " + injected + " effect aliases before freeze");
        }
        if (failed > 0) {
            EffectificationLogger.warn("[Registry] Failed to inject " + failed + " effect aliases");
        }
    }
    
    private int injectWildcardAliases(String sourcePattern, Holder.Reference<T> targetHolder, 
                                      String targetId, EffectUnifier unifier) {
        int count = 0;
        for (MobEffect effect : unifier.getMatchingEffects(sourcePattern)) {
            String effectId = EffectUtils.getEffectId(effect);
            if (effectId != null && !effectId.equals(targetId) && injectSingleAlias(effectId, targetHolder)) {
                count++;
            }
        }
        return count;
    }
    
    private boolean injectSingleAlias(String sourceId, Holder.Reference<T> targetHolder) {
        try {
            ResourceLocation sourceIdentifier = ResourceLocation.parse(sourceId);
            
            Holder.Reference<T> existingHolder = byLocation.get(sourceIdentifier);
            if (existingHolder != null) {
                return existingHolder == targetHolder;
            }
            
            byLocation.put(sourceIdentifier, targetHolder);
            
            ResourceKey<T> sourceKey = (ResourceKey<T>) ResourceKey.create(key, sourceIdentifier);
            byKey.put(sourceKey, targetHolder);
            
            return true;
        } catch (Exception e) {
            EffectificationLogger.warn("[Registry] Failed to inject alias for: " + sourceId + " - " + e.getMessage());
            return false;
        }
    }
}
