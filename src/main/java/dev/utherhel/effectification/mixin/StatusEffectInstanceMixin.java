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
    
    /** The converted amplifier and the raw one it was converted from (the instance's amplifier changes on upgrade). */
    @Unique
    private Integer effectification$cachedAmplifier = null;

    @Unique
    private int effectification$cachedRawAmplifier = Integer.MIN_VALUE;
    
    @Unique
    private String effectification$cachedEffectId = null;
    
    @Unique
    private long effectification$cachedUnifierVersion = -1;
    
    @Shadow
    //? if >=1.20.5 {
    public abstract net.minecraft.core.Holder<MobEffect> getEffect();
    //?} else {
    /*public abstract MobEffect getEffect();
    *///?}
    
    @Shadow
    public abstract int getAmplifier();
    
    @Inject(method = "getEffect", at = @At("RETURN"), cancellable = true)
    //? if >=1.20.5 {
    private void onGetEffectType(CallbackInfoReturnable<net.minecraft.core.Holder<MobEffect>> cir) {
    //?} else {
    /*private void onGetEffectType(CallbackInfoReturnable<MobEffect> cir) {
    *///?}
        if (Boolean.TRUE.equals(effectification$isProcessing.get())) {
            return;
        }
        
        effectification$isProcessing.set(true);
        try {
            effectification$checkVersion();
            
            if (!effectification$conversionChecked) {
                effectification$checkAndCacheConversion(cir);
                return;
            }
            
            if (effectification$cachedTargetEffect != null) {
                //? if >=1.20.5 {
                cir.setReturnValue(effectification$getRegistryEntry(effectification$cachedTargetEffect));
                //?} else {
                /*cir.setReturnValue(effectification$cachedTargetEffect);
                *///?}
            }
        } finally {
            effectification$isProcessing.set(false);
        }
    }
    
    //? if >=1.20.5 {
    @Unique
    private net.minecraft.core.Holder<MobEffect> effectification$getRegistryEntry(MobEffect effect) {
        net.minecraft.core.Registry<MobEffect> registry = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT;
        return registry.wrapAsHolder(effect);
    }
    //?}
    
    @Unique
    //? if >=1.20.5 {
    private void effectification$checkAndCacheConversion(CallbackInfoReturnable<net.minecraft.core.Holder<MobEffect>> cir) {
    //?} else {
    /*private void effectification$checkAndCacheConversion(CallbackInfoReturnable<MobEffect> cir) {
    *///?}
        EffectUnifier unifier = Effectification.getEffectUnifier();
        if (unifier == null) {
            effectification$conversionChecked = true;
            return;
        }
        
        //? if >=1.20.5 {
        MobEffect effect = cir.getReturnValue().value();
        //?} else {
        /*MobEffect effect = cir.getReturnValue();
        *///?}
        effectification$cachedEffectId = EffectUtils.getEffectId(effect);
        if (effectification$cachedEffectId == null) {
            effectification$conversionChecked = true;
            return;
        }
        
        // static mappings too: an id they aliased in the registry already is the target here (no mapping matches
        // it), and one whose source is a registered effect converts at runtime like a dynamic mapping
        EffectMappingEntry mapping = unifier.getMapping(effectification$cachedEffectId);
        if (mapping != null && !mapping.isOnlyVisualChange()) {
            MobEffect targetEffect = mapping.getResolvedTargetEffect();
            if (targetEffect != null && targetEffect != effect) {
                effectification$cachedTargetEffect = targetEffect;
                effectification$conversionChecked = true;
                //? if >=1.20.5 {
                cir.setReturnValue(effectification$getRegistryEntry(targetEffect));
                //?} else {
                /*cir.setReturnValue(targetEffect);
                *///?}
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
            effectification$ensureChecked();

            int raw = cir.getReturnValue();
            if (effectification$cachedAmplifier != null && effectification$cachedRawAmplifier == raw) {
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
            
            // scaled when the mapping applies: not visual-only, and one of its targets is installed (it may be the
            // effect itself: a mapping onto itself only changes the level)
            EffectMappingEntry mapping = unifier.getMapping(effectification$cachedEffectId);
            if (mapping == null || mapping.isOnlyVisualChange() || mapping.getResolvedTargetEffect() == null) {
                return;
            }
            
            int convertedAmplifier = unifier.convertAmplifier(effectification$cachedEffectId, raw);
            effectification$cachedAmplifier = convertedAmplifier;
            effectification$cachedRawAmplifier = raw;
            cir.setReturnValue(convertedAmplifier);
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
            effectification$ensureChecked();
            
            if (effectification$cachedEffectId == null) {
                return;
            }
            
            EffectUnifier unifier = Effectification.getEffectUnifier();
            if (unifier == null) {
                return;
            }
            
            EffectMappingEntry mapping = unifier.getMapping(effectification$cachedEffectId);
            if (mapping != null) {
                MobEffect targetEffect = mapping.getResolvedTargetEffect();
                //? if >=1.20.5 {
                if (targetEffect != null && targetEffect != this.getEffect().value()) {
                //?} else {
                /*if (targetEffect != null && targetEffect != this.getEffect()) {
                *///?}
                    cir.setReturnValue(targetEffect.getDescriptionId());
                }
            }
        } finally {
            effectification$isProcessing.set(false);
        }
    }
    
    /** Drops the cached conversion when the mappings were reloaded since. */
    @Unique
    private void effectification$checkVersion() {
        long currentVersion = Effectification.getEffectUnifierVersion();
        if (effectification$cachedUnifierVersion != currentVersion) {
            effectification$clearCache();
            effectification$cachedUnifierVersion = currentVersion;
        }
    }

    /**
     * getAmplifier and getDescriptionId depend on the conversion that getEffect works out: run it first when
     * nothing asked for the effect yet (a tooltip or an amplifier check can come before any getEffect call).
     * Called with the recursion guard already set, so the getEffect call below returns the raw effect.
     */
    @Unique
    private void effectification$ensureChecked() {
        effectification$checkVersion();
        if (effectification$conversionChecked) {
            return;
        }
        EffectUnifier unifier = Effectification.getEffectUnifier();
        //? if >=1.20.5 {
        MobEffect effect = this.getEffect().value();
        //?} else {
        /*MobEffect effect = this.getEffect();
        *///?}
        effectification$conversionChecked = true;
        effectification$cachedEffectId = EffectUtils.getEffectId(effect);
        if (unifier == null || effectification$cachedEffectId == null) {
            return;
        }
        EffectMappingEntry mapping = unifier.getMapping(effectification$cachedEffectId);
        if (mapping != null && !mapping.isOnlyVisualChange()) {
            MobEffect targetEffect = mapping.getResolvedTargetEffect();
            if (targetEffect != null && targetEffect != effect) {
                effectification$cachedTargetEffect = targetEffect;
            }
        }
    }

    @Override
    public void effectification$clearCache() {
        effectification$cachedTargetEffect = null;
        effectification$conversionChecked = false;
        effectification$cachedAmplifier = null;
        effectification$cachedRawAmplifier = Integer.MIN_VALUE;
        effectification$cachedEffectId = null;
    }
}
