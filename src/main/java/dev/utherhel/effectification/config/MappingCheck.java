package dev.utherhel.effectification.config;

import dev.utherhel.effectification.EffectificationLogger;
import dev.utherhel.effectification.util.EffectUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Checks the mappings against the effects that are actually registered, so a typo is told apart from a mapping for a
 * mod that is simply not installed: the first is a problem (logged as a warning, shown in red in the config screen),
 * the second only makes the mapping wait (logged at ALL).
 */
public final class MappingCheck {
    private static final String PLACEHOLDER_SOURCE = "modid:effect_name";
    private static final String PLACEHOLDER_TARGET = "modid:target_effect";

    private MappingCheck() {
    }

    /** What happens with one mapping on this game. */
    public enum State { ACTIVE, WAITING, PROBLEM }

    public record Result(State state, String message) {
    }

    /** The problem of a mapping as one sentence, or null when it has none (waiting for a mod is not a problem). */
    public static String problem(EffectMappingEntry mapping) {
        try {
            Result r = check(mapping, false);
            return r.state() == State.PROBLEM ? r.message() : null;
        } catch (RuntimeException e) {
            return null; // the registry is not there yet (config screen opened very early)
        }
    }

    public static Result check(EffectMappingEntry mapping, boolean isStatic) {
        Set<String> namespaces = namespaces();
        String source = mapping.getSourceEffect() == null ? "" : mapping.getSourceEffect().trim();
        String target = mapping.getTargetEffect() == null ? "" : mapping.getTargetEffect().trim();
        if (source.isEmpty() || source.equals(PLACEHOLDER_SOURCE)) {
            return new Result(State.PROBLEM, "the source effect is not filled in");
        }
        if (target.isEmpty() || target.equals(PLACEHOLDER_TARGET)) {
            return new Result(State.PROBLEM, "the target effect is not filled in");
        }
        int bang = source.indexOf('!');
        String main = (bang < 0 ? source : source.substring(0, bang)).trim();
        if (main.indexOf(':') <= 0) {
            return new Result(State.PROBLEM, "source '" + main + "' needs the form namespace:effect, e.g. minecraft:poison");
        }
        if (main.equals("*:*")) {
            return new Result(State.PROBLEM, "source '*:*' would convert every effect and is ignored");
        }
        if (Double.isNaN(mapping.getAmplifierMultiplier()) || mapping.getAmplifierMultiplier() < 0) {
            return new Result(State.PROBLEM, "amplifierMultiplier must be 0 or more, not " + mapping.getAmplifierMultiplier());
        }
        for (String t : mapping.getTargetEffects()) {
            if (t.contains("*")) {
                return new Result(State.PROBLEM, "target '" + t + "' cannot contain *: name the effect itself");
            }
            if (t.indexOf(':') <= 0) {
                return new Result(State.PROBLEM, "target '" + t + "' needs the form namespace:effect");
            }
        }
        for (String id : idList(mapping.getExtraApplyEffectTickList(), mapping.getDiscardEffectTickList())) {
            if (!id.contains("*") && id.indexOf(':') > 0 && EffectUtils.getEffectByName(id) == null
                && namespaces.contains(namespace(id))) {
                return new Result(State.PROBLEM, "there is no effect '" + id + "' (extra/discard tick); check the spelling");
            }
        }

        // the targets: the first installed one is used
        if (mapping.getResolvedTargetEffect() == null) {
            List<String> typos = new ArrayList<>();
            for (String t : mapping.getTargetEffects()) {
                if (namespaces.contains(namespace(t))) typos.add(t);
            }
            if (!typos.isEmpty()) {
                return new Result(State.PROBLEM, "there is no effect " + String.join(", ", typos)
                    + " although its mod is installed; check the spelling");
            }
            return new Result(State.WAITING, "none of its targets is installed (" + target + ")");
        }

        // the source
        if (main.contains("*")) {
            for (ResourceLocation id : BuiltInRegistries.MOB_EFFECT.keySet()) {
                if (mapping.matches(id.toString())) return new Result(State.ACTIVE, "");
            }
            return new Result(State.WAITING, "no installed effect matches '" + main + "'");
        }
        if (EffectUtils.getEffectByName(main) == null) {
            if (isStatic) {
                return new Result(State.ACTIVE, ""); // a static mapping makes this id point at the target
            }
            if (namespaces.contains(namespace(main))) {
                return new Result(State.PROBLEM, "there is no effect '" + main + "' although its mod is installed;"
                    + " check the spelling (or make it a static mapping to give that id to the target)");
            }
            return new Result(State.WAITING, "the mod of '" + main + "' is not installed");
        }
        return new Result(State.ACTIVE, "");
    }

    /** Logs every problem as a warning, the waiting mappings at ALL, and a one-line summary. */
    public static void report(EffectificationConfig config) {
        int active = 0;
        int waiting = 0;
        int problems = 0;
        List<EffectMappingEntry> statics = config.getStaticMappings();
        List<EffectMappingEntry> all = config.getMappings();
        for (int i = 0; i < all.size(); i++) {
            EffectMappingEntry m = all.get(i);
            boolean isStatic = i < statics.size();
            Result r;
            try {
                r = check(m, isStatic);
            } catch (RuntimeException e) {
                EffectificationLogger.warn("Could not check mapping " + m + ": " + e);
                continue;
            }
            String where = (isStatic ? "static" : "dynamic") + " mapping " + m.getSourceEffect() + " -> " + m.getTargetEffect();
            switch (r.state()) {
                case ACTIVE -> active++;
                case WAITING -> {
                    waiting++;
                    EffectificationLogger.info("Inactive " + where + ": " + r.message());
                }
                case PROBLEM -> {
                    problems++;
                    EffectificationLogger.warn("Problem in " + where + ": " + r.message());
                }
            }
        }
        EffectificationLogger.summary(all.size() + " effect mappings (" + statics.size() + " static, "
            + (all.size() - statics.size()) + " dynamic): " + active + " active, " + waiting
            + " waiting for mods that are not installed" + (problems > 0 ? ", " + problems
            + " with problems (see the warnings above)" : "") + ".");
    }

    private static List<String> idList(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(a);
        for (String s : b) {
            int bang = s.indexOf('!');
            out.add((bang < 0 ? s : s.substring(0, bang)).trim());
        }
        return out;
    }

    private static String namespace(String id) {
        int colon = id.indexOf(':');
        return colon > 0 ? id.substring(0, colon).trim() : "";
    }

    private static Set<String> namespaces() {
        Set<String> out = new HashSet<>();
        for (ResourceLocation id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            out.add(id.getNamespace());
        }
        return out;
    }
}
