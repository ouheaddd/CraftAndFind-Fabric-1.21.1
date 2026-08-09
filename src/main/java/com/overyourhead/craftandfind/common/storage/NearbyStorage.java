package com.overyourhead.craftandfind.common.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cached view of loaded container positions inside the workbench radius.
 * Items are never stored here: every operation resolves the current block
 * entity and reads or moves stacks in the original container.
 */
public final class NearbyStorage {
    private final Level level;
    private final BlockPos origin;
    private final List<ContainerReference> containers;

    private NearbyStorage(Level level, BlockPos origin, List<ContainerReference> containers) {
        this.level = level;
        this.origin = origin.immutable();
        this.containers = List.copyOf(containers);
    }

    public static NearbyStorage empty(BlockPos origin) {
        return new NearbyStorage(null, origin, List.of());
    }

    /**
     * Performs the relatively expensive radius scan. Only container positions
     * are cached, so slot contents can still change immediately between scans.
     */
    public static NearbyStorage scan(Level level, BlockPos origin, int radius) {
        List<ContainerReference> found = new ArrayList<>();
        BlockPos min = origin.offset(-radius, -radius, -radius);
        BlockPos max = origin.offset(radius, radius, radius);
        double radiusSquared = (double) radius * radius;

        for (BlockPos mutablePos : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = mutablePos.immutable();
            if (pos.equals(origin)
                    || pos.distSqr(origin) > radiusSquared
                    || !level.hasChunkAt(pos)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container) {
                found.add(new ContainerReference(pos, (int) pos.distSqr(origin)));
            }
        }

        return new NearbyStorage(level, origin, found);
    }

    /**
     * Builds the item list shown in the storage panel. Item components are part
     * of identity, so differently enchanted or otherwise component-bearing
     * stacks remain separate entries.
     */
    public List<StorageItemEntry> snapshot() {
        List<MutableEntry> totals = new ArrayList<>();

        for (SlotReference reference : currentSlots()) {
            ItemStack stack = reference.currentStack();
            if (stack.isEmpty()) {
                continue;
            }

            MutableEntry existing = null;
            for (MutableEntry candidate : totals) {
                if (ItemStack.isSameItemSameComponents(candidate.stack, stack)) {
                    existing = candidate;
                    break;
                }
            }

            if (existing == null) {
                totals.add(new MutableEntry(stack.copyWithCount(1), stack.getCount()));
            } else {
                existing.count = saturatedAdd(existing.count, stack.getCount());
            }
        }

        List<StorageItemEntry> result = new ArrayList<>(totals.size());
        for (MutableEntry total : totals) {
            result.add(new StorageItemEntry(total.stack, total.count));
        }
        result.sort(Comparator
                .comparing((StorageItemEntry value) -> value.stack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(value -> value.stack().getDescriptionId()));
        return List.copyOf(result);
    }

    /** Adds nearby storage stacks to vanilla's recipe availability counter. */
    public void account(StackedContents contents) {
        for (SlotReference reference : currentSlots()) {
            ItemStack stack = reference.currentStack();
            if (!stack.isEmpty()) {
                contents.accountStack(stack, stack.getCount());
            }
        }
    }

    /**
     * Moves up to {@code remaining} matching items into a crafting-grid slot.
     * Started storage stacks are consumed first, then nearer containers.
     * Returns the number of items still missing after the operation.
     */
    public int moveToCraftingSlot(Slot target, ItemStack wanted, int remaining) {
        if (remaining <= 0 || wanted.isEmpty()) {
            return 0;
        }

        List<SlotReference> candidates = currentSlots().stream()
                .filter(reference -> {
                    ItemStack stack = reference.currentStack();
                    return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, wanted);
                })
                .sorted(Comparator
                        .comparingInt((SlotReference reference) -> isStartedStack(reference.currentStack()) ? 0 : 1)
                        .thenComparingInt(SlotReference::distanceSquared)
                        .thenComparingLong(reference -> reference.pos().asLong()))
                .toList();

        for (SlotReference reference : candidates) {
            if (remaining <= 0) {
                break;
            }

            ItemStack source = reference.currentStack();
            if (source.isEmpty()) {
                continue;
            }

            ItemStack targetStack = target.getItem();
            if (!targetStack.isEmpty() && !ItemStack.isSameItemSameComponents(targetStack, wanted)) {
                break;
            }

            int targetCount = targetStack.isEmpty() ? 0 : targetStack.getCount();
            int room = Math.min(target.getMaxStackSize(wanted), wanted.getMaxStackSize()) - targetCount;
            if (room <= 0) {
                break;
            }

            int moved = Math.min(Math.min(source.getCount(), remaining), room);
            if (moved <= 0) {
                continue;
            }

            if (targetStack.isEmpty()) {
                target.set(wanted.copyWithCount(moved));
            } else {
                targetStack.grow(moved);
                target.setChanged();
            }

            source.shrink(moved);
            reference.container().setItem(
                    reference.slot(),
                    source.isEmpty() ? ItemStack.EMPTY : source
            );
            reference.container().setChanged();
            remaining -= moved;
        }

        return remaining;
    }

    /**
     * Returns every container holding the selected stack, together with its
     * exact amount. The container with the largest amount is first; equal
     * amounts are resolved by distance to the requesting player.
     */
    public List<StorageHighlightTarget> highlightTargets(ItemStack wanted, Vec3 observerPosition) {
        if (wanted.isEmpty()) {
            return List.of();
        }

        Map<BlockPos, Integer> perBlockAmounts = new HashMap<>();
        for (SlotReference reference : currentSlots()) {
            ItemStack stack = reference.currentStack();
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, wanted)) {
                continue;
            }

            perBlockAmounts.merge(reference.pos(), stack.getCount(), NearbyStorage::saturatedAdd);
        }

        Map<HighlightArea, Integer> amounts = new HashMap<>();
        for (Map.Entry<BlockPos, Integer> entry : perBlockAmounts.entrySet()) {
            amounts.merge(highlightArea(entry.getKey()), entry.getValue(), NearbyStorage::saturatedAdd);
        }

        return amounts.entrySet().stream()
                .map(entry -> new StorageHighlightTarget(
                        entry.getKey().minPos(),
                        entry.getKey().maxPos(),
                        entry.getValue()
                ))
                .sorted(Comparator
                        .comparingInt(StorageHighlightTarget::count)
                        .reversed()
                        .thenComparingDouble(target -> distanceSquared(target, observerPosition))
                        .thenComparingLong(target -> target.minPos().asLong())
                        .thenComparingLong(target -> target.maxPos().asLong()))
                .toList();
    }

    /** Resolves both halves of a live vanilla normal or trapped chest. */
    private HighlightArea highlightArea(BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) {
            return HighlightArea.single(pos);
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)
                || !state.hasProperty(ChestBlock.TYPE)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return HighlightArea.single(pos);
        }

        BlockPos partnerPos = pos.relative(ChestBlock.getConnectedDirection(state));
        if (!level.hasChunkAt(partnerPos)) {
            return HighlightArea.single(pos);
        }

        BlockState partnerState = level.getBlockState(partnerPos);
        boolean validPartner = partnerState.getBlock() == state.getBlock()
                && partnerState.hasProperty(ChestBlock.TYPE)
                && partnerState.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && partnerPos.relative(ChestBlock.getConnectedDirection(partnerState)).equals(pos);
        return validPartner ? HighlightArea.of(pos, partnerPos) : HighlightArea.single(pos);
    }

    private static double distanceSquared(StorageHighlightTarget target, Vec3 observerPosition) {
        double x = target.centerX() - observerPosition.x;
        double y = target.centerY() - observerPosition.y;
        double z = target.centerZ() - observerPosition.z;
        return x * x + y * y + z * z;
    }

    /**
     * Resolves cached positions against the live world. Removed or unloaded
     * containers disappear immediately, while newly filled slots are visible
     * without waiting for the next radius scan.
     */
    private List<SlotReference> currentSlots() {
        if (level == null || containers.isEmpty()) {
            return List.of();
        }

        List<SlotReference> result = new ArrayList<>();
        for (ContainerReference reference : containers) {
            Container container = reference.currentContainer(level);
            if (container == null) {
                continue;
            }

            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (!container.getItem(slot).isEmpty()) {
                    result.add(new SlotReference(
                            reference.pos(),
                            container,
                            slot,
                            reference.distanceSquared()
                    ));
                }
            }
        }
        return result;
    }

    private static boolean isStartedStack(ItemStack stack) {
        return stack.getCount() < stack.getMaxStackSize();
    }

    private static int saturatedAdd(int first, int second) {
        long result = (long) first + second;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }


    private record HighlightArea(BlockPos minPos, BlockPos maxPos) {
        private HighlightArea {
            minPos = minPos.immutable();
            maxPos = maxPos.immutable();
        }

        static HighlightArea single(BlockPos pos) {
            return new HighlightArea(pos, pos);
        }

        static HighlightArea of(BlockPos first, BlockPos second) {
            return new HighlightArea(
                    new BlockPos(
                            Math.min(first.getX(), second.getX()),
                            Math.min(first.getY(), second.getY()),
                            Math.min(first.getZ(), second.getZ())
                    ),
                    new BlockPos(
                            Math.max(first.getX(), second.getX()),
                            Math.max(first.getY(), second.getY()),
                            Math.max(first.getZ(), second.getZ())
                    )
            );
        }
    }

    private record ContainerReference(BlockPos pos, int distanceSquared) {
        Container currentContainer(Level level) {
            if (!level.hasChunkAt(pos)) {
                return null;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            return blockEntity instanceof Container container ? container : null;
        }
    }

    private record SlotReference(BlockPos pos, Container container, int slot, int distanceSquared) {
        ItemStack currentStack() {
            return container.getItem(slot);
        }
    }

    private static final class MutableEntry {
        private final ItemStack stack;
        private int count;

        private MutableEntry(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }
}
