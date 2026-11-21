package dev.utherhel.effectification.util;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

public class EffectUtils {
    
    private static final ConcurrentHashMap<String, ResourceLocation> identifierCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<MobEffect, String> effectIdCache = new ConcurrentHashMap<>();
    
    private EffectUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    public static String getEffectId(MobEffect effect) {
        if (effect == null) {
            return null;
        }
        
        return effectIdCache.computeIfAbsent(effect, e -> {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(e);
            return id != null ? id.toString() : null;
        });
    }
    

    public static MobEffect getEffectByName(String effectId) {
        if (effectId == null || effectId.isEmpty()) {
            return null;
        }
        
        try {
            // Use cached Identifier to avoid repeated object creation
            //? if >=1.21 {
            ResourceLocation location = identifierCache.computeIfAbsent(effectId, ResourceLocation::parse);
            //?} else {
            /*ResourceLocation location = identifierCache.computeIfAbsent(effectId, ResourceLocation::new);
            *///?}
            //? if >=1.21.2 {
            /*return BuiltInRegistries.MOB_EFFECT.getValue(location);
            *///?} else {
            return BuiltInRegistries.MOB_EFFECT.get(location);
            //?}
        } catch (Exception e) {
            return null;
        }
    }
    
    // Gets first available effect from list (fallback logic)
    public static MobEffect getFirstAvailableEffect(List<String> effectIds) {
        if (effectIds == null || effectIds.isEmpty()) {
            return null;
        }
        
        for (String effectId : effectIds) {
            MobEffect effect = getEffectByName(effectId);
            if (effect != null) {
                return effect;
            }
        }
        
        return null;
    }
}
