package com.overyourhead.craftandfind.common.network;

import com.overyourhead.craftandfind.common.menu.StorageWorkbenchMenu;
import com.overyourhead.craftandfind.common.network.payload.GhostRecipePayload;
import com.overyourhead.craftandfind.common.network.payload.HighlightPositionsPayload;
import com.overyourhead.craftandfind.common.network.payload.HighlightRequestPayload;
import com.overyourhead.craftandfind.common.network.payload.StorageSnapshotPayload;
import com.overyourhead.craftandfind.config.CraftAndFindServerConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class CraftAndFindNetwork {
    private CraftAndFindNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(
                HighlightRequestPayload.TYPE,
                HighlightRequestPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                StorageSnapshotPayload.TYPE,
                StorageSnapshotPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                HighlightPositionsPayload.TYPE,
                HighlightPositionsPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                GhostRecipePayload.TYPE,
                GhostRecipePayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                HighlightRequestPayload.TYPE,
                CraftAndFindNetwork::handleHighlightRequest
        );
    }

    private static void handleHighlightRequest(
            HighlightRequestPayload payload,
            ServerPlayNetworking.Context context
    ) {
        ServerPlayer player = context.player();
        if (!(player.containerMenu instanceof StorageWorkbenchMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId() || payload.stack().isEmpty()) {
            return;
        }

        var allTargets = menu.refreshStorage().highlightTargets(payload.stack(), player.position());
        int targetLimit = Math.min(
                allTargets.size(),
                CraftAndFindServerConfig.maxHighlightedContainers()
        );
        var visibleTargets = allTargets.subList(0, targetLimit);

        ServerPlayNetworking.send(
                player,
                new HighlightPositionsPayload(payload.stack(), visibleTargets)
        );
    }
}
