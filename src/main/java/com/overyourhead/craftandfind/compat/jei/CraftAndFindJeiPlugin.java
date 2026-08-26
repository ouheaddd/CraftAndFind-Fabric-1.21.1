package com.overyourhead.craftandfind.compat.jei;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class CraftAndFindJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            CraftAndFindMod.MOD_ID,
            "jei"
    );

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(
                StorageWorkbenchScreen.class,
                new StorageWorkbenchJeiGuiHandler()
        );
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new StorageWorkbenchJeiTransferHandler(registration.getTransferHelper()),
                RecipeTypes.CRAFTING
        );
    }
}
