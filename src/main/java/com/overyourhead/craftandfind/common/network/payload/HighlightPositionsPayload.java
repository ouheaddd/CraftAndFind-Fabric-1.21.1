package com.overyourhead.craftandfind.common.network.payload;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.common.storage.StorageHighlightTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record HighlightPositionsPayload(
        ItemStack stack,
        List<StorageHighlightTarget> targets
) implements CustomPacketPayload {
    private static final int MAX_TARGETS = 512;

    public static final Type<HighlightPositionsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "highlight_positions")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HighlightPositionsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.stack().copyWithCount(1));

                int size = Math.min(payload.targets().size(), MAX_TARGETS);
                buffer.writeVarInt(size);
                for (int index = 0; index < size; index++) {
                    StorageHighlightTarget target = payload.targets().get(index);
                    buffer.writeBlockPos(target.minPos());
                    buffer.writeBlockPos(target.maxPos());
                    buffer.writeVarInt(target.count());
                }
            },
            buffer -> {
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer).copyWithCount(1);
                int size = readBoundedSize(buffer, MAX_TARGETS, "highlight targets");
                List<StorageHighlightTarget> targets = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    BlockPos minPos = buffer.readBlockPos();
                    BlockPos maxPos = buffer.readBlockPos();
                    int count = buffer.readVarInt();
                    if (count < 0) {
                        throw new IllegalArgumentException("Invalid highlight item count: " + count);
                    }
                    targets.add(new StorageHighlightTarget(minPos, maxPos, count));
                }
                return new HighlightPositionsPayload(stack, targets);
            }
    );

    public HighlightPositionsPayload {
        stack = stack.copyWithCount(1);

        Map<TargetKey, StorageHighlightTarget> unique = new LinkedHashMap<>();
        for (StorageHighlightTarget target : targets) {
            if (target.count() <= 0 || unique.size() >= MAX_TARGETS) {
                continue;
            }
            unique.putIfAbsent(new TargetKey(target.minPos(), target.maxPos()), target);
        }
        targets = List.copyOf(unique.values());
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

    private record TargetKey(BlockPos minPos, BlockPos maxPos) {
    }
}
