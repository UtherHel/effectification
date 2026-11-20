package dev.utherhel.effectification.mixin;

import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectUnifier;
import dev.utherhel.effectification.util.EffectUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffect.class)
public class StatusEffectMixin {
    @Unique
    private static final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);
    
    @Inject(method = "applyEffectTick", at = @At("HEAD"), cancellable = true)
    private void onApplyUpdateEffect(LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(isProcessing.get())) {
            return;
        }
        
        isProcessing.set(true);
        try {
            EffectUnifier unifier = Effectification.getEffectUnifier();
            if (unifier == null) {
                return;
            }
            
            MobEffect thisEffect = (MobEffect)(Object)this;
            String effectId = EffectUtils.getEffectId(thisEffect);
            
            if (effectId != null && unifier.shouldDiscardEffectTick(effectId)) {
                cir.setReturnValue(true);
            }
        } finally {
            isProcessing.set(false);
        }
    }
}
