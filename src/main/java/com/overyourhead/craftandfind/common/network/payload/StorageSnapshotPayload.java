package com.overyourhead.craftandfind.common.network.payload;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record StorageSnapshotPayload(int containerId, List<StorageItemEntry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 2048;

    public static final Type<StorageSnapshotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "storage_snapshot")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.containerId());
                int size = Math.min(payload.entries().size(), MAX_ENTRIES);
                buffer.writeVarInt(size);
                for (int index = 0; index < size; index++) {
                    StorageItemEntry entry = payload.entries().get(index);
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, entry.stack());
                    buffer.writeVarInt(entry.count());
                }
            },
            buffer -> {
                int containerId = buffer.readVarInt();
                int size = readBoundedSize(buffer, MAX_ENTRIES, "storage entries");
                List<StorageItemEntry> entries = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                    int count = buffer.readVarInt();
                    if (!stack.isEmpty() && count > 0) {
                        entries.add(new StorageItemEntry(stack, count));
                    }
                }
                return new StorageSnapshotPayload(containerId, entries);
            }
    );

    public StorageSnapshotPayload {
        entries = List.copyOf(entries);
    }

    private static int readBoundedSize(RegistryFriendlyByteBuf buffer, int maximum, String valueName) {
        int size = buffer.readVarInt();
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException("Invalid " + valueName + " count: " + size);
        }
        return size;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
