package dev.utherhel.effectification.forge;

//? if forge {
/*import dev.utherhel.effectification.EffectUnifier;
import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectificationLogger;
import dev.utherhel.effectification.config.EffectMappingEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

@Mod(Effectification.MOD_ID)
public final class EffectificationForge {
    public EffectificationForge() {
        Effectification.init();
        // Forge's registry wrapper replaces MappedRegistry.freeze, so RegistryMixin never sees the effect registry
        // here: the static aliases and the mapping check run once every mod has registered its effects
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EffectificationForge::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            injectStaticAliases();
            Effectification.onEffectRegistryReady();
        });
    }

    // Gives the id of every static source that no mod registers to its target, in Forge's own registry: saved effects
    // with that id load as the target. Registered sources keep their id and are converted at runtime.
    private static void injectStaticAliases() {
        EffectUnifier unifier = Effectification.getEffectUnifier();
        ForgeRegistry<MobEffect> registry = RegistryManager.ACTIVE.getRegistry(ForgeRegistries.Keys.MOB_EFFECTS);
        if (unifier == null || registry == null) {
            return;
        }
        int injected = 0;
        for (EffectMappingEntry mapping : unifier.getStaticMappings().values()) {
            String id = mapping.getSourceEffect().trim();
            int bang = id.indexOf('!');
            if (bang >= 0) id = id.substring(0, bang).trim();
            if (mapping.isOnlyVisualChange() || id.contains("*")) {
                continue;
            }
            ResourceLocation source = ResourceLocation.tryParse(id);
            MobEffect target = mapping.getResolvedTargetEffect();
            ResourceLocation targetId = target == null ? null : registry.getKey(target);
            if (source == null || targetId == null || registry.containsKey(source)) {
                continue; // a missing target is reported by MappingCheck
            }
            // the registries are locked once registration is over; aliases can only be added while unlocked
            boolean locked = registry.isLocked();
            try {
                if (locked) registry.unfreeze();
                registry.addAlias(source, targetId);
                injected++;
            } catch (RuntimeException e) {
                EffectificationLogger.warn("[Registry] Forge did not take the alias " + id + ": " + e.getMessage());
            } finally {
                if (locked) registry.freeze();
            }
        }
        if (injected > 0) {
            EffectificationLogger.info("[Registry] Injected " + injected + " effect aliases into Forge's registry");
        }
    }
}
*///?}
