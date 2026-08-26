package com.overyourhead.craftandfind.compat.jei;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.client.RecipeViewerHooks;
import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import com.mojang.blaze3d.platform.InputConstants;
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
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        RecipeViewerHooks.setJeiKeyboardLookup((stack, keyCode, scanCode) -> {
            InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
            RecipeIngredientRole role;

            if (jeiRuntime.getKeyMappings().getShowRecipe().isActiveAndMatches(key)) {
                role = RecipeIngredientRole.OUTPUT;
            } else if (jeiRuntime.getKeyMappings().getShowUses().isActiveAndMatches(key)) {
                role = RecipeIngredientRole.INPUT;
            } else {
                return false;
            }

            var focus = jeiRuntime.getJeiHelpers()
                    .getFocusFactory()
                    .createFocus(role, VanillaTypes.ITEM_STACK, stack);
            jeiRuntime.getRecipesGui().show(focus);
            return true;
        });
    }

    @Override
    public void onRuntimeUnavailable() {
        RecipeViewerHooks.setJeiKeyboardLookup(null);
    }

}
