package com.overyourhead.craftandfind.common.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cached view of loaded storage positions inside the workbench radius.
 * Items are never stored here: every operation resolves the current block
 * entity / Fabric storage API and reads or moves stacks in the original storage.
 */
public final class NearbyStorage {
    private final Level level;
    private final BlockPos origin;
    private final List<StorageReference> storages;

    private NearbyStorage(Level level, BlockPos origin, List<StorageReference> storages) {
        this.level = level;
        this.origin = origin.immutable();
        this.storages = List.copyOf(storages);
    }

    public static NearbyStorage empty(BlockPos origin) {
        return new NearbyStorage(null, origin, List.of());
    }

    /**
     * Performs the relatively expensive radius scan. Only storage positions
     * are cached, so slot contents can still change immediately between scans.
     */
    public static NearbyStorage scan(Level level, BlockPos origin, int radius) {
        List<StorageReference> found = new ArrayList<>();
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

            if (resolveStorage(level, pos) != null) {
                found.add(new StorageReference(pos, (int) pos.distSqr(origin)));
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
     * Started storage stacks are consumed first, then nearer storages.
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

            int requested = Math.min(Math.min(source.getCount(), remaining), room);
            if (requested <= 0) {
                continue;
            }

            // Fabric Transfer API storages may reject or reduce extraction.
            // Simulate first, then commit the real extraction before changing
            // the crafting grid.
            ItemStack simulated = reference.extract(requested, true);
            if (simulated.isEmpty() || !ItemStack.isSameItemSameComponents(simulated, wanted)) {
                continue;
            }

            int extractCount = Math.min(requested, simulated.getCount());
            ItemStack extracted = reference.extract(extractCount, false);
            if (extracted.isEmpty() || !ItemStack.isSameItemSameComponents(extracted, wanted)) {
                continue;
            }

            int moved = Math.min(extractCount, extracted.getCount());
            if (moved <= 0) {
                continue;
            }

            if (targetStack.isEmpty()) {
                target.set(wanted.copyWithCount(moved));
            } else {
                targetStack.grow(moved);
                target.setChanged();
            }

            remaining -= moved;
        }

        return remaining;
    }

    /**
     * Returns every storage block holding the selected stack, together with its
     * exact amount. The storage with the largest amount is first; equal
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
     * storages disappear immediately, while newly filled slots are visible
     * without waiting for the next radius scan.
     */
    private List<SlotReference> currentSlots() {
        if (level == null || storages.isEmpty()) {
            return List.of();
        }

        List<SlotReference> result = new ArrayList<>();
        for (StorageReference reference : storages) {
            Storage<ItemVariant> storage = reference.currentStorage(level);
            if (storage == null) {
                continue;
            }

            for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
                ItemStack stack = stackFromView(view);
                if (!stack.isEmpty()) {
                    result.add(new SlotReference(
                            reference.pos(),
                            view,
                            reference.distanceSquared()
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Resolves a block storage through Fabric Transfer API. The lookup already
     * provides vanilla Container / WorldlyContainer fallbacks, while mods can
     * expose custom Storage<ItemVariant> implementations without Craft & Find
     * depending on them directly.
     */
    private static Storage<ItemVariant> resolveStorage(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return null;
        }

        // Keep the radius scan focused on actual inventory block entities. This
        // avoids treating capability-like utility blocks as nearby chests.
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }

        Storage<ItemVariant> unsided = ItemStorage.SIDED.find(level, pos, null);
        if (unsided != null) {
            return unsided;
        }

        // Some mods expose storage only from a side. Use the side with the
        // largest visible view set instead of hard-coding one direction.
        Storage<ItemVariant> best = null;
        int bestViews = -1;
        for (Direction direction : Direction.values()) {
            Storage<ItemVariant> candidate = ItemStorage.SIDED.find(level, pos, direction);
            if (candidate == null) {
                continue;
            }

            int views = countViews(candidate);
            if (views > bestViews) {
                best = candidate;
                bestViews = views;
            }
        }
        return best;
    }

    private static int countViews(Storage<ItemVariant> storage) {
        int count = 0;
        for (StorageView<ItemVariant> ignored : storage) {
            if (count == Integer.MAX_VALUE) {
                return count;
            }
            count++;
        }
        return count;
    }

    private static ItemStack stackFromView(StorageView<ItemVariant> view) {
        if (view.isResourceBlank() || view.getAmount() <= 0L) {
            return ItemStack.EMPTY;
        }

        ItemVariant resource = view.getResource();
        long amount = Math.min(view.getAmount(), Integer.MAX_VALUE);
        return resource.toStack((int) amount);
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

    private record StorageReference(BlockPos pos, int distanceSquared) {
        Storage<ItemVariant> currentStorage(Level level) {
            return resolveStorage(level, pos);
        }
    }

    private record SlotReference(BlockPos pos, StorageView<ItemVariant> view, int distanceSquared) {
        ItemStack currentStack() {
            return stackFromView(view);
        }

        ItemStack extract(int amount, boolean simulate) {
            if (amount <= 0 || view.isResourceBlank() || view.getAmount() <= 0L) {
                return ItemStack.EMPTY;
            }

            ItemVariant resource = view.getResource();
            try (Transaction transaction = Transaction.openOuter()) {
                long extracted = view.extract(resource, amount, transaction);
                if (extracted <= 0L) {
                    return ItemStack.EMPTY;
                }

                if (!simulate) {
                    transaction.commit();
                }

                int count = (int) Math.min(extracted, Integer.MAX_VALUE);
                return resource.toStack(count);
            }
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
