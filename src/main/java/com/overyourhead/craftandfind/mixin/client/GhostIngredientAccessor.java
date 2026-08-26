package com.overyourhead.craftandfind.mixin.client;

import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GhostRecipe.GhostIngredient.class)
public interface GhostIngredientAccessor {
    @Accessor("ingredient")
    Ingredient craftandfind$getIngredient();
}
