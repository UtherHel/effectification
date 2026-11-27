package dev.utherhel.effectification.mixin;

import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectUnifier;
import dev.utherhel.effectification.EffectificationLogger;
import dev.utherhel.effectification.config.EffectMappingEntry;
import dev.utherhel.effectification.util.EffectUtils;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// Handles unidirectional mode and extra effect ticks
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private static final ThreadLocal<Boolean> isProcessingConversion = ThreadLocal.withInitial(() -> false);
    
    @Unique
    private static final ThreadLocal<Boolean> isProcessingTick = ThreadLocal.withInitial(() -> false);
    
    // Caches source -> target during addEffect for hasEffect() lookups
    @Unique
    private static final ThreadLocal<Map<MobEffect, MobEffect>> conversionCache = 
        ThreadLocal.withInitial(HashMap::new);
    
    @Shadow
    //? if >=1.20.5 {
    public abstract Map<net.minecraft.core.Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap();
    //?} else {
    /*public abstract Map<MobEffect, MobEffectInstance> getActiveEffectsMap();
    *///?}
    
    // Caches conversion for hasEffect() during addEffect
    // Actual conversion happens in StatusEffectInstanceMixin
    @ModifyVariable(
        method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"),
        argsOnly = true
    )
    private MobEffectInstance modifyEffectInstance(MobEffectInstance instance) {
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
            
            //? if >=1.20.5 {
            MobEffect effect = instance.getEffect().value();
            //?} else {
            /*MobEffect effect = instance.getEffect();
            *///?}
            String effectId = EffectUtils.getEffectId(effect);
            if (effectId == null) {
                return instance;
            }
            
            EffectMappingEntry mapping = unifier.getMapping(effectId);
            if (mapping != null && !mapping.isOnlyVisualChange()) {
                MobEffect targetEffect = mapping.getResolvedTargetEffect();
                
                if (targetEffect == null) {
                    EffectificationLogger.info("[AddEffect] " + effectId + " is not converted: none of its targets ("
                        + mapping.getTargetEffect() + ") is installed");
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
    
    // Clear conversion cache after addEffect
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private void onAddStatusEffectReturn(MobEffectInstance instance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        Map<MobEffect, MobEffect> cache = conversionCache.get();
        if (!cache.isEmpty()) {
            cache.clear();
        }
    }
    
    @ModifyVariable(
        method = "hasEffect",
        at = @At("HEAD"),
        argsOnly = true
    )
    //? if >=1.20.5 {
    private net.minecraft.core.Holder<MobEffect> modifyHasEffectParameter(net.minecraft.core.Holder<MobEffect> effect) {
        if (effect == null) return effect;
        MobEffect converted = getUnifiedEffectForLookup(effect.value());
        if (converted != effect.value()) {
            return getRegistryEntry(converted);
        }
        return effect;
    }
    //?} else {
    /*private MobEffect modifyHasEffectParameter(MobEffect effect) {
        return getUnifiedEffectForLookup(effect);
    }
    *///?}
    
    @ModifyVariable(
        method = "getEffect",
        at = @At("HEAD"),
        argsOnly = true
    )
    //? if >=1.20.5 {
    private net.minecraft.core.Holder<MobEffect> modifyGetEffectParameter(net.minecraft.core.Holder<MobEffect> effect) {
        if (effect == null) return effect;
        MobEffect converted = getUnifiedEffectForLookup(effect.value());
        if (converted != effect.value()) {
            return getRegistryEntry(converted);
        }
        return effect;
    }
    //?} else {
    /*private MobEffect modifyGetEffectParameter(MobEffect effect) {
        return getUnifiedEffectForLookup(effect);
    }
    *///?}
    
    @ModifyVariable(
        method = "removeEffect",
        at = @At("HEAD"),
        argsOnly = true
    )
    //? if >=1.20.5 {
    private net.minecraft.core.Holder<MobEffect> modifyRemoveEffectParameter(net.minecraft.core.Holder<MobEffect> effect) {
        if (effect == null) return effect;
        MobEffect converted = getUnifiedEffectSafe(effect.value());
        if (converted != effect.value()) {
            return getRegistryEntry(converted);
        }
        return effect;
    }
    //?} else {
    /*private MobEffect modifyRemoveEffectParameter(MobEffect effect) {
        return getUnifiedEffectSafe(effect);
    }
    *///?}
    
    //? if >=1.20.5 {
    @Unique
    private net.minecraft.core.Holder<MobEffect> getRegistryEntry(MobEffect effect) {
        net.minecraft.core.Registry<MobEffect> registry = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT;
        return registry.wrapAsHolder(effect);
    }
    //?}
    
    // Gets unified effect with recursion protection
    @Unique
    private MobEffect getUnifiedEffectSafe(MobEffect effect) {
        Boolean processing = isProcessingConversion.get();
        if (Boolean.TRUE.equals(processing)) return effect;
        
        try {
            isProcessingConversion.set(true);
            
            if (effect == null) return effect;
            
            MobEffect cachedTarget = conversionCache.get().get(effect);
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
                
                MobEffect targetEffect = mapping.getResolvedTargetEffect();
                if (targetEffect != null && targetEffect != effect) {
                    return targetEffect;
                }
            }
            
            return effect;
        } finally {
            isProcessingConversion.set(false);
        }
    }
    
    // Respects unidirectional mode for hasEffect/getEffect
    @Unique
    private MobEffect getUnifiedEffectForLookup(MobEffect effect) {
        Boolean processing = isProcessingConversion.get();
        if (Boolean.TRUE.equals(processing)) return effect;
        
        isProcessingConversion.set(true);
        try {
            if (effect == null) return effect;
            
            MobEffect cachedTarget = conversionCache.get().get(effect);
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
            
            // a converted effect is stored under its target: a lookup of the source finds it there, except for
            // unidirectional mappings (the source stays hidden) and visual-only ones (nothing was converted)
            EffectMappingEntry mapping = unifier.getMapping(effectId);
            if (mapping == null || mapping.isUnidirectional() || mapping.isOnlyVisualChange()) {
                return effect;
            }
            MobEffect target = mapping.getResolvedTargetEffect();
            return target != null ? target : effect;
        } finally {
            isProcessingConversion.set(false);
        }
    }
    
    // Calls extraApplyEffectTick after updating effects
    @Inject(method = "tickEffects", at = @At("RETURN"))
    private void onTickStatusEffects(CallbackInfo ci) {
        // effect ticks run on the server only, as in vanilla; the client would apply damage or healing a second time
        if (shouldSkipTickProcessing() || ((LivingEntity) (Object) this).level().isClientSide()) {
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
        //? if >=1.20.5 {
        Map<net.minecraft.core.Holder<MobEffect>, MobEffectInstance> activeEffects = entity.getActiveEffectsMap();
        //?} else {
        /*Map<MobEffect, MobEffectInstance> activeEffects = entity.getActiveEffectsMap();
        *///?}
        
        if (activeEffects.isEmpty()) {
            return;
        }
        
        // a copy: an extra tick may add or remove effects of this entity
        //? if >=1.20.5 {
        for (Map.Entry<net.minecraft.core.Holder<MobEffect>, MobEffectInstance> entry : List.copyOf(activeEffects.entrySet())) {
        //?} else {
        /*for (Map.Entry<MobEffect, MobEffectInstance> entry : List.copyOf(activeEffects.entrySet())) {
        *///?}
            processEffectEntry(entity, entry, unifier);
        }
    }
    
    @Unique
    //? if >=1.20.5 {
    private void processEffectEntry(LivingEntity entity, Map.Entry<net.minecraft.core.Holder<MobEffect>, MobEffectInstance> entry, EffectUnifier unifier) {
        MobEffect effect = entry.getKey().value();
    //?} else {
    /*private void processEffectEntry(LivingEntity entity, Map.Entry<MobEffect, MobEffectInstance> entry, EffectUnifier unifier) {
        MobEffect effect = entry.getKey();
    *///?}
        MobEffectInstance instance = entry.getValue();
        
        String effectId = EffectUtils.getEffectId(effect);
        if (effectId == null) {
            return;
        }
        
        // the stored effect is the target of a converted effect, so the mappings are found by target as well
        for (EffectMappingEntry mapping : unifier.getExtraTickMappings(effect, effectId)) {
            applyExtraEffectTicks(entity, instance, mapping, unifier);
        }
    }
    
    @Unique
    private void applyExtraEffectTicks(LivingEntity entity, MobEffectInstance instance, EffectMappingEntry mapping, EffectUnifier unifier) {
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
    private void applyExtraEffectTickExact(LivingEntity entity, MobEffectInstance instance, String effectId) {
        MobEffect extraEffect = EffectUtils.getEffectByName(effectId);
        //? if >=1.20.2 {
        if (extraEffect != null && extraEffect.shouldApplyEffectTickThisTick(instance.getDuration(), instance.getAmplifier())) {
        //?} else {
        /*if (extraEffect != null && extraEffect.isDurationEffectTick(instance.getDuration(), instance.getAmplifier())) {
        *///?}
            //? if >=1.21.2 {
            /*if (entity.level() instanceof ServerLevel serverLevel) extraEffect.applyEffectTick(serverLevel, entity, instance.getAmplifier());
            *///?} else {
            extraEffect.applyEffectTick(entity, instance.getAmplifier());
            //?}
        }
    }
    
    // Applies extra effect tick for wildcard patterns
    @Unique
    private void applyExtraEffectTickWildcard(LivingEntity entity, MobEffectInstance instance, String pattern, EffectUnifier unifier) {
        if (pattern.equals("*:*")) {
            EffectificationLogger.warnOnce("extra *:*", "extraApplyEffectTick '*:*' is ignored: it would tick every effect");
            return;
        }
        
        int colonIndex = pattern.indexOf(':');
        if (colonIndex != -1 && colonIndex < pattern.length() - 1) {
            char firstChar = pattern.charAt(0);
            if (firstChar != '*' && pattern.charAt(colonIndex + 1) == '*' && 
                (colonIndex + 2 >= pattern.length() || pattern.charAt(colonIndex + 2) != '*')) {
                EffectificationLogger.warnOnce("extra " + pattern, "extraApplyEffectTick '" + pattern
                    + "' is ignored: it would tick every effect of that mod; name the effects instead");
                return;
            }
        }
        
        List<MobEffect> matchingEffects = unifier.getMatchingEffects(pattern);
        for (MobEffect effect : matchingEffects) {
            //? if >=1.20.2 {
            if (effect.shouldApplyEffectTickThisTick(instance.getDuration(), instance.getAmplifier())) {
            //?} else {
            /*if (effect.isDurationEffectTick(instance.getDuration(), instance.getAmplifier())) {
            *///?}
                //? if >=1.21.2 {
                /*if (entity.level() instanceof ServerLevel serverLevel) effect.applyEffectTick(serverLevel, entity, instance.getAmplifier());
                *///?} else {
                effect.applyEffectTick(entity, instance.getAmplifier());
                //?}
            }
        }
    }
}
