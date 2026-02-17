package za.co.infernos.mekanismelements.client;

import za.co.infernos.mekanismelements.api.recipes.AdsorptionRecipe;
import za.co.infernos.mekanismelements.api.recipes.RadiationIrradiatingRecipe;
import za.co.infernos.mekanismelements.common.MSLang;
import za.co.infernos.mekanismelements.common.MekanismElements;
import za.co.infernos.mekanismelements.common.recipe.MSRecipeType;
import za.co.infernos.mekanismelements.common.registries.MSBlocks;
import mekanism.api.recipes.vanilla_input.FluidRecipeInput;
import mekanism.api.recipes.vanilla_input.ItemChemicalRecipeInput;
import mekanism.client.recipe_viewer.type.SimpleRVRecipeType;
import mekanism.common.registration.impl.RecipeTypeRegistryObject;

public class MSJEIRecipeType {
    public static final za.co.infernos.mekanismelements.client.jei.MSJEIRecipeViewerRecipeType<AdsorptionRecipe> ADSORPTION_SEPARATOR = 
            new za.co.infernos.mekanismelements.client.jei.MSJEIRecipeViewerRecipeType<>(
                    MSRecipeType.ADSORPTION,
                    AdsorptionRecipe.class,
                    MSLang.DESCRIPTION_ADSORPTION_SEPARATOR,
                    MekanismElements.rl("textures/gui/jei/adsorption_separator.png"),
                    3, 3, 170, 79,
                    MSBlocks.ADSORPTION_SEPARATOR
            );
    //public static final SimpleRVRecipeType<?, ChemicalDemolitionRecipe, ?> CHEMICAL_DEMOLITION_MACHINE = new SimpleRVRecipeType<>(...);
    public static final za.co.infernos.mekanismelements.client.jei.MSJEIRecipeViewerRecipeType<RadiationIrradiatingRecipe> RADIATION_IRRADIATOR = 
            new za.co.infernos.mekanismelements.client.jei.MSJEIRecipeViewerRecipeType<>(
                    MSRecipeType.RADIATION_IRRADIATING,
                    RadiationIrradiatingRecipe.class,
                    MSLang.DESCRIPTION_RADIATION_IRRADIATOR,
                    MekanismElements.rl("textures/gui/jei/radiation_irradiator.png"),
                    3, 3, 170, 79,
                    MSBlocks.RADIATION_IRRADIATOR
            );
}

