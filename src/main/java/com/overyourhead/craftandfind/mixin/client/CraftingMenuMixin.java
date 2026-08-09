package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(method = "fillCraftSlotsStackedContents", at = @At("TAIL"))
    private void craftandfind$includeNearbyStorage(StackedContents contents, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getInstance();
        CraftingMenu self = (CraftingMenu) (Object) this;

        if (minecraft.player == null || minecraft.player.containerMenu != self) {
            return;
        }
        if (!ClientStorageState.isActive(self.containerId)) {
            return;
        }

        for (StorageItemEntry entry : ClientStorageState.entries(self.containerId)) {
            accountFullCount(contents, entry);
        }
    }

    private static void accountFullCount(StackedContents contents, StorageItemEntry entry) {
        int remaining = entry.count();
        int stackLimit = Math.max(1, entry.stack().getMaxStackSize());

        // The snapshot stack itself has count 1. accountStack(stack, maxCount)
        // caps that stack; it does not replace its count. Feed real-sized chunks
        // so the recipe book sees all 64/128/384/etc. stored ingredients.
        while (remaining > 0) {
            int amount = Math.min(remaining, stackLimit);
            contents.accountStack(entry.stack().copyWithCount(amount));
            remaining -= amount;
        }
    }
}
