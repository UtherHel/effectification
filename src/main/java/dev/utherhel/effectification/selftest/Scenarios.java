package dev.utherhel.effectification.selftest;

import dev.utherhel.effectification.Effectification;
import dev.utherhel.effectification.EffectUnifier;
import dev.utherhel.effectification.config.EffectMappingEntry;
import dev.utherhel.effectification.util.EffectUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Server scenes of Effectification, on the mappings of the test config (dev/modtest.toml, files_server):
 *
 * <pre>
 * static   modtest:ghost_effect   -> glowing           a registry alias for an id no mod registers
 * dynamic  poison                 -> wither            amplifier x2
 *          fire_resistance        -> water_breathing   onlyVisualChange
 *          hunger                 -> weakness          unidirectional
 *          luck                   -> luck              extraApplyEffectTick poison
 *          unluck                 -> unluck            discardEffectTick regeneration
 *          slow_fall*             -> levitation        wildcard
 *          blind* !blindness      -> darkness          wildcard whose only match is excluded
 *          night_vision           -> conduit_power     amplifier x3 (the mp player scene)
 * </pre>
 *
 * Every effect goes on its own pig (NoAI, 100 health: wither and poison ticks never kill it).
 */
final class Scenarios {
    private static final String CONVERT = "mt_eff_convert";
    private static final String VISUAL = "mt_eff_visual";
    private static final String UNI = "mt_eff_uni";
    private static final String EXTRA = "mt_eff_extra";
    private static final String DISCARD = "mt_eff_discard";
    private static final String WILD = "mt_eff_wild";
    private static final String EXCLUDED = "mt_eff_excluded";
    private static final String STATIC = "mt_eff_static";
    private static final String[] PIGS = {CONVERT, VISUAL, UNI, EXTRA, DISCARD, WILD, EXCLUDED, STATIC};
    private static final int MINUTE = 20 * 60;

    private Scenarios() {
    }

    static void server(Script<MinecraftServer> s) {
        if (s.scene("config")) {
            s.check("dynamic_mapping_loaded", srv -> {
                EffectUnifier u = Effectification.getEffectUnifier();
                if (u == null) return "no effect unifier";
                EffectMappingEntry m = u.getMapping("minecraft:poison");
                return m != null && "minecraft:wither".equals(m.getTargetEffect()) ? null
                    : "the test config's minecraft:poison -> minecraft:wither mapping is not loaded (got " + m + ")";
            }).check("static_mapping_loaded", srv ->
                Effectification.getEffectUnifier().getStaticMappings().containsKey("modtest:ghost_effect") ? null
                    : "static mappings: " + Effectification.getEffectUnifier().getStaticMappings().keySet());
        }
        if (s.scene("effects")) {
            s.run(srv -> {
                Probe.cmd(srv, "kill @e[type=!minecraft:player]");
                for (String p : PIGS) {
                    Probe.cmd(srv, "summon minecraft:pig 0 -60 4 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"" + p + "\"]}");
                    LivingEntity e = Probe.living(srv, p);
                    e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                    e.setHealth(100);
                }
                pig(srv, CONVERT).addEffect(new MobEffectInstance(MobEffects.POISON, MINUTE, 1));
                pig(srv, VISUAL).addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MINUTE, 1));
                pig(srv, UNI).addEffect(new MobEffectInstance(MobEffects.HUNGER, MINUTE, 0));
                pig(srv, EXTRA).addEffect(new MobEffectInstance(MobEffects.LUCK, MINUTE, 0));
                pig(srv, DISCARD).setHealth(40);
                pig(srv, DISCARD).addEffect(new MobEffectInstance(MobEffects.REGENERATION, MINUTE, 2));
                pig(srv, WILD).addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, MINUTE, 0));
                pig(srv, EXCLUDED).addEffect(new MobEffectInstance(MobEffects.BLINDNESS, MINUTE, 0));
                Probe.cmd(srv, "effect give @e[tag=" + STATIC + "] modtest:ghost_effect 60 0 true");
            })
                .waitTicks(80)
                .check("converted_to_target", srv -> pig(srv, CONVERT).hasEffect(MobEffects.WITHER) ? null
                    : "a pig given poison has no wither: " + effects(pig(srv, CONVERT)))
                .check("amplifier_multiplied", srv -> {
                    MobEffectInstance i = pig(srv, CONVERT).getEffect(MobEffects.WITHER);
                    return i != null && i.getAmplifier() == 2 ? null
                        : "poison amplifier 1 x2 should be wither amplifier 2: " + effects(pig(srv, CONVERT));
                })
                .check("source_still_seen", srv -> pig(srv, CONVERT).hasEffect(MobEffects.POISON) ? null
                    : "hasEffect(poison) is false on the converted pig, although the mapping is not unidirectional "
                    + "(CHANGELOG: only unidirectional mappings hide the source from hasEffect)")
                .check("unidirectional_converted", srv -> pig(srv, UNI).hasEffect(MobEffects.WEAKNESS) ? null
                    : "a pig given hunger has no weakness: " + effects(pig(srv, UNI)))
                .check("unidirectional_hides_source", srv -> !pig(srv, UNI).hasEffect(MobEffects.HUNGER) ? null
                    : "hasEffect(hunger) is true although the mapping is unidirectional")
                .check("visual_only_not_converted", srv -> {
                    LivingEntity p = pig(srv, VISUAL);
                    MobEffectInstance i = p.getEffect(MobEffects.FIRE_RESISTANCE);
                    return i != null && i.getAmplifier() == 1 && !p.hasEffect(MobEffects.WATER_BREATHING) ? null
                        : "onlyVisualChange should keep fire_resistance (amplifier 1): " + effects(p);
                })
                .check("visual_only_renamed", srv -> {
                    MobEffectInstance i = pig(srv, VISUAL).getEffect(MobEffects.FIRE_RESISTANCE);
                    if (i != null) i.getEffect(); // the rename keys on the effect id that getEffect() caches
                    String want = TestCompat.effect(MobEffects.WATER_BREATHING).getDescriptionId();
                    return i != null && want.equals(i.getDescriptionId()) ? null
                        : "fire_resistance is named " + (i == null ? null : i.getDescriptionId()) + ", expected " + want;
                })
                .check("extra_tick_applied", srv -> pig(srv, EXTRA).getHealth() < 100 ? null
                    : "luck with extraApplyEffectTick poison did no poison damage in 80 ticks (health "
                    + pig(srv, EXTRA).getHealth() + ")")
                .check("discard_tick_blocks", srv -> Probe.near("health of a pig with regeneration II whose tick is "
                    + "discarded", pig(srv, DISCARD).getHealth(), 40, 1e-3))
                .check("wildcard_converted", srv -> pig(srv, WILD).hasEffect(MobEffects.LEVITATION) ? null
                    : "slow_falling (slow_fall*) was not converted to levitation: " + effects(pig(srv, WILD)))
                .check("wildcard_exclusion", srv -> {
                    LivingEntity p = pig(srv, EXCLUDED);
                    return p.hasEffect(MobEffects.BLINDNESS) && !p.hasEffect(MobEffects.DARKNESS) ? null
                        : "blindness is excluded from blind* but was converted: " + effects(p);
                })
                .check("static_alias", srv -> pig(srv, STATIC).hasEffect(MobEffects.GLOWING) ? null
                    : "/effect give ... modtest:ghost_effect did not give glowing: " + effects(pig(srv, STATIC)))
                .check("remove_by_source", srv -> {
                    LivingEntity p = pig(srv, CONVERT);
                    p.removeEffect(MobEffects.POISON);
                    return !p.hasEffect(MobEffects.WITHER) ? null
                        : "removeEffect(poison) left the converted wither: " + effects(p);
                })
                .run(srv -> Probe.cmd(srv, "kill @e[type=!minecraft:player]"));
        }
    }

    static void player(Script<MinecraftServer> s) {
        if (s.scene("player")) {
            s.run(srv -> {
                ServerPlayer p = SelfTest.player(srv);
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 5 * MINUTE, 1));
                p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 5 * MINUTE, 0));
            }).waitTicks(10)
                .check("player_effect_converted", srv -> {
                    MobEffectInstance i = SelfTest.player(srv).getEffect(MobEffects.CONDUIT_POWER);
                    return i != null && i.getAmplifier() == 3 ? null
                        : "night_vision amplifier 1 x3 should be conduit_power amplifier 3: " + effects(SelfTest.player(srv));
                });
        }
    }

    // ---------------------------------------------------------------- helpers
    static LivingEntity pig(MinecraftServer srv, String tag) {
        return Probe.living(srv, tag);
    }

    static String effects(LivingEntity e) {
        StringBuilder sb = new StringBuilder("[");
        for (MobEffectInstance i : e.getActiveEffects()) {
            if (sb.length() > 1) sb.append(", ");
            sb.append(EffectUtils.getEffectId(TestCompat.effect(i.getEffect()))).append(" ").append(i.getAmplifier());
        }
        return sb.append("]").toString();
    }
}
