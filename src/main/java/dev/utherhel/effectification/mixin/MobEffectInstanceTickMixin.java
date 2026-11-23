package dev.utherhel.effectification.mixin;

import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectUnifier;
import dev.utherhel.effectification.util.EffectUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * discardEffectTick at the call site: since 1.20.2 every vanilla effect that ticks (regeneration, poison, wither, ...)
 * overrides MobEffect.applyEffectTick in its own class, so the HEAD injection of {@link StatusEffectMixin} into
 * MobEffect never runs for them. The instance's tick calls applyEffectTick on the effect; skipping that call works
 * for every effect class.
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceTickMixin {
    //? if >=1.21.5 {
    /*@Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;I)Z"))
    private boolean effectification$discardTick(MobEffect effect, net.minecraft.server.level.ServerLevel level, LivingEntity entity, int amplifier) {
        return effectification$discarded(effect) || effect.applyEffectTick(level, entity, amplifier);
    }
    *///?} else if >=1.21.2 {
    /*@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;I)Z"))
    private boolean effectification$discardTick(MobEffect effect, net.minecraft.server.level.ServerLevel level, LivingEntity entity, int amplifier) {
        return effectification$discarded(effect) || effect.applyEffectTick(level, entity, amplifier);
    }
    *///?} else if >=1.20.5 {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)Z"))
    private boolean effectification$discardTick(MobEffect effect, LivingEntity entity, int amplifier) {
        // true keeps the effect, as if the tick had run
        return effectification$discarded(effect) || effect.applyEffectTick(entity, amplifier);
    }
    //?} else if >=1.20.2 {
    /*@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)V"))
    private void effectification$discardTick(MobEffect effect, LivingEntity entity, int amplifier) {
        if (!effectification$discarded(effect)) effect.applyEffectTick(entity, amplifier);
    }
    *///?} else {
    /*@Redirect(method = "applyEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)V"))
    private void effectification$discardTick(MobEffect effect, LivingEntity entity, int amplifier) {
        if (!effectification$discarded(effect)) effect.applyEffectTick(entity, amplifier);
    }
    *///?}

    @Unique
    private static boolean effectification$discarded(MobEffect effect) {
        EffectUnifier unifier = Effectification.getEffectUnifier();
        if (unifier == null) return false;
        String effectId = EffectUtils.getEffectId(effect);
        return effectId != null && unifier.shouldDiscardEffectTick(effectId);
    }
}
