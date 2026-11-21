package dev.utherhel.effectification.fabric;

//? if fabric {
import dev.kikugie.fletching_table.fabric.Entrypoint;
import dev.utherhel.effectification.Effectification;
import net.fabricmc.api.ModInitializer;

@Entrypoint
public final class EffectificationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Effectification.init();
    }
}
//?}
