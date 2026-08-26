package com.overyourhead.craftandfind.compat.jei;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class StorageWorkbenchJeiGuiHandler implements IGuiContainerHandler<StorageWorkbenchScreen> {
    @Override
    public List<Rect2i> getGuiExtraAreas(StorageWorkbenchScreen screen) {
        if (!screen.isStoragePanelOpen()) {
            return List.of();
        }
        return List.of(screen.storagePanelArea());
    }

    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
            IClickableIngredientFactory factory,
            StorageWorkbenchScreen screen,
            double mouseX,
            double mouseY
    ) {
        ItemStack stack = screen.storageItemUnderMouse(mouseX, mouseY);
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        return factory.createBuilder(stack)
                .buildWithArea((int) mouseX, (int) mouseY, 1, 1);
    }
}
