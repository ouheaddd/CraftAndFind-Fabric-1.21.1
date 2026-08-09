package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Routes recipe-cell states to Craft & Find GUI sprites only in our screen. */
@Mixin(RecipeButton.class)
public abstract class RecipeButtonTextureMixin {
    @Shadow @Final private static ResourceLocation SLOT_CRAFTABLE_SPRITE;
    @Shadow @Final private static ResourceLocation SLOT_UNCRAFTABLE_SPRITE;
    @Shadow @Final private static ResourceLocation SLOT_MANY_CRAFTABLE_SPRITE;
    @Shadow @Final private static ResourceLocation SLOT_MANY_UNCRAFTABLE_SPRITE;

    @Redirect(
            method = "renderWidget",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeButton;SLOT_CRAFTABLE_SPRITE:Lnet/minecraft/resources/ResourceLocation;", opcode = Opcodes.GETSTATIC),
            require = 0
    )
    private ResourceLocation craftandfind$craftableSprite() {
        return ownScreen() ? WorkbenchTextures.RECIPE_SLOT_AVAILABLE_SPRITE : SLOT_CRAFTABLE_SPRITE;
    }

    @Redirect(
            method = "renderWidget",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeButton;SLOT_UNCRAFTABLE_SPRITE:Lnet/minecraft/resources/ResourceLocation;", opcode = Opcodes.GETSTATIC),
            require = 0
    )
    private ResourceLocation craftandfind$uncraftableSprite() {
        return ownScreen() ? WorkbenchTextures.RECIPE_SLOT_UNAVAILABLE_SPRITE : SLOT_UNCRAFTABLE_SPRITE;
    }

    @Redirect(
            method = "renderWidget",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeButton;SLOT_MANY_CRAFTABLE_SPRITE:Lnet/minecraft/resources/ResourceLocation;", opcode = Opcodes.GETSTATIC),
            require = 0
    )
    private ResourceLocation craftandfind$manyCraftableSprite() {
        return ownScreen() ? WorkbenchTextures.RECIPE_SLOT_MANY_AVAILABLE_SPRITE : SLOT_MANY_CRAFTABLE_SPRITE;
    }

    @Redirect(
            method = "renderWidget",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeButton;SLOT_MANY_UNCRAFTABLE_SPRITE:Lnet/minecraft/resources/ResourceLocation;", opcode = Opcodes.GETSTATIC),
            require = 0
    )
    private ResourceLocation craftandfind$manyUncraftableSprite() {
        return ownScreen() ? WorkbenchTextures.RECIPE_SLOT_MANY_UNAVAILABLE_SPRITE : SLOT_MANY_UNCRAFTABLE_SPRITE;
    }

    private static boolean ownScreen() {
        return Minecraft.getInstance().screen instanceof StorageWorkbenchScreen;
    }
}
