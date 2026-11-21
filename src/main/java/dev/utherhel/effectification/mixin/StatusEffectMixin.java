package dev.utherhel.effectification.mixin;

import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectUnifier;
import dev.utherhel.effectification.util.EffectUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=1.20.5 {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?} else {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

@Mixin(MobEffect.class)
public class StatusEffectMixin {
    @Unique
    private static final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);
    
    @Inject(method = "applyEffectTick", at = @At("HEAD"), cancellable = true)
    //? if >=1.21.2 {
    /*private void onApplyUpdateEffect(ServerLevel level, LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
    *///?} else if >=1.20.5 {
    private void onApplyUpdateEffect(LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
    //?} else {
    /*private void onApplyUpdateEffect(LivingEntity entity, int amplifier, CallbackInfo ci) {
    *///?}
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
                //? if >=1.20.5 {
                cir.setReturnValue(true);
                //?} else {
                /*ci.cancel();
                *///?}
            }
        } finally {
            isProcessing.set(false);
        }
    }
}
