package com.overyourhead.craftandfind.client.network;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.render.StorageHighlightRenderer;
import com.overyourhead.craftandfind.common.network.payload.GhostRecipePayload;
import com.overyourhead.craftandfind.common.network.payload.HighlightPositionsPayload;
import com.overyourhead.craftandfind.common.network.payload.StorageSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.world.inventory.CraftingMenu;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handleStorageSnapshot(StorageSnapshotPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.containerMenu.containerId != payload.containerId()) {
            return;
        }

        ClientStorageState.update(payload.containerId(), payload.entries());
        minecraft.player.getInventory().setChanged();

        if (minecraft.screen instanceof StorageWorkbenchScreen storageScreen) {
            storageScreen.onStorageUpdated();
            return;
        }

        if (minecraft.screen instanceof CraftingScreen craftingScreen
                && minecraft.player.containerMenu instanceof CraftingMenu craftingMenu) {
            minecraft.setScreen(new StorageWorkbenchScreen(
                    craftingMenu,
                    minecraft.player.getInventory(),
                    craftingScreen.getTitle()
            ));
        }
    }

    public static void handleHighlightPositions(HighlightPositionsPayload payload) {
        StorageHighlightRenderer.show(payload.stack(), payload.targets());
    }

    public static void handleGhostRecipe(GhostRecipePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.containerMenu.containerId != payload.containerId()) {
            return;
        }
        if (minecraft.screen instanceof StorageWorkbenchScreen storageScreen) {
            storageScreen.showGhostRecipe(payload.recipeId());
        }
    }
}
