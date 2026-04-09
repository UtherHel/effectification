package dev.utherhel.effectification.fabric;

import dev.utherhel.effectification.Effectification;
import net.fabricmc.api.ModInitializer;

public final class EffectificationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Effectification.init();
    }
}
