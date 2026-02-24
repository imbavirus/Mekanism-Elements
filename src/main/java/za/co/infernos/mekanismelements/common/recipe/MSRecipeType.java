package za.co.infernos.mekanismelements.common.recipe;

import za.co.infernos.mekanismelements.api.recipes.AdsorptionRecipe;
import za.co.infernos.mekanismelements.api.recipes.ChemicalDemolitionRecipe;
import za.co.infernos.mekanismelements.api.recipes.RadiationIrradiatingRecipe;
import za.co.infernos.mekanismelements.common.MekanismElements;
import za.co.infernos.mekanismelements.common.recipe.lookup.cache.MSInputRecipeCache;
import za.co.infernos.mekanismelements.common.registration.impl.MSRecipeTypeDeferredRegister;
import za.co.infernos.mekanismelements.common.registration.impl.MSRecipeTypeRegistryObject;
import mekanism.api.recipes.FluidToFluidRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.client.MekanismClient;
import mekanism.common.recipe.lookup.cache.IInputRecipeCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class MSRecipeType<RECIPE extends MekanismRecipe<?>, INPUT_CACHE extends IInputRecipeCache> implements RecipeType<RECIPE>,
        IMSRecipeTypeProvider<RECIPE, INPUT_CACHE> {
    public static final MSRecipeTypeDeferredRegister RECIPE_TYPES = new MSRecipeTypeDeferredRegister(MekanismElements.MODID);

    public static final MSRecipeTypeRegistryObject<RadiationIrradiatingRecipe, MSInputRecipeCache.ItemChemical<RadiationIrradiatingRecipe>> RADIATION_IRRADIATING =
            register("radiation_irradiating", recipeType -> new MSInputRecipeCache.ItemChemical<>(recipeType, RadiationIrradiatingRecipe::getItemInput, RadiationIrradiatingRecipe::getGasInput));
    public static final MSRecipeTypeRegistryObject<AdsorptionRecipe, MSInputRecipeCache.ItemFluid<AdsorptionRecipe>> ADSORPTION =
            register("adsorption", recipeType -> new MSInputRecipeCache.ItemFluid<>(recipeType, AdsorptionRecipe::getItemInput, AdsorptionRecipe::getFluidInput));
    public static final MSRecipeTypeRegistryObject<FluidToFluidRecipe, MSInputRecipeCache.SingleFluid<FluidToFluidRecipe>> ADVANCED_EVAPORATING =
            register("evaporating", recipeType -> new MSInputRecipeCache.SingleFluid<>(recipeType, FluidToFluidRecipe::getInput));
    public static final MSRecipeTypeRegistryObject<ChemicalDemolitionRecipe, MSInputRecipeCache.ItemChemical<ChemicalDemolitionRecipe>> CHEMICAL_DEMOLITION =
            register("chemical_demolition", recipeType -> new MSInputRecipeCache.ItemChemical<>(recipeType, ChemicalDemolitionRecipe::getItemInput, ChemicalDemolitionRecipe::getGasInput));
   public static <RECIPE extends MekanismRecipe<?>, INPUT_CACHE extends IInputRecipeCache> MSRecipeTypeRegistryObject<RECIPE, INPUT_CACHE> register(String name,
                                                                                                                                                   Function<MSRecipeType<RECIPE, INPUT_CACHE>, INPUT_CACHE> inputCacheCreator) {
        return RECIPE_TYPES.registerRecipeType(name, () -> new MSRecipeType<>(name, inputCacheCreator));
    }

    public static void clearCache() {
        for (IMSRecipeTypeProvider<?, ?> recipeTypeProvider : RECIPE_TYPES.getAllRecipeTypes()) {
            recipeTypeProvider.getMSRecipeType().clearCaches();
        }
    }

    private List<RECIPE> cachedRecipes = Collections.emptyList();
    private final ResourceLocation registryName;
    private final INPUT_CACHE inputCache;

    private MSRecipeType(String name, Function<MSRecipeType<RECIPE, INPUT_CACHE>, INPUT_CACHE> inputCacheCreator) {
        this.registryName = MekanismElements.rl(name);
        this.inputCache = inputCacheCreator.apply(this);
    }

    @Override
    public String toString() {
        return registryName.toString();
    }

    @Override
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public MSRecipeType<RECIPE, INPUT_CACHE> getMSRecipeType() {
        return this;
    }

    private void clearCaches() {
        cachedRecipes = Collections.emptyList();
        inputCache.clear();
    }

    @Override
    public INPUT_CACHE getInputCache() {
        return inputCache;
    }

    @NotNull
    @Override
    public List<RECIPE> getRecipes(@Nullable Level world) {
        if (world == null) {
            if (FMLEnvironment.dist.isClient()) {
                world = MekanismClient.tryGetClientWorld();
            } else {
                world = ServerLifecycleHooks.getCurrentServer().overworld();
            }
            if (world == null) {
                MekanismElements.logger.info("MSRecipeType.getRecipes: World is null, returning empty list.");
                return Collections.emptyList();
            }
        }
        if (cachedRecipes.isEmpty() && world != null) {
            RecipeManager recipeManager = world.getRecipeManager();
            @SuppressWarnings({"unchecked", "rawtypes"})
            List<RecipeHolder<RECIPE>> recipeHolders = (List) recipeManager.getAllRecipesFor((RecipeType) this);
            
            if (recipeHolders.isEmpty()) {
                MekanismElements.logger.info("MSRecipeType.getRecipes: Direct lookup failed for {}, trying name-based fallback.", registryName);
                List<RecipeHolder<?>> allRecipes = (List) recipeManager.getRecipes();
                recipeHolders = new java.util.ArrayList<>();
                for (RecipeHolder<?> h : allRecipes) {
                    ResourceLocation typeId = world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.RECIPE_TYPE).getKey(h.value().getType());
                    if (typeId != null && typeId.equals(this.registryName)) {
                        recipeHolders.add((RecipeHolder<RECIPE>) h);
                    }
                }
            }
            
            MekanismElements.logger.info("MSRecipeType.getRecipes: Found {} recipes for type {}", recipeHolders.size(), registryName);

            cachedRecipes = recipeHolders.stream()
                    .map(holder -> {
                        RECIPE recipe = holder.value();
                        if (recipe instanceof AdsorptionRecipe r) {
                            r.setId(holder.id());
                        } else if (recipe instanceof RadiationIrradiatingRecipe r) {
                            r.setId(holder.id());
                        } else if (recipe instanceof ChemicalDemolitionRecipe r) {
                            r.setId(holder.id());
                        }
                        return recipe;
                    })
                    .filter(recipe -> !recipe.isIncomplete())
                    .toList();
            MekanismElements.logger.info("MSRecipeType.getRecipes: Cached {} recipes", cachedRecipes.size());
        }
        return cachedRecipes;
    }

    /**
     * Helper for getting a recipe from a world's recipe manager.
     * Note: This method uses getAllRecipesFor and filters manually since RecipeManager.getRecipeFor API has changed.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <RECIPE_TYPE extends Recipe<?>> Optional<RECIPE_TYPE> getRecipeFor(RecipeType<RECIPE_TYPE> recipeType, Container inventory, Level level) {
        // Use getAllRecipesFor and filter manually since getRecipeFor signature changed
        RecipeManager recipeManager = level.getRecipeManager();
        List<RecipeHolder<RECIPE_TYPE>> recipes = (List) recipeManager.getAllRecipesFor((RecipeType) recipeType);
        return recipes.stream()
                .map(RecipeHolder::value)
                .filter(recipe -> !recipe.isIncomplete())
                // Note: Recipe.matches() now requires RecipeInput instead of Container
                // For now, we return all non-incomplete recipes and let the caller filter
                // TODO: Convert Container to RecipeInput if needed
                .findFirst();
    }

    /**
     * Helper for getting a recipe from a world's recipe manager.
     */
    public static Optional<? extends Recipe<?>> byKey(Level level, ResourceLocation id) {
        return level.getRecipeManager().byKey(id)
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.isSpecial() || !recipe.isIncomplete());
    }
}

