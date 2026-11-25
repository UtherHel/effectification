package dev.utherhel.effectification.selftest;

import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectUnifier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Client scenes of the self-test (client-only class). {@link #title} runs from the title screen without a world
 * (dev/mt.py client), {@link #client} in the world of a dedicated server (dev/mt.py mp).
 *
 * <p>The client has no test config of its own: on a server with the mod its mappings must arrive with Fzzy Config's
 * config sync (the config registers with RegisterType.BOTH), which the visual-only rename on the client shows.
 */
final class ClientScenarios {
    private ClientScenarios() {
    }

    static void title(Script<Minecraft> s) {
        if (s.scene("title")) {
            s.check("unifier_ready", mc -> Effectification.getEffectUnifier() != null ? null
                : "no effect unifier on the client");
        }
    }

    static void client(Script<Minecraft> s) {
        if (s.scene("client")) {
            s.check("player_present", mc -> mc.player != null && mc.player.isAlive() ? null : "no living local player");
            if (!ClientSelfTest.serverMod()) {
                s.check("unifier_ready", mc -> Effectification.getEffectUnifier() != null ? null
                    : "no effect unifier on the client");
                return;
            }
            // the server's player scene gives night_vision II (-> conduit_power, amplifier x3) and fire_resistance
            // (onlyVisualChange -> named water_breathing)
            s.until("effects from the server", 20 * 60, mc -> mc.player.hasEffect(MobEffects.CONDUIT_POWER)
                    && mc.player.hasEffect(MobEffects.FIRE_RESISTANCE))
                .check("converted_effect_received", mc -> {
                    MobEffectInstance i = mc.player.getEffect(MobEffects.CONDUIT_POWER);
                    return i != null && i.getAmplifier() == 3 ? null
                        : "conduit_power amplifier " + (i == null ? null : i.getAmplifier()) + ", expected 3";
                })
                .eventually("server_mappings_synced", 20 * 30, mc -> {
                    EffectUnifier u = Effectification.getEffectUnifier();
                    return u != null && u.getMapping("minecraft:poison") != null ? null
                        : "the client has no mapping for minecraft:poison: the server's config did not reach it";
                })
                .check("visual_only_renamed_on_client", mc -> {
                    MobEffectInstance i = mc.player.getEffect(MobEffects.FIRE_RESISTANCE);
                    if (i != null) i.getEffect(); // the rename keys on the effect id that getEffect() caches
                    String want = TestCompat.effect(MobEffects.WATER_BREATHING).getDescriptionId();
                    return i != null && want.equals(i.getDescriptionId()) ? null
                        : "the client names fire_resistance " + (i == null ? null : i.getDescriptionId()) + ", expected "
                        + want;
                });
        }
    }
}
