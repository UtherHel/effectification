package dev.utherhel.effectification.mixin;

import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectUnifier;
import dev.utherhel.effectification.EffectificationLogger;
import dev.utherhel.effectification.config.EffectMappingEntry;
import dev.utherhel.effectification.util.EffectUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Handles unidirectional mode and extra effect ticks
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private static final ThreadLocal<Boolean> isProcessingConversion = ThreadLocal.withInitial(() -> false);
    
    @Unique
    private static final ThreadLocal<Boolean> isProcessingTick = ThreadLocal.withInitial(() -> false);
    
    // Caches source -> target during addStatusEffect for hasEffect() lookups
    @Unique
    private static final ThreadLocal<Map<StatusEffect, StatusEffect>> conversionCache = 
        ThreadLocal.withInitial(HashMap::new);
    
    @Shadow
    public abstract Map<net.minecraft.registry.entry.RegistryEntry<StatusEffect>, StatusEffectInstance> getActiveStatusEffects();
    
    // Caches conversion for hasEffect() during addStatusEffect
    // Actual conversion happens in StatusEffectInstanceMixin
    @ModifyVariable(
        method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
        at = @At("HEAD"),
        argsOnly = true
    )
    private StatusEffectInstance modifyEffectInstance(StatusEffectInstance instance) {
        Boolean processing = isProcessingConversion.get();
        if (Boolean.TRUE.equals(processing)) {
            return instance;
        }
        
        isProcessingConversion.set(true);
        try {
            if (instance == null) {
                return instance;
            }
            
            EffectUnifier unifier = Effectification.getEffectUnifier();
            if (unifier == null) {
                return instance;
            }
            
            StatusEffect effect = instance.getEffectType().value();
            String effectId = EffectUtils.getEffectId(effect);
            if (effectId == null) {
                return instance;
            }
            
            EffectMappingEntry mapping = unifier.getMapping(effectId);
            if (mapping != null && !mapping.isOnlyVisualChange()) {
                StatusEffect targetEffect = mapping.getResolvedTargetEffect();
                
                if (targetEffect == null) {
                    EffectificationLogger.warn("[AddEffect] Target effect not found for: " + effectId + 
                        ", targets: " + mapping.getTargetEffect() + " - effect will NOT be converted!");
                    return instance;
                }
                
                if (targetEffect != effect) {
                    conversionCache.get().put(effect, targetEffect);
                }
            }
            
            return instance;
        } finally {
            isProcessingConversion.set(false);
        }
    }
    
    // Clear conversion cache after addStatusEffect
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("RETURN"))
    private void onAddStatusEffectReturn(StatusEffectInstance instance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        Map<StatusEffect, StatusEffect> cache = conversionCache.get();
        if (!cache.isEmpty()) {
            cache.clear();
        }
    }
    
    @ModifyVariable(
        method = "hasStatusEffect",
        at = @At("HEAD"),
        argsOnly = true
    )
    private net.minecraft.registry.entry.RegistryEntry<StatusEffect> modifyHasEffectParameter(net.minecraft.registry.entry.RegistryEntry<StatusEffect> effect) {
        if (effect == null) return effect;
        StatusEffect converted = getUnifiedEffectForLookup(effect.value());
        if (converted != effect.value()) {
            return getRegistryEntry(converted);
        }
        return effect;
    }
    
    @ModifyVariable(
        method = "getStatusEffect",
        at = @At("HEAD"),
        argsOnly = true
    )
    private net.minecraft.registry.entry.RegistryEntry<StatusEffect> modifyGetEffectParameter(net.minecraft.registry.entry.RegistryEntry<StatusEffect> effect) {
        if (effect == null) return effect;
        StatusEffect converted = getUnifiedEffectForLookup(effect.value());
        if (converted != effect.value()) {
            return getRegistryEntry(converted);
        }
        return effect;
    }
    
    @ModifyVariable(
        method = "removeStatusEffect",
        at = @At("HEAD"),
        argsOnly = true
    )
    private net.minecraft.registry.entry.RegistryEntry<StatusEffect> modifyRemoveEffectParameter(net.minecraft.registry.entry.RegistryEntry<StatusEffect> effect) {
        if (effect == null) return effect;
        StatusEffect converted = getUnifiedEffectSafe(effect.value());
        if (converted != effect.value()) {
            return getRegistryEntry(converted);
        }
        return effect;
    }
    
    @Unique
    private net.minecraft.registry.entry.RegistryEntry<StatusEffect> getRegistryEntry(StatusEffect effect) {
        net.minecraft.registry.Registry<StatusEffect> registry = net.minecraft.registry.Registries.STATUS_EFFECT;
        return registry.getEntry(effect);
    }
    
    // Gets unified effect with recursion protection
    @Unique
    private StatusEffect getUnifiedEffectSafe(StatusEffect effect) {
        Boolean processing = isProcessingConversion.get();
        if (Boolean.TRUE.equals(processing)) return effect;
        
        try {
            isProcessingConversion.set(true);
            
            if (effect == null) return effect;
            
            StatusEffect cachedTarget = conversionCache.get().get(effect);
            if (cachedTarget != null) {
                return cachedTarget;
            }
            EffectUnifier unifier = Effectification.getEffectUnifier();
            if (unifier == null) return effect;
            
            String effectId = EffectUtils.getEffectId(effect);
            if (effectId == null) return effect;
            
            EffectMappingEntry mapping = unifier.getMapping(effectId);
            if (mapping != null) {
                if (mapping.isOnlyVisualChange()) {
                    return effect;
                }
                
                StatusEffect targetEffect = mapping.getResolvedTargetEffect();
                if (targetEffect != null && targetEffect != effect) {
                    return targetEffect;
                }
            }
            
            return effect;
        } finally {
            isProcessingConversion.set(false);
        }
    }
    
    // Respects unidirectional mode for hasEffect/getStatusEffect
    @Unique
    private StatusEffect getUnifiedEffectForLookup(StatusEffect effect) {
        Boolean processing = isProcessingConversion.get();
        if (Boolean.TRUE.equals(processing)) return effect;
        
        isProcessingConversion.set(true);
        try {
            if (effect == null) return effect;
            
            StatusEffect cachedTarget = conversionCache.get().get(effect);
            if (cachedTarget != null) {
                return cachedTarget;
            }
            EffectUnifier unifier = Effectification.getEffectUnifier();
            if (unifier == null) {
                return effect;
            }
            
            String effectId = EffectUtils.getEffectId(effect);
            if (effectId == null) {
                return effect;
            }
            
            EffectMappingEntry mapping = unifier.getMapping(effectId);
            if (mapping != null && (mapping.isUnidirectional() || mapping.isOnlyVisualChange())) {
                return effect;
            }
            
            return effect;
        } finally {
            isProcessingConversion.set(false);
        }
    }
    
    // Calls extraApplyEffectTick after updating effects
    @Inject(method = "tickStatusEffects", at = @At("RETURN"))
    private void onTickStatusEffects(CallbackInfo ci) {
        if (shouldSkipTickProcessing()) {
            return;
        }
        
        try {
            isProcessingTick.set(true);
            processExtraEffectTicks();
        } finally {
            isProcessingTick.set(false);
        }
    }
    
    @Unique
    private boolean shouldSkipTickProcessing() {
        return Boolean.TRUE.equals(isProcessingTick.get());
    }
    
    @Unique
    private void processExtraEffectTicks() {
        EffectUnifier unifier = Effectification.getEffectUnifier();
        if (unifier == null) {
            return;
        }
        
        LivingEntity entity = (LivingEntity) (Object) this;
        Map<net.minecraft.registry.entry.RegistryEntry<StatusEffect>, StatusEffectInstance> activeEffects = entity.getActiveStatusEffects();
        
        if (activeEffects.isEmpty()) {
            return;
        }
        
        for (Map.Entry<net.minecraft.registry.entry.RegistryEntry<StatusEffect>, StatusEffectInstance> entry : activeEffects.entrySet()) {
            processEffectEntry(entity, entry, unifier);
        }
    }
    
    @Unique
    private void processEffectEntry(LivingEntity entity, Map.Entry<net.minecraft.registry.entry.RegistryEntry<StatusEffect>, StatusEffectInstance> entry, EffectUnifier unifier) {
        StatusEffect effect = entry.getKey().value();
        StatusEffectInstance instance = entry.getValue();
        
        String effectId = EffectUtils.getEffectId(effect);
        if (effectId == null) {
            return;
        }
        
        EffectMappingEntry mapping = unifier.getMapping(effectId);
        if (mapping == null) {
            return;
        }
        
        String extraTick = mapping.getExtraApplyEffectTick();
        if (extraTick == null || extraTick.isEmpty()) {
            return;
        }
        
        applyExtraEffectTicks(entity, instance, mapping, unifier);
    }
    
    @Unique
    private void applyExtraEffectTicks(LivingEntity entity, StatusEffectInstance instance, EffectMappingEntry mapping, EffectUnifier unifier) {
        List<String> extraPatterns = mapping.getExtraApplyEffectTickList();
        for (String extraEffectPattern : extraPatterns) {
            if (extraEffectPattern.contains("*")) {
                applyExtraEffectTickWildcard(entity, instance, extraEffectPattern, unifier);
            } else {
                applyExtraEffectTickExact(entity, instance, extraEffectPattern);
            }
        }
    }
    
    @Unique
    private void applyExtraEffectTickExact(LivingEntity entity, StatusEffectInstance instance, String effectId) {
        StatusEffect extraEffect = EffectUtils.getEffectByName(effectId);
        if (extraEffect != null && extraEffect.canApplyUpdateEffect(instance.getDuration(), instance.getAmplifier())) {
            extraEffect.applyUpdateEffect(entity, instance.getAmplifier());
        }
    }
    
    // Applies extra effect tick for wildcard patterns
    @Unique
    private void applyExtraEffectTickWildcard(LivingEntity entity, StatusEffectInstance instance, String pattern, EffectUnifier unifier) {
        if (pattern.equals("*:*")) {
            EffectificationLogger.warn("Dangerous extraApplyEffectTick pattern rejected: *:* (would call all effects)");
            return;
        }
        
        int colonIndex = pattern.indexOf(':');
        if (colonIndex != -1 && colonIndex < pattern.length() - 1) {
            char firstChar = pattern.charAt(0);
            if (firstChar != '*' && pattern.charAt(colonIndex + 1) == '*' && 
                (colonIndex + 2 >= pattern.length() || pattern.charAt(colonIndex + 2) != '*')) {
                EffectificationLogger.warn("Dangerous extraApplyEffectTick pattern rejected: " + pattern + " (would call all effects from mod)");
                return;
            }
        }
        
        List<StatusEffect> matchingEffects = unifier.getMatchingEffects(pattern);
        for (StatusEffect effect : matchingEffects) {
            if (effect.canApplyUpdateEffect(instance.getDuration(), instance.getAmplifier())) {
                effect.applyUpdateEffect(entity, instance.getAmplifier());
            }
        }
    }
}
