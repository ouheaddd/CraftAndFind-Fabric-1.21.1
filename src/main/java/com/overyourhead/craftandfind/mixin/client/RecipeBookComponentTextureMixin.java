package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps vanilla recipe-book behavior but gives Craft & Find its own panel
 * texture namespace. Other crafting screens continue using the vanilla atlas.
 */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentTextureMixin {
    private static final ResourceLocation VANILLA_RECIPE_BOOK = ResourceLocation.withDefaultNamespace(
            "textures/gui/recipe_book.png"
    );

    @Redirect(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;RECIPE_BOOK_LOCATION:Lnet/minecraft/resources/ResourceLocation;",
                    opcode = Opcodes.GETSTATIC
            )
    )
    private ResourceLocation craftandfind$useOwnRecipePanelTexture() {
        return Minecraft.getInstance().screen instanceof StorageWorkbenchScreen
                ? WorkbenchTextures.RECIPE_PANEL
                : VANILLA_RECIPE_BOOK;
    }
}
