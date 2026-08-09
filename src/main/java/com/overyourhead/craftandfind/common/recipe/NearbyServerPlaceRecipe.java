package com.overyourhead.craftandfind.common.recipe;

import com.overyourhead.craftandfind.common.storage.NearbyStorage;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;

public final class NearbyServerPlaceRecipe extends ServerPlaceRecipe<CraftingInput, CraftingRecipe> {
    private final NearbyStorage storage;

    public NearbyServerPlaceRecipe(CraftingMenu menu, NearbyStorage storage) {
        super(menu);
        this.storage = storage;
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
