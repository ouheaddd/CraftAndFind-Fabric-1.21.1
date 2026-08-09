package com.overyourhead.craftandfind.client;

import com.overyourhead.craftandfind.client.network.ClientPayloadRegistration;
import com.overyourhead.craftandfind.client.render.StorageHighlightRenderer;
import com.overyourhead.craftandfind.config.CraftAndFindClientConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class CraftAndFindClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CraftAndFindClientConfig.load();
        ClientPayloadRegistration.register();
        WorldRenderEvents.AFTER_TRANSLUCENT.register(StorageHighlightRenderer::render);
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) ->
                StorageHighlightRenderer.renderFixedGuiItem(guiGraphics));
    }
}
