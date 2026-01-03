package com.fxd927.mekanismelements.common.recipe.serializer;

import com.fxd927.mekanismelements.api.recipes.AdsorptionRecipe;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.Mekanism;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class AdsorptionRecipeSerializer <RECIPE extends AdsorptionRecipe> implements RecipeSerializer<RECIPE> {
    private final AdsorptionRecipeSerializer.IFactory<RECIPE> factory;

    public AdsorptionRecipeSerializer(AdsorptionRecipeSerializer.IFactory<RECIPE> factory) {
        this.factory = factory;
    }

    @NotNull
    public RECIPE fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
        JsonElement itemInput = GsonHelper.isArrayNode(json, SerializationConstants.ITEM_INPUT) ? GsonHelper.getAsJsonArray(json, SerializationConstants.ITEM_INPUT) :
                GsonHelper.getAsJsonObject(json, SerializationConstants.ITEM_INPUT);
        ItemStackIngredient itemIngredient = ItemStackIngredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, itemInput)
                .getOrThrow(JsonSyntaxException::new);
        JsonElement fluidInput = GsonHelper.isArrayNode(json, SerializationConstants.FLUID_INPUT) ? GsonHelper.getAsJsonArray(json, SerializationConstants.FLUID_INPUT) :
                GsonHelper.getAsJsonObject(json, SerializationConstants.FLUID_INPUT);
        FluidStackIngredient fluidIngredient = FluidStackIngredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, fluidInput)
                .getOrThrow(JsonSyntaxException::new);
        ChemicalStack output = ChemicalStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(json, SerializationConstants.OUTPUT))
                .getOrThrow(JsonSyntaxException::new);
        if (output.isEmpty()) {
            throw new JsonSyntaxException("Recipe output must not be empty.");
        }
        return this.factory.create(recipeId, itemIngredient, fluidIngredient, output);
    }

    public RECIPE fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
        // fromNetwork is deprecated, but kept for compatibility
        // Convert FriendlyByteBuf to RegistryFriendlyByteBuf for stream codec
        if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
            try {
                ItemStackIngredient itemInput = ItemStackIngredient.STREAM_CODEC.decode(registryBuffer);
                FluidStackIngredient fluidInput = FluidStackIngredient.STREAM_CODEC.decode(registryBuffer);
                ChemicalStack output = ChemicalStack.STREAM_CODEC.decode(registryBuffer);
                return this.factory.create(recipeId, itemInput, fluidInput, output);
            } catch (Exception e) {
                Mekanism.logger.error("Error reading itemstack chemical to chemical recipe from packet.", e);
                throw e;
            }
        }
        throw new UnsupportedOperationException("RegistryFriendlyByteBuf required");
    }

    public void toNetwork(@NotNull RegistryFriendlyByteBuf buffer, @NotNull RECIPE recipe) {
        try {
            recipe.write(buffer);
        } catch (Exception e) {
            Mekanism.logger.error("Error writing itemstack chemical to chemical recipe to packet.", e);
            throw e;
        }
    }

    @Override
    public MapCodec<RECIPE> codec() {
        // Return a dummy MapCodec - JSON parsing is handled by fromJson
        return MapCodec.unit(null);
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, RECIPE> streamCodec() {
        return StreamCodec.of(
                (buffer, recipe) -> {
                    try {
                        recipe.write(buffer);
                    } catch (Exception e) {
                        Mekanism.logger.error("Error writing recipe to packet.", e);
                        throw e;
                    }
                },
                buffer -> {
                    try {
                        ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buffer);
                        ItemStackIngredient itemInput = ItemStackIngredient.STREAM_CODEC.decode(buffer);
                        FluidStackIngredient fluidInput = FluidStackIngredient.STREAM_CODEC.decode(buffer);
                        ChemicalStack output = ChemicalStack.STREAM_CODEC.decode(buffer);
                        return this.factory.create(recipeId, itemInput, fluidInput, output);
                    } catch (Exception e) {
                        Mekanism.logger.error("Error reading recipe from packet.", e);
                        throw e;
                    }
                }
        );
    }

    @FunctionalInterface
    public interface IFactory<RECIPE extends AdsorptionRecipe> {

        RECIPE create(ResourceLocation id, ItemStackIngredient itemInput, FluidStackIngredient fluidInput, ChemicalStack output);
    }
}
