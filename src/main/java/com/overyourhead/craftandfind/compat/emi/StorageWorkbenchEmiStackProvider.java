package com.overyourhead.craftandfind.compat.emi;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.world.item.ItemStack;

public final class StorageWorkbenchEmiStackProvider implements EmiStackProvider<StorageWorkbenchScreen> {
    @Override
    public EmiStackInteraction getStackAt(StorageWorkbenchScreen screen, int x, int y) {
        ItemStack stack = screen.storageItemUnderMouse(x, y);
        if (stack.isEmpty()) {
            stack = screen.recipeItemUnderMouse(x, y);
        }
        if (stack.isEmpty()) {
            return EmiStackInteraction.EMPTY;
        }

        // false = keyboard lookup target only. EMI will still see this stack for
        // R/U, but normal mouse clicks remain owned by Craft & Find.
        return new EmiStackInteraction(EmiStack.of(stack), null, false);
    }
}
