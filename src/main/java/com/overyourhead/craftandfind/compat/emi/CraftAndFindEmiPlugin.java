package com.overyourhead.craftandfind.compat.emi;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public final class CraftAndFindEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addStackProvider(StorageWorkbenchScreen.class, new StorageWorkbenchEmiStackProvider());

        registry.addExclusionArea(StorageWorkbenchScreen.class, (screen, consumer) -> {
            if (!screen.isStoragePanelOpen()) {
                return;
            }

            Rect2i area = screen.storagePanelArea();
            consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
        });

        StorageWorkbenchEmiRecipeHandler handler = new StorageWorkbenchEmiRecipeHandler();
        registry.addRecipeHandler(MenuType.CRAFTING, handler);

        // StorageWorkbench intentionally uses vanilla MenuType.CRAFTING. EMI also
        // has a vanilla handler for that menu type, so put our narrow handler first.
        // It only claims recipes while a Craft & Find storage snapshot is active.
        prioritizeCraftingHandler(handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void prioritizeCraftingHandler(EmiRecipeHandler<CraftingMenu> handler) {
        try {
            Class<?> fillerClass = Class.forName("dev.emi.emi.registry.EmiRecipeFiller");
            Field handlersField = fillerClass.getField("handlers");
            Object rawMap = handlersField.get(null);
            if (!(rawMap instanceof Map map)) {
                return;
            }

            Object rawList = map.get(MenuType.CRAFTING);
            if (!(rawList instanceof List list)) {
                return;
            }

            if (list.remove(handler)) {
                list.add(0, handler);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // If EMI changes this internal list in a future build, never crash
            // Craft & Find. Stack lookup still works and recipe fill falls back.
        }
    }
}
