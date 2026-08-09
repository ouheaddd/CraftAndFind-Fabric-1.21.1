package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Allows category-tab frames to be themed independently. */
@Mixin(RecipeBookTabButton.class)
public abstract class RecipeBookTabButtonTextureMixin {
    @Shadow @Final private static WidgetSprites SPRITES;

    @Redirect(
            method = "<init>",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookTabButton;SPRITES:Lnet/minecraft/client/gui/components/WidgetSprites;", opcode = Opcodes.GETSTATIC),
            require = 0
    )
    private WidgetSprites craftandfind$categoryTabSprites() {
        if (Minecraft.getInstance().screen instanceof StorageWorkbenchScreen) {
            return new WidgetSprites(
                    WorkbenchTextures.CATEGORY_TAB_SPRITE,
                    WorkbenchTextures.CATEGORY_TAB_SELECTED_SPRITE
            );
        }
        return SPRITES;
    }
}
