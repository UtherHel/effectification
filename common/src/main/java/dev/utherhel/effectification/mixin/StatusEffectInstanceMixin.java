package dev.utherhel.effectification.mixin;

import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectUnifier;
import dev.utherhel.effectification.config.EffectMappingEntry;
import dev.utherhel.effectification.util.CacheCleanable;
import dev.utherhel.effectification.util.EffectUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class StatusEffectInstanceMixin implements CacheCleanable {
    
    @Unique
    private static final ThreadLocal<Boolean> effectification$isProcessing = ThreadLocal.withInitial(() -> false);
    
    @Unique
    private MobEffect effectification$cachedTargetEffect = null;
    
    @Unique
    private boolean effectification$conversionChecked = false;
    
    @Unique
    private Integer effectification$cachedAmplifier = null;
    
    @Unique
    private String effectification$cachedEffectId = null;
    
    @Unique
    private long effectification$cachedUnifierVersion = -1;
    
    @Shadow
    public abstract net.minecraft.core.Holder<MobEffect> getEffect();
    
    @Shadow
    public abstract int getAmplifier();
    
    @Inject(method = "getEffect", at = @At("RETURN"), cancellable = true)
    private void onGetEffectType(CallbackInfoReturnable<net.minecraft.core.Holder<MobEffect>> cir) {
        if (Boolean.TRUE.equals(effectification$isProcessing.get())) {
            return;
        }
        
        effectification$isProcessing.set(true);
        try {
            // Check if cache is still valid
            long currentVersion = Effectification.getEffectUnifierVersion();
            if (effectification$cachedUnifierVersion != currentVersion) {
                effectification$clearCache();
                effectification$cachedUnifierVersion = currentVersion;
            }
            
            if (!effectification$conversionChecked) {
                effectification$checkAndCacheConversion(cir);
                return;
            }
            
            if (effectification$cachedTargetEffect != null) {
                cir.setReturnValue(effectification$getRegistryEntry(effectification$cachedTargetEffect));
            }
        } finally {
            effectification$isProcessing.set(false);
        }
    }
    
    @Unique
    private net.minecraft.core.Holder<MobEffect> effectification$getRegistryEntry(MobEffect effect) {
        net.minecraft.core.Registry<MobEffect> registry = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT;
        return registry.wrapAsHolder(effect);
    }
    
    @Unique
    private void effectification$checkAndCacheConversion(CallbackInfoReturnable<net.minecraft.core.Holder<MobEffect>> cir) {
        EffectUnifier unifier = Effectification.getEffectUnifier();
        if (unifier == null) {
            effectification$conversionChecked = true;
            return;
        }
        
        MobEffect effect = cir.getReturnValue().value();
        effectification$cachedEffectId = EffectUtils.getEffectId(effect);
        if (effectification$cachedEffectId == null) {
            effectification$conversionChecked = true;
            return;
        }
        
        if (unifier.getStaticMappings().containsKey(effectification$cachedEffectId)) {
            effectification$conversionChecked = true;
            return;
        }
        
        EffectMappingEntry mapping = unifier.getMapping(effectification$cachedEffectId);
        if (mapping != null && !mapping.isOnlyVisualChange()) {
            MobEffect targetEffect = mapping.getResolvedTargetEffect();
            if (targetEffect != null && targetEffect != effect) {
                effectification$cachedTargetEffect = targetEffect;
                effectification$conversionChecked = true;
                cir.setReturnValue(effectification$getRegistryEntry(targetEffect));
                return;
            }
        }
        
        effectification$conversionChecked = true;
    }
    
    @Inject(method = "getAmplifier", at = @At("RETURN"), cancellable = true)
    private void onGetAmplifier(CallbackInfoReturnable<Integer> cir) {
        if (Boolean.TRUE.equals(effectification$isProcessing.get())) {
            return;
        }
        
        try {
            effectification$isProcessing.set(true);
            
            if (effectification$cachedAmplifier != null) {
                cir.setReturnValue(effectification$cachedAmplifier);
                return;
            }
            
            if (effectification$cachedEffectId == null) {
                return;
            }
            
            EffectUnifier unifier = Effectification.getEffectUnifier();
            if (unifier == null) {
                return;
            }
            
            if (unifier.getStaticMappings().containsKey(effectification$cachedEffectId)) {
                return;
            }
            
            EffectMappingEntry mapping = unifier.getMapping(effectification$cachedEffectId);
            if (mapping != null && !mapping.isOnlyVisualChange()) {
                int convertedAmplifier = unifier.convertAmplifier(effectification$cachedEffectId, cir.getReturnValue());
                effectification$cachedAmplifier = convertedAmplifier;
                cir.setReturnValue(convertedAmplifier);
            }
        } finally {
            effectification$isProcessing.set(false);
        }
    }
    
    @Inject(method = "getDescriptionId", at = @At("RETURN"), cancellable = true)
    private void onGetTranslationKey(CallbackInfoReturnable<String> cir) {
        if (Boolean.TRUE.equals(effectification$isProcessing.get())) {
            return;
        }
        
        try {
            effectification$isProcessing.set(true);
            
            if (effectification$cachedEffectId == null) {
                return;
            }
            
            EffectUnifier unifier = Effectification.getEffectUnifier();
            if (unifier == null) {
                return;
            }
            
            if (unifier.getStaticMappings().containsKey(effectification$cachedEffectId)) {
                return;
            }
            
            EffectMappingEntry mapping = unifier.getMapping(effectification$cachedEffectId);
            if (mapping != null) {
                MobEffect targetEffect = mapping.getResolvedTargetEffect();
                if (targetEffect != null && targetEffect != this.getEffect().value()) {
                    cir.setReturnValue(targetEffect.getDescriptionId());
                }
            }
        } finally {
            effectification$isProcessing.set(false);
        }
    }
    
    @Override
    public void effectification$clearCache() {
        effectification$cachedTargetEffect = null;
        effectification$conversionChecked = false;
        effectification$cachedAmplifier = null;
        effectification$cachedEffectId = null;
    }
}
