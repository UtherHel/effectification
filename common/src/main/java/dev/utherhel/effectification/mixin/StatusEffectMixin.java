package dev.utherhel.effectification.mixin;

import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectUnifier;
import dev.utherhel.effectification.util.EffectUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffect.class)
public class StatusEffectMixin {
    @Unique
    private static final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);
    
    @Inject(method = "applyUpdateEffect", at = @At("HEAD"), cancellable = true)
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
            
            StatusEffect thisEffect = (StatusEffect)(Object)this;
            String effectId = EffectUtils.getEffectId(thisEffect);
            
            if (effectId != null && unifier.shouldDiscardEffectTick(effectId)) {
                cir.setReturnValue(true);
            }
        } finally {
            isProcessing.set(false);
        }
    }
}
