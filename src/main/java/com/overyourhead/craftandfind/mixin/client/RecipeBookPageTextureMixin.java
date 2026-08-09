package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Allows next/previous page buttons to use Craft & Find-owned sprites. */
@Mixin(RecipeBookPage.class)
public abstract class RecipeBookPageTextureMixin {
    @Shadow @Final private static WidgetSprites PAGE_FORWARD_SPRITES;
    @Shadow @Final private static WidgetSprites PAGE_BACKWARD_SPRITES;

    @Redirect(
            method = "init",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookPage;PAGE_FORWARD_SPRITES:Lnet/minecraft/client/gui/components/WidgetSprites;", opcode = Opcodes.GETSTATIC),
            require = 0
    )
    private WidgetSprites craftandfind$forwardSprites() {
        if (Minecraft.getInstance().screen instanceof StorageWorkbenchScreen) {
            return new WidgetSprites(WorkbenchTextures.PAGE_NEXT_SPRITE, WorkbenchTextures.PAGE_NEXT_HOVERED_SPRITE);
        }
        return PAGE_FORWARD_SPRITES;
    }

    @Redirect(
            method = "init",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookPage;PAGE_BACKWARD_SPRITES:Lnet/minecraft/client/gui/components/WidgetSprites;", opcode = Opcodes.GETSTATIC),
            require = 0
    )
    private WidgetSprites craftandfind$backwardSprites() {
        if (Minecraft.getInstance().screen instanceof StorageWorkbenchScreen) {
            return new WidgetSprites(WorkbenchTextures.PAGE_PREVIOUS_SPRITE, WorkbenchTextures.PAGE_PREVIOUS_HOVERED_SPRITE);
        }
        return PAGE_BACKWARD_SPRITES;
    }
}
