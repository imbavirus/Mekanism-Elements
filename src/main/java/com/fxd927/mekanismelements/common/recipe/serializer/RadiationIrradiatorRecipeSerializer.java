package com.fxd927.mekanismelements.common.recipe.serializer;

import com.fxd927.mekanismelements.api.recipes.RadiationIrradiatingRecipe;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.Mekanism;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class RadiationIrradiatorRecipeSerializer<RECIPE extends RadiationIrradiatingRecipe> implements RecipeSerializer<RECIPE> {
    private final RadiationIrradiatorRecipeSerializer.IFactory<RECIPE> factory;

    public RadiationIrradiatorRecipeSerializer(RadiationIrradiatorRecipeSerializer.IFactory<RECIPE> factory) {
        this.factory = factory;
    }

    @NotNull
    public RECIPE fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
        JsonElement itemInput = GsonHelper.isArrayNode(json, SerializationConstants.ITEM_INPUT) ? GsonHelper.getAsJsonArray(json, SerializationConstants.ITEM_INPUT) :
                GsonHelper.getAsJsonObject(json, SerializationConstants.ITEM_INPUT);
        ItemStackIngredient itemIngredient = ItemStackIngredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, itemInput)
                .getOrThrow(JsonSyntaxException::new);
        JsonElement gasInput = GsonHelper.isArrayNode(json, SerializationConstants.CHEMICAL_INPUT) ? GsonHelper.getAsJsonArray(json, SerializationConstants.CHEMICAL_INPUT) :
                GsonHelper.getAsJsonObject(json, SerializationConstants.CHEMICAL_INPUT);
        ChemicalStackIngredient gasIngredient = ChemicalStackIngredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, gasInput)
                .getOrThrow(JsonSyntaxException::new);
        ChemicalStack output = ChemicalStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(json, SerializationConstants.OUTPUT))
                .getOrThrow(JsonSyntaxException::new);
        if (output.isEmpty()) {
            throw new JsonSyntaxException("Recipe output must not be empty.");
        }
        return this.factory.create(recipeId, itemIngredient, gasIngredient, output);
    }

    public RECIPE fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
        // fromNetwork is deprecated, but kept for compatibility
        // Convert FriendlyByteBuf to RegistryFriendlyByteBuf for stream codec
        if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
            try {
                ItemStackIngredient itemInput = ItemStackIngredient.STREAM_CODEC.decode(registryBuffer);
                ChemicalStackIngredient gasInput = ChemicalStackIngredient.STREAM_CODEC.decode(registryBuffer);
                ChemicalStack output = ChemicalStack.STREAM_CODEC.decode(registryBuffer);
                return this.factory.create(recipeId, itemInput, gasInput, output);
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
    public com.mojang.serialization.MapCodec<RECIPE> codec() {
        // Return a dummy MapCodec - JSON parsing is handled by fromJson
        return com.mojang.serialization.MapCodec.unit(null);
    }

    @Override
    public net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, RECIPE> streamCodec() {
        return net.minecraft.network.codec.StreamCodec.of(
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
                        ChemicalStackIngredient gasInput = ChemicalStackIngredient.STREAM_CODEC.decode(buffer);
                        ChemicalStack output = ChemicalStack.STREAM_CODEC.decode(buffer);
                        return this.factory.create(recipeId, itemInput, gasInput, output);
                    } catch (Exception e) {
                        Mekanism.logger.error("Error reading recipe from packet.", e);
                        throw e;
                    }
                }
        );
    }

    @FunctionalInterface
    public interface IFactory<RECIPE extends RadiationIrradiatingRecipe> {

        RECIPE create(ResourceLocation id, ItemStackIngredient itemInput, ChemicalStackIngredient gasInput, ChemicalStack output);
    }
}
