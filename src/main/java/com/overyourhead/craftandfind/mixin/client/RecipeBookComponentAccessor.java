package com.overyourhead.craftandfind.mixin.client;

import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
    @Accessor("ghostRecipe")
    GhostRecipe craftandfind$getGhostRecipe();

    @Invoker("setVisible")
    void craftandfind$setVisible(boolean visible);
}
