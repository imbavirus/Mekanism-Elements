package com.fxd927.mekanismelements.common.recipe.serializer;

import com.fxd927.mekanismelements.api.recipes.ChemicalDemolitionRecipe;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import mekanism.api.SerializationConstants;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import com.mojang.serialization.MapCodec;
import mekanism.common.Mekanism;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class ChemicalDemolitionRecipeSerializer <RECIPE extends ChemicalDemolitionRecipe> implements RecipeSerializer<RECIPE> {
    private final ChemicalDemolitionRecipeSerializer.IFactory<RECIPE> factory;

    public ChemicalDemolitionRecipeSerializer(ChemicalDemolitionRecipeSerializer.IFactory<RECIPE> factory) {
        this.factory = factory;
    }

    @NotNull
    public RECIPE fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
        JsonElement itemInput = GsonHelper.isArrayNode(json, SerializationConstants.ITEM_INPUT) ? GsonHelper.getAsJsonArray(json, SerializationConstants.ITEM_INPUT) :
                GsonHelper.getAsJsonObject(json, SerializationConstants.ITEM_INPUT);
        ItemStackIngredient itemIngredient = ItemStackIngredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, itemInput)
                .getOrThrow(JsonSyntaxException::new);
        JsonElement fluidInput = GsonHelper.isArrayNode(json, SerializationConstants.CHEMICAL_INPUT) ? GsonHelper.getAsJsonArray(json, SerializationConstants.CHEMICAL_INPUT) :
                GsonHelper.getAsJsonObject(json, SerializationConstants.CHEMICAL_INPUT);
        ChemicalStackIngredient fluidIngredient = ChemicalStackIngredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, fluidInput)
                .getOrThrow(JsonSyntaxException::new);
        ItemStack firstOutput = ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(json, SerializationConstants.MAIN_OUTPUT))
                .getOrThrow(JsonSyntaxException::new);
        ItemStack secondOutput = ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(json, SerializationConstants.SECONDARY_OUTPUT))
                .getOrThrow(JsonSyntaxException::new);

        if (firstOutput.isEmpty()) {
            throw new JsonSyntaxException("Recipe output must not be empty.");
        }
        if (secondOutput.isEmpty()) {
            throw new JsonSyntaxException("Recipe output must not be empty.");
        }
        return this.factory.create(recipeId, itemIngredient, fluidIngredient, firstOutput, secondOutput);
    }

    public RECIPE fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
        // fromNetwork is deprecated, but kept for compatibility
        // Convert FriendlyByteBuf to RegistryFriendlyByteBuf for stream codec
        if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
            try {
                ItemStackIngredient itemInput = ItemStackIngredient.STREAM_CODEC.decode(registryBuffer);
                ChemicalStackIngredient fluidInput = ChemicalStackIngredient.STREAM_CODEC.decode(registryBuffer);
                ItemStack firstOutput = ItemStack.STREAM_CODEC.decode(registryBuffer);
                ItemStack secondOutput = ItemStack.STREAM_CODEC.decode(registryBuffer);
                return this.factory.create(recipeId, itemInput, fluidInput, firstOutput, secondOutput);
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
                        ChemicalStackIngredient fluidInput = ChemicalStackIngredient.STREAM_CODEC.decode(buffer);
                        ItemStack firstOutput = ItemStack.STREAM_CODEC.decode(buffer);
                        ItemStack secondOutput = ItemStack.STREAM_CODEC.decode(buffer);
                        return this.factory.create(recipeId, itemInput, fluidInput, firstOutput, secondOutput);
                    } catch (Exception e) {
                        Mekanism.logger.error("Error reading recipe from packet.", e);
                        throw e;
                    }
                }
        );
    }

    @FunctionalInterface
    public interface IFactory<RECIPE extends ChemicalDemolitionRecipe> {

        RECIPE create(ResourceLocation id, ItemStackIngredient itemInput, ChemicalStackIngredient gasInput, ItemStack firstOutput, ItemStack secondOutput);
    }
}
