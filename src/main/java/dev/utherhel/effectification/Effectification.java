package dev.utherhel.effectification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.utherhel.effectification.config.EffectificationConfig;
import dev.utherhel.effectification.config.EffectMappingEntry;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?}
//? if forge {
/*import net.minecraftforge.fml.loading.FMLPaths;
*///?}
//? if neoforge {
/*import net.neoforged.fml.loading.FMLPaths;
*///?}

public final class Effectification {
    public static final String MOD_ID = "effectification";
    private static volatile EffectUnifier effectUnifier;
    private static EffectificationConfig config;
    private static boolean isFabric = false;
    private static volatile long effectUnifierVersion = 0; // Incremented on each reload
    /** The effect registry is complete (all mods registered their effects); mappings can be checked against it. */
    private static volatile boolean effectRegistryReady = false;
    /** The static mappings the registry aliases were made from: changing them needs a restart. */
    private static List<String> appliedStaticMappings = List.of();
    
    private static final List<Runnable> cacheClearCallbacks = new ArrayList<>();
    
    private Effectification() {
    }
    
    public static void init() {
        //? if fabric {
        isFabric = true;
        //?} else {
        /*isFabric = false;
        *///?}
        //? if fabric {
        EffectificationLogger.info("Running on platform: Fabric");
        //?} else if neoforge {
        /*EffectificationLogger.info("Running on platform: NeoForge");
        *///?} else {
        /*EffectificationLogger.info("Running on platform: Forge");
        *///?}
        
        EffectificationConfig migratedConfig = migrateFromOldConfig();
        
        if (migratedConfig != null) {
            config = migratedConfig;
        } else {
            Supplier<EffectificationConfig> configSupplier = EffectificationConfig::new;
            config = ConfigApi.registerAndLoadConfig(configSupplier, RegisterType.BOTH);
        }
        
        effectUnifier = new EffectUnifier(config);
        effectUnifierVersion++; // Increment version to invalidate all caches
        
        // Register cache clear callback for StatusEffectInstance caches
        registerCacheClearCallback(() -> {
            EffectificationLogger.info("EffectUnifier version incremented, StatusEffectInstance caches will be invalidated");
        });
        
        EffectificationLogger.info("Loaded " + config.getMappings().size() + " effect mappings");
        config.getMappings().forEach(mapping -> 
            EffectificationLogger.info("  - " + mapping.toString())
        );
    }
    

    private static EffectificationConfig migrateFromOldConfig() {
        Path oldConfigPath = configFolder().resolve("effectification/effect_mappings.json");
        Path newConfigPath = configFolder().resolve("effectification/effectification.toml");
        
        if (!shouldMigrate(oldConfigPath, newConfigPath)) {
            return null;
        }
        
        EffectificationLogger.info("Found old JSON config, migrating to new TOML format...");
        
        try {
            String json = Files.readString(oldConfigPath);
            EffectificationConfig newConfig = parseOldConfig(json);
            EffectificationConfig registeredConfig = registerMigratedConfig(newConfig);
            backupOldConfig(oldConfigPath);
            
            EffectificationLogger.info("Successfully migrated " + newConfig.getMappings().size() + " mappings to new TOML format");
            return registeredConfig;
        } catch (IOException e) {
            EffectificationLogger.error("Failed to migrate old config: " + e.getMessage(), e);
            EffectificationLogger.error("Will use default configuration instead");
            return null;
        }
    }
    
    private static boolean shouldMigrate(Path oldConfigPath, Path newConfigPath) {
        return Files.exists(oldConfigPath) && !Files.exists(newConfigPath);
    }
    
    private static EffectificationConfig parseOldConfig(String json) {
        Gson gson = new GsonBuilder().create();
        JsonObject root = gson.fromJson(json, JsonObject.class);
        
        EffectificationConfig newConfig = new EffectificationConfig();
        newConfig.staticMappings.clear();
        newConfig.dynamicMappings.clear();
        
        JsonArray mappingsArray = extractMappingsArray(root, gson, json);
        if (mappingsArray != null) {
            parseMappings(mappingsArray, newConfig);
        }
        
        return newConfig;
    }
    
    private static JsonArray extractMappingsArray(JsonObject root, Gson gson, String json) {
        if (root.has("mappings")) {
            return root.getAsJsonArray("mappings");
        }
        return gson.fromJson(json, JsonArray.class);
    }
    
    private static void parseMappings(JsonArray mappingsArray, EffectificationConfig newConfig) {
        for (JsonElement element : mappingsArray) {
            JsonObject mapping = element.getAsJsonObject();
            EffectMappingEntry newMapping = createMappingFromJson(mapping);
            newConfig.staticMappings.add(newMapping);
        }
    }
    
    private static EffectMappingEntry createMappingFromJson(JsonObject mapping) {
        String sourceEffect = extractString(mapping, "sourceEffect");
        String targetEffect = extractString(mapping, "targetEffect");
        double amplifierMultiplier = extractDouble(mapping, "amplifierMultiplier");
        boolean onlyVisualChange = extractBoolean(mapping, "onlyVisualChange");
        boolean unidirectional = extractBoolean(mapping, "unidirectional");
        String extraApplyEffectTick = extractString(mapping, "extraApplyEffectTick");
        String discardEffectTick = extractString(mapping, "discardEffectTick");
        
        return new EffectMappingEntry(
            sourceEffect, targetEffect, amplifierMultiplier,
            onlyVisualChange, unidirectional,
            extraApplyEffectTick, discardEffectTick
        );
    }
    
    private static String extractString(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return "";
        }
        JsonElement element = obj.get(key);
        if (element.isJsonObject() && element.getAsJsonObject().has("storedValue")) {
            return element.getAsJsonObject().get("storedValue").getAsString();
        }
        return element.getAsString();
    }
    
    private static double extractDouble(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return 1.0;
        }
        JsonElement element = obj.get(key);
        if (element.isJsonObject() && element.getAsJsonObject().has("storedValue")) {
            return element.getAsJsonObject().get("storedValue").getAsDouble();
        }
        return element.getAsDouble();
    }
    
    private static boolean extractBoolean(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return false;
        }
        JsonElement element = obj.get(key);
        if (element.isJsonObject() && element.getAsJsonObject().has("storedValue")) {
            return element.getAsJsonObject().get("storedValue").getAsBoolean();
        }
        return element.getAsBoolean();
    }
    

    
    private static EffectificationConfig registerMigratedConfig(EffectificationConfig newConfig) {
        Supplier<EffectificationConfig> supplier = () -> newConfig;
        return ConfigApi.registerAndLoadConfig(supplier, RegisterType.BOTH);
    }
    
    private static void backupOldConfig(Path oldConfigPath) throws IOException {
        Path backupPath = oldConfigPath.resolveSibling("effect_mappings.json.old");
        Files.move(oldConfigPath, backupPath);
        EffectificationLogger.info("Old config backed up to: " + backupPath);
    }
    

    public static void registerCacheClearCallback(Runnable callback) {
        cacheClearCallbacks.add(callback);
    }
    
    // Reloads EffectUnifier when config changes
    // Registry aliases require restart, but unidirectional/extra/discard ticks update immediately
    /** Called once the effect registry is complete: the static aliases are in, and the mappings can be checked. */
    public static void onEffectRegistryReady() {
        // NeoForge freezes the registry several times during startup: report the mappings only the first time
        boolean first = !effectRegistryReady;
        effectRegistryReady = true;
        appliedStaticMappings = describe(config.getStaticMappings());
        if (first) {
            dev.utherhel.effectification.config.MappingCheck.report(config);
        }
    }

    private static List<String> describe(List<EffectMappingEntry> mappings) {
        List<String> out = new ArrayList<>();
        for (EffectMappingEntry m : mappings) {
            out.add(m.toString());
        }
        return out;
    }

    public static void reloadEffectUnifier() {
        for (Runnable callback : cacheClearCallbacks) {
            try {
                callback.run();
            } catch (Exception e) {
                EffectificationLogger.warn("Failed to clear cache: " + e.getMessage());
            }
        }
        
        effectUnifier = new EffectUnifier(config);
        effectUnifierVersion++; // Increment version to invalidate all caches
        EffectificationLogger.resetWarnOnce();
        
        EffectificationLogger.info("Reloaded " + config.getMappings().size() + " effect mappings");
        config.getMappings().forEach(mapping -> 
            EffectificationLogger.info("  - " + mapping.toString())
        );
        if (effectRegistryReady) {
            if (!describe(config.getStaticMappings()).equals(appliedStaticMappings)) {
                EffectificationLogger.warn("The static mappings changed: the ids they give to other effects change after"
                    + " a restart. Everything else applies now.");
            }
            dev.utherhel.effectification.config.MappingCheck.report(config);
        }
    }
    
    public static long getEffectUnifierVersion() {
        return effectUnifierVersion;
    }
    
    public static EffectUnifier getEffectUnifier() {
        return effectUnifier;
    }
    
    public static EffectificationConfig getConfig() {
        return config;
    }
    
    private static Path configFolder() {
        //? if fabric {
        return FabricLoader.getInstance().getConfigDir();
        //?} else {
        /*return FMLPaths.CONFIGDIR.get();
        *///?}
    }
    
    public static boolean isFabric() {
        return isFabric;
    }
}
