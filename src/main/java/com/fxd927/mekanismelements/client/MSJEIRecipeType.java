package com.fxd927.mekanismelements.client;

import com.fxd927.mekanismelements.api.recipes.AdsorptionRecipe;
import com.fxd927.mekanismelements.api.recipes.RadiationIrradiatingRecipe;
import com.fxd927.mekanismelements.common.MSLang;
import com.fxd927.mekanismelements.common.MekanismElements;
import com.fxd927.mekanismelements.common.recipe.MSRecipeType;
import com.fxd927.mekanismelements.common.registries.MSBlocks;
import mekanism.api.recipes.vanilla_input.FluidRecipeInput;
import mekanism.api.recipes.vanilla_input.ItemChemicalRecipeInput;
import mekanism.client.recipe_viewer.type.SimpleRVRecipeType;
import mekanism.common.registration.impl.RecipeTypeRegistryObject;

public class MSJEIRecipeType {
    // SimpleRVRecipeType constructor: (RecipeTypeRegistryObject, Class, IHasTranslationKey, ResourceLocation, int, int, int, int, ItemLike...)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final SimpleRVRecipeType<FluidRecipeInput, AdsorptionRecipe, com.fxd927.mekanismelements.common.recipe.lookup.cache.MSInputRecipeCache.ItemFluid<AdsorptionRecipe>> ADSORPTION_SEPARATOR = 
            new SimpleRVRecipeType(
                    (RecipeTypeRegistryObject) (Object) MSRecipeType.ADSORPTION,
                    AdsorptionRecipe.class,
                    MSLang.DESCRIPTION_ADSORPTION_SEPARATOR,
                    MekanismElements.rl("textures/gui/jei/adsorption_separator.png"),
                    3, 3, 170, 79,
                    MSBlocks.ADSORPTION_SEPARATOR
            );
    //public static final SimpleRVRecipeType<?, ChemicalDemolitionRecipe, ?> CHEMICAL_DEMOLITION_MACHINE = new SimpleRVRecipeType<>(...);
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final SimpleRVRecipeType<ItemChemicalRecipeInput, RadiationIrradiatingRecipe, com.fxd927.mekanismelements.common.recipe.lookup.cache.MSInputRecipeCache.ItemChemical<RadiationIrradiatingRecipe>> RADIATION_IRRADIATOR = 
            new SimpleRVRecipeType(
                    (RecipeTypeRegistryObject) (Object) MSRecipeType.RADIATION_IRRADIATING,
                    RadiationIrradiatingRecipe.class,
                    MSLang.DESCRIPTION_RADIATION_IRRADIATOR,
                    MekanismElements.rl("textures/gui/jei/radiation_irradiator.png"),
                    3, 3, 170, 79,
                    MSBlocks.RADIATION_IRRADIATOR
            );
}
