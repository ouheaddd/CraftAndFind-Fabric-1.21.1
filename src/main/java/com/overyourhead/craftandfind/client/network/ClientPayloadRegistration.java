package com.overyourhead.craftandfind.client.network;

import com.overyourhead.craftandfind.common.network.payload.GhostRecipePayload;
import com.overyourhead.craftandfind.common.network.payload.HighlightPositionsPayload;
import com.overyourhead.craftandfind.common.network.payload.StorageSnapshotPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientPayloadRegistration {
    private ClientPayloadRegistration() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                StorageSnapshotPayload.TYPE,
                (payload, context) -> ClientPayloadHandler.handleStorageSnapshot(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(
                HighlightPositionsPayload.TYPE,
                (payload, context) -> ClientPayloadHandler.handleHighlightPositions(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(
                GhostRecipePayload.TYPE,
                (payload, context) -> ClientPayloadHandler.handleGhostRecipe(payload)
        );
    }
}
