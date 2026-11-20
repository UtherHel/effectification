package dev.utherhel.effectification.config;

import dev.utherhel.effectification.EffectificationLogger;
import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.annotations.RequiresAction;
import me.fzzyhmstrs.fzzy_config.annotations.Action;
import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

@Version(version = 6)
@Translation(prefix = "effectification.config")
public class EffectificationConfig extends Config {
    
    @Comment("Static mappings - registry aliasing at startup (requires restart)")
    @RequiresAction(action = Action.RESTART)
    public ValidatedList<EffectMappingEntry> staticMappings;
    
    @Comment("Dynamic mappings - runtime effect conversion (reloadable)")
    public ValidatedList<EffectMappingEntry> dynamicMappings;
    
    public DebugSection debug = new DebugSection();
    
    public EffectificationConfig() {
        //? if >=1.21 {
        super(ResourceLocation.fromNamespaceAndPath("effectification", "effectification"));
        //?} else {
        /*super(ResourceLocation.tryBuild("effectification", "effectification"));
        *///?}
        ValidatedAny<EffectMappingEntry> handler = new ValidatedAny<>(new EffectMappingEntry());
        
        this.staticMappings = handler.toList(new ArrayList<>());
        this.dynamicMappings = handler.toList(createDefaultMappings());
    }
    
    @Override
    public int defaultPermLevel() {
        return 2;
    }
    
    @Override
    public me.fzzyhmstrs.fzzy_config.api.SaveType saveType() {
        return me.fzzyhmstrs.fzzy_config.api.SaveType.OVERWRITE;
    }
    
    /**
     * Creates default dynamic mappings based on platform
     */
    private static List<EffectMappingEntry> createDefaultMappings() {
        boolean isFabric = dev.utherhel.effectification.Effectification.isFabric();
        return isFabric ? DefaultMappings.getFabricDynamicDefaults() 
                        : DefaultMappings.getForgeDynamicDefaults();
    }
    
    public List<EffectMappingEntry> getMappings() {
        List<EffectMappingEntry> combined = new ArrayList<>();
        combined.addAll(staticMappings);
        combined.addAll(dynamicMappings);
        return combined;
    }
    
    public List<EffectMappingEntry> getStaticMappings() {
        return new ArrayList<>(staticMappings);
    }
    
    public List<EffectMappingEntry> getDynamicMappings() {
        return new ArrayList<>(dynamicMappings);
    }
    
    @Override
    public void update(int deserializedVersion) {
        if (deserializedVersion < 6) {
            EffectificationLogger.info("Migrating config from version " + deserializedVersion + " to 6");
        }
        
        ValidatedAny<EffectMappingEntry> handler = new ValidatedAny<>(new EffectMappingEntry());
        
        if (staticMappings == null) {
            staticMappings = handler.toList(new ArrayList<>());
        }
        if (dynamicMappings == null) {
            dynamicMappings = handler.toList(new ArrayList<>());
        }
        
        if (staticMappings.isEmpty() && dynamicMappings.isEmpty()) {
            EffectificationLogger.info("No mappings found, adding defaults to dynamic");
            dynamicMappings = handler.toList(createDefaultMappings());
        }
    }
    
    /**
     * Called automatically by fconfig when config is updated on client.
     * Reloads the EffectUnifier to apply new mappings immediately.
     */
    @Override
    public void onUpdateClient() {
        dev.utherhel.effectification.Effectification.reloadEffectUnifier();
    }
    
    /**
     * Called automatically by fconfig when config is updated on server.
     * Reloads the EffectUnifier to apply new mappings immediately.
     */
    @Override
    public void onUpdateServer(me.fzzyhmstrs.fzzy_config.event.api.ServerUpdateContext context) {
        dev.utherhel.effectification.Effectification.reloadEffectUnifier();
    }
}
