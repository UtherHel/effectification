package dev.utherhel.effectification.neoforge;

import dev.utherhel.effectification.Effectification;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Effectification.MOD_ID)
public final class EffectificationNeoForge {
    public EffectificationNeoForge(IEventBus modEventBus) {
        Effectification.init();
    }
}
