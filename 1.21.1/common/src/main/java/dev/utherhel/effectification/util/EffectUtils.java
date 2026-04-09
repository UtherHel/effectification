package dev.utherhel.effectification.util;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class EffectUtils {
    
    private static final ConcurrentHashMap<String, Identifier> identifierCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<StatusEffect, String> effectIdCache = new ConcurrentHashMap<>();
    
    private EffectUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    public static String getEffectId(StatusEffect effect) {
        if (effect == null) {
            return null;
        }
        
        return effectIdCache.computeIfAbsent(effect, e -> {
            Identifier id = Registries.STATUS_EFFECT.getId(e);
            return id != null ? id.toString() : null;
        });
    }
    

    public static StatusEffect getEffectByName(String effectId) {
        if (effectId == null || effectId.isEmpty()) {
            return null;
        }
        
        try {
            // Use cached Identifier to avoid repeated object creation
            Identifier location = identifierCache.computeIfAbsent(effectId, Identifier::of);
            return Registries.STATUS_EFFECT.get(location);
        } catch (Exception e) {
            return null;
        }
    }
    
    // Gets first available effect from list (fallback logic)
    public static StatusEffect getFirstAvailableEffect(List<String> effectIds) {
        if (effectIds == null || effectIds.isEmpty()) {
            return null;
        }
        
        for (String effectId : effectIds) {
            StatusEffect effect = getEffectByName(effectId);
            if (effect != null) {
                return effect;
            }
        }
        
        return null;
    }
}
