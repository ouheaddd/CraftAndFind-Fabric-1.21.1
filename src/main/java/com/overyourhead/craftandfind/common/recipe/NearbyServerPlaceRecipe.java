package com.overyourhead.craftandfind.common.recipe;

import com.overyourhead.craftandfind.common.storage.NearbyStorage;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Recipe placer used by the Storage Workbench.
 *
 * Vanilla ServerPlaceRecipe#recipeClicked first checks the player's recipe book
 * before it is allowed to place anything. Mods such as NERB intentionally keep
 * that recipe book empty, which made Craft & Find's own recipe panel and JEI
 * transfer look successful on the client while the server silently refused to
 * populate the 3x3 grid.
 *
 * The workbench already validates the requested CraftingRecipe in
 * StorageWorkbenchMenu, so this override deliberately bypasses only that
 * recipe-book ownership gate. Vanilla's proven placement/layout code is still
 * used afterwards, while addItemToSlot extends its item source with nearby
 * storage.
 */
public final class NearbyServerPlaceRecipe extends ServerPlaceRecipe<CraftingInput, CraftingRecipe> {
    private final NearbyStorage storage;

    public NearbyServerPlaceRecipe(CraftingMenu menu, NearbyStorage storage) {
        super(menu);
        this.storage = storage;
    }

    @Override
    public void recipeClicked(ServerPlayer player, RecipeHolder<CraftingRecipe> recipe, boolean placeAll) {
        if (recipe == null) {
            return;
        }

        // Rebuild exactly the availability pool ServerPlaceRecipe expects, but
        // do not require recipe-book ownership. StorageWorkbenchMenu's
        // fillCraftSlotsStackedContents override also accounts NearbyStorage.
        this.inventory = player.getInventory();
        this.stackedContents.clear();
        this.inventory.fillStackedContents(this.stackedContents);
        this.menu.fillCraftSlotsStackedContents(this.stackedContents);

        if (!this.stackedContents.canCraft(recipe.value(), null)) {
            this.inventory.setChanged();
            return;
        }

        // The workbench returns its persistent grid before constructing this
        // placer, so handleRecipeClicked can safely use vanilla's layout logic
        // without the normal recipe-book precondition.
        this.handleRecipeClicked(recipe, placeAll);
        this.inventory.setChanged();
    }

    @Override
    public void addItemToSlot(Integer stackingIndex, int slotIndex, int maxAmount, int x, int y) {
        Slot target = this.menu.getSlot(slotIndex);
        ItemStack wanted = StackedContents.fromStackingIndex(stackingIndex);
        int remaining = maxAmount;

        while (remaining > 0) {
            int before = remaining;
            int inventoryRemaining = this.moveItemToGrid(target, wanted, remaining);

            if (inventoryRemaining >= 0) {
                remaining = inventoryRemaining;
            } else {
                remaining = storage.moveToCraftingSlot(target, wanted, remaining);
            }

            if (remaining >= before) {
                return;
            }
        }
    }
}
