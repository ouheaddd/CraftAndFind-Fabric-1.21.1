package com.overyourhead.craftandfind.common.network.payload;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record HighlightRequestPayload(int containerId, ItemStack stack) implements CustomPacketPayload {
    public static final Type<HighlightRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "highlight_request")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HighlightRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.containerId());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.stack().copyWithCount(1));
            },
            buffer -> new HighlightRequestPayload(
                    buffer.readVarInt(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer).copyWithCount(1)
            )
    );

    public HighlightRequestPayload {
        stack = stack.copyWithCount(1);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
