package com.overyourhead.craftandfind.compat.emi;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

public final class StorageWorkbenchEmiRecipeHandler implements StandardRecipeHandler<CraftingMenu> {
    private static final int RESULT_SLOT = 0;
    private static final int FIRST_CRAFTING_SLOT = 1;
    private static final int CRAFTING_SLOT_COUNT = 9;

    @Override
    public List<Slot> getInputSources(CraftingMenu handler) {
        return new ArrayList<>(handler.slots.subList(FIRST_CRAFTING_SLOT, handler.slots.size()));
    }

    @Override
    public List<Slot> getCraftingSlots(CraftingMenu handler) {
        return new ArrayList<>(handler.slots.subList(
                FIRST_CRAFTING_SLOT,
                FIRST_CRAFTING_SLOT + CRAFTING_SLOT_COUNT
        ));
    }

    @Override
    public Slot getOutputSlot(CraftingMenu handler) {
        return handler.getSlot(RESULT_SLOT);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        CraftingMenu menu = minecraft.player.containerMenu instanceof CraftingMenu craftingMenu
                ? craftingMenu
                : null;

        return menu != null
                && ClientStorageState.isActive(menu.containerId)
                && recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING;
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<CraftingMenu> screen) {
        CraftingMenu menu = screen.getMenu();
        if (!ClientStorageState.isActive(menu.containerId)) {
            return StandardRecipeHandler.super.getInventory(screen);
        }

        List<EmiStack> available = new ArrayList<>();

        // Player inventory plus anything already in the persistent 3x3 grid.
        for (Slot slot : getInputSources(menu)) {
            if (!slot.getItem().isEmpty()) {
                available.add(EmiStack.of(slot.getItem()));
            }
        }

        // The snapshot comes from NearbyStorage, so all Craft & Find exclusions
        // (including the Sculk Sentinel Chest) automatically apply here too.
        for (StorageItemEntry entry : ClientStorageState.entries(menu.containerId)) {
            if (!entry.stack().isEmpty() && entry.count() > 0) {
                available.add(EmiStack.of(entry.stack(), entry.count()));
            }
        }

        return new EmiPlayerInventory(available);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
        CraftingMenu menu = context.getScreenHandler();
        if (!ClientStorageState.isActive(menu.containerId)) {
            return StandardRecipeHandler.super.craft(recipe, context);
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.gameMode == null || recipe.getId() == null) {
            return false;
        }

        RecipeHolder<?> vanillaRecipe = minecraft.level.getRecipeManager()
                .byKey(recipe.getId())
                .orElse(null);

        if (vanillaRecipe == null || !(vanillaRecipe.value() instanceof CraftingRecipe)) {
            return StandardRecipeHandler.super.craft(recipe, context);
        }

        // Return to the handled screen, then send the normal recipe-book packet.
        // StorageWorkbenchMenu handles it and pulls from NearbyStorage server-side.
        minecraft.setScreen(context.getScreen());
        minecraft.gameMode.handlePlaceRecipe(
                menu.containerId,
                vanillaRecipe,
                context.getAmount() > 1
        );
        return true;
    }
}
