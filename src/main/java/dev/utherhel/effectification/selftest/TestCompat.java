package dev.utherhel.effectification.selftest;

import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;

/**
 * The common calls of the self-test that differ between Minecraft versions (1.20.1 - 26.x, Stonecutter conditions;
 * the active version decides which branch is uncommented). Created by dev/modtest/sync.py when missing; owned by the
 * mod (scenes add their own helpers here).
 */
final class TestCompat {
    private TestCompat() {
    }

    static String versionName() {
//? if >=1.21.6 {
/*        return SharedConstants.getCurrentVersion().name();
*///?} else {
        return SharedConstants.getCurrentVersion().getName();
//?}
    }

    /** Runs a console command as the server (permission level 4, output suppressed). */
    static void command(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** Scoreboard tags of an entity. */
    static Set<String> tags(Entity e) {
//? if >=26.1 {
/*        return e.entityTags();
*///?} else {
        return e.getTags();
//?}
    }

    /** Vanilla attribute id as commands and datapacks spell it: "generic." / "player." prefixes were dropped in 1.21.2. */
    static String attributeId(String name) {
//? if >=1.21.2 {
/*        return "minecraft:" + name;
*///?} else {
        return "minecraft:" + (name.equals("block_interaction_range") || name.equals("entity_interaction_range")
            || name.equals("block_break_speed") ? "player." : "generic.") + name;
//?}
    }

    // ---------------------------------------------------------------- Effectification scenes

    /** The effect behind a {@code MobEffects} constant (a holder since 1.20.5). */
//? if >=1.20.5 {
    static MobEffect effect(Holder<MobEffect> e) {
        return e.value();
    }
//?} else {
/*    static MobEffect effect(MobEffect e) {
        return e;
    }
*///?}
}
