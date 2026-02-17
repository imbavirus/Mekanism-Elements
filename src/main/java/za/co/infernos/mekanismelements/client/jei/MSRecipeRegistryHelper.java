package za.co.infernos.mekanismelements.client.jei;

import za.co.infernos.mekanismelements.common.recipe.IMSRecipeTypeProvider;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.List;

public class MSRecipeRegistryHelper {
    private MSRecipeRegistryHelper() {
    }

    public static <RECIPE extends MekanismRecipe<?>> void register(IRecipeRegistration registry, IRecipeViewerRecipeType<RECIPE> recipeType,
                                                                IMSRecipeTypeProvider<RECIPE, ?> type) {
        System.out.println("DEBUG: MSRecipeRegistryHelper.register ENTERED for " + recipeType);
        ClientLevel world = getWorld();
        za.co.infernos.mekanismelements.common.MekanismElements.logger.info("MSRecipeRegistryHelper: Attempting to register recipes for {}", recipeType);
        if (world != null) {
            List<RECIPE> recipes = type.getRecipes(world);
            za.co.infernos.mekanismelements.common.MekanismElements.logger.info("MSRecipeRegistryHelper: Retrieved {} recipes for {}", recipes.size(), recipeType);
            if (!recipes.isEmpty()) {
                register(registry, recipeType, recipes);
                za.co.infernos.mekanismelements.common.MekanismElements.logger.info("MSRecipeRegistryHelper: Successfully registered {} recipes for {}", recipes.size(), recipeType);
            } else {
                za.co.infernos.mekanismelements.common.MekanismElements.logger.warn("MSRecipeRegistryHelper: No recipes found for {}", recipeType);
            }
        } else {
             za.co.infernos.mekanismelements.common.MekanismElements.logger.error("MSRecipeRegistryHelper: World is null! Cannot retrieve recipes for {}", recipeType);
        }
    }

    public static <RECIPE> void register(IRecipeRegistration registry, IRecipeViewerRecipeType<RECIPE> recipeType, List<RECIPE> recipes) {
        if (!recipes.isEmpty()) {
            registry.addRecipes(MekanismJEI.recipeType(recipeType), recipes);
        }
    }

    private static ClientLevel getWorld() {
        return Minecraft.getInstance().level;
    }
}

