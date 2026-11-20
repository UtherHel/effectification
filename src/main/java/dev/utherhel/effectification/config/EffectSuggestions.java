package dev.utherhel.effectification.config;

import me.fzzyhmstrs.fzzy_config.util.AllowableStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Provides smart autosuggestions for effect IDs with wildcard support
 */
public final class EffectSuggestions {
    
    private EffectSuggestions() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static AllowableStrings create(boolean allowWildcards) {
        Predicate<String> predicate = s -> true;
        Supplier<List<String>> supplier = () -> getAllEffectIds(allowWildcards);
        return new AllowableStrings(predicate, supplier);
    }
    
    private static List<String> getAllEffectIds(boolean includeWildcards) {
        List<String> effectIds = new ArrayList<>();
        Map<String, List<String>> effectsByName = new LinkedHashMap<>();
        
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (id != null) {
                String fullId = id.toString();
                String name = id.getPath();
                
                effectsByName.computeIfAbsent(name, k -> new ArrayList<>()).add(fullId);
            }
        }
        
        if (includeWildcards) {
            List<String> wildcards = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : effectsByName.entrySet()) {
                if (entry.getValue().size() > 1) {
                    wildcards.add("*:" + entry.getKey());
                }
            }
            wildcards.sort(String::compareTo);
            effectIds.addAll(wildcards);
        }
        
        List<String> allIds = new ArrayList<>();
        for (List<String> ids : effectsByName.values()) {
            allIds.addAll(ids);
        }
        allIds.sort(String::compareTo);
        effectIds.addAll(allIds);
        
        return effectIds;
    }
}
