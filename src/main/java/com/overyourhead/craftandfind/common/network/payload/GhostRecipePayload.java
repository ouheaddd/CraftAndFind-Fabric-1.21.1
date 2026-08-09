package com.overyourhead.craftandfind.common.network.payload;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Tells the client which unavailable recipe should be drawn as ghost items. */
public record GhostRecipePayload(int containerId, ResourceLocation recipeId) implements CustomPacketPayload {
    public static final Type<GhostRecipePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "ghost_recipe")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, GhostRecipePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.containerId());
                buffer.writeUtf(payload.recipeId().toString(), 32767);
            },
            buffer -> new GhostRecipePayload(
                    buffer.readVarInt(),
                    ResourceLocation.parse(buffer.readUtf(32767))
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
