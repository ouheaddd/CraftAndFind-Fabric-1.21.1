package com.overyourhead.craftandfind.common.network.payload;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Requests storage-aware placement of a crafting recipe in the currently open
 * Storage Workbench. Used by optional recipe-viewer integrations such as JEI.
 */
public record RecipeTransferPayload(
        int containerId,
        ResourceLocation recipeId,
        boolean placeAll
) implements CustomPacketPayload {
    public static final Type<RecipeTransferPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "recipe_transfer")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeTransferPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.containerId());
                buffer.writeUtf(payload.recipeId().toString(), 32767);
                buffer.writeBoolean(payload.placeAll());
            },
            buffer -> new RecipeTransferPayload(
                    buffer.readVarInt(),
                    ResourceLocation.parse(buffer.readUtf(32767)),
                    buffer.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
