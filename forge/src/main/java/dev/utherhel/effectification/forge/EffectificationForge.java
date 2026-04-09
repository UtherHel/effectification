package dev.utherhel.effectification.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.utherhel.effectification.Effectification;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Effectification.MOD_ID)
public final class EffectificationForge {
    public EffectificationForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(Effectification.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        
        Effectification.init();
    }
}
