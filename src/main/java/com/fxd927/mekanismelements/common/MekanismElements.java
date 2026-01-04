package com.fxd927.mekanismelements.common;

import com.fxd927.mekanismelements.common.config.MSConfig;
import com.fxd927.mekanismelements.common.recipe.MSRecipeType;
import com.fxd927.mekanismelements.common.registries.*;
import mekanism.common.lib.Version;
import mekanism.common.tile.component.TileComponentChunkLoader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import static mekanism.api.MekanismAPI.logger;

@Mod(MekanismElements.MODID)
public class MekanismElements
{
    public static final String MODID = "mekanismelements";

    public final Version versionNumber;
    private MSReloadListener recipeCacheManager;

    public MekanismElements()
    {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::addReloadListenersLowest);
        IEventBus modEventBus = ModLoadingContext.get().getActiveContainer().getEventBus();
        MSConfig.registerConfigs(ModLoadingContext.get());
        modEventBus.addListener(this::commonSetup);
        MSCreativeTab.CREATIVE_TABS.register(modEventBus);
        MSFluids.FLUIDS.register(modEventBus);
        MSGases.GASES.register(modEventBus);
        MSItems.ITEMS.register(modEventBus);
        MSItems.BUILDING_ITEMS.register(modEventBus);
        MSBlocks.BLOCKS.register(modEventBus);
        MSBlocks.BUILDING_BLOCKS.register(modEventBus);
        MSContainerTypes.CONTAINER_TYPES.register(modEventBus);
        MSTileEntityTypes.TILE_ENTITY_TYPES.register(modEventBus);
        MSEffects.MOB_EFFECTS.register(modEventBus);
        MSRecipeType.RECIPE_TYPES.register(modEventBus);
        MSRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        MSSounds.SOUND_EVENTS.register(modEventBus);

        MSGases.Coolants.init();

        versionNumber = new Version(ModLoadingContext.get().getActiveContainer());
        // Events are registered via addListener() calls above, no need to register this class
    }

    public static ResourceLocation rl(String path){
        return ResourceLocation.fromNamespaceAndPath(MekanismElements.MODID, path);
    }

    private void setRecipeCacheManager(MSReloadListener manager) {
        if (recipeCacheManager == null) {
            recipeCacheManager = manager;
        } else {
            logger.warn("Recipe cache manager has already been set.");
        }
    }

    public MSReloadListener getRecipeCacheManager() {
        return recipeCacheManager;
    }

    private void addReloadListenersLowest(AddReloadListenerEvent event) {
        if (recipeCacheManager != null) {
            event.addListener(recipeCacheManager);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        logger.info("Version {} initializing...", versionNumber);
        setRecipeCacheManager(new MSReloadListener());

        event.enqueueWork(() -> {
            // Chunk loading callback registration - may need to be handled differently in NeoForge
            MSFluids.FLUIDS.registerBucketDispenserBehavior();
        });
    }
}
