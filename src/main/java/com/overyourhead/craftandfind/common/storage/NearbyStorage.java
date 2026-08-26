package com.overyourhead.craftandfind.common.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A cached view of loaded storage positions inside the workbench radius.
 * Items are never stored here: every operation resolves the current block
 * entity / Fabric storage API and reads or moves stacks in the original storage.
 */
public final class NearbyStorage {
    private static final String SCULK_SENTINEL_CHEST_ID = "curiouschests:sculk_sentinel_chest";

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
     * Returns as much of a crafting-grid stack as possible to nearby storage.
     * The returned stack is the remainder that no storage accepted.
     */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();
        for (ResolvedStorage resolved : currentStorages()) {
            remaining = resolved.storage().insert(remaining, false);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
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
        List<SlotReference> result = new ArrayList<>();
        for (ResolvedStorage resolved : currentStorages()) {
            resolved.storage().collectSlots(
                    resolved.pos(),
                    resolved.distanceSquared(),
                    result
            );
        }
        return result;
    }

    /**
     * Resolves cached storage positions and removes duplicate logical inventories.
     * Multi-block/network storages may expose the exact same Transfer API storage
     * from several block positions; those positions must count only once.
     */
    private List<ResolvedStorage> currentStorages() {
        if (level == null || storages.isEmpty()) {
            return List.of();
        }

        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ResolvedStorage> result = new ArrayList<>();
        for (StorageReference reference : storages) {
            StorageAccess storage = reference.currentStorage(level);
            if (storage == null || !seen.add(storage.identity())) {
                continue;
            }
            result.add(new ResolvedStorage(reference.pos(), storage, reference.distanceSquared()));
        }

        result.sort(Comparator
                .comparingInt(ResolvedStorage::distanceSquared)
                .thenComparingLong(value -> value.pos().asLong()));
        return result;
    }

    /**
     * Vanilla and vanilla-style containers are read directly. This is important
     * for double chests: Fabric's Transfer API may expose the full logical chest
     * from both block positions, which otherwise doubles the visible count.
     * Non-Container modded storages keep using the generic Transfer API fallback.
     */
    private static StorageAccess resolveStorage(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return null;
        }

        // Curious Chests' Sculk Sentinel Chest is intentionally invisible to
        // Craft & Find. Check the block id before touching its block entity or
        // any inventory/capability API, so its contents can never be read here.
        if (SCULK_SENTINEL_CHEST_ID.equals(
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString()
        )) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }

        if (blockEntity instanceof Container container) {
            return new ContainerStorageAccess(container);
        }

        Storage<ItemVariant> transferStorage = findTransferStorage(level, pos);
        return transferStorage != null ? new TransferStorageAccess(transferStorage) : null;
    }

    private static Storage<ItemVariant> findTransferStorage(Level level, BlockPos pos) {
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

    private interface StorageAccess {
        Object identity();

        void collectSlots(BlockPos pos, int distanceSquared, List<SlotReference> output);

        ItemStack insert(ItemStack stack, boolean simulate);
    }

    private record ContainerStorageAccess(Container container) implements StorageAccess {
        @Override
        public Object identity() {
            return container;
        }

        @Override
        public void collectSlots(BlockPos pos, int distanceSquared, List<SlotReference> output) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (!container.getItem(slot).isEmpty()) {
                    output.add(new SlotReference(
                            pos,
                            new ContainerSlotAccess(container, slot),
                            distanceSquared
                    ));
                }
            }
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack remaining = stack.copy();
            boolean changed = false;
            for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
                for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                    ItemStack existing = container.getItem(slot);
                    boolean matching = !existing.isEmpty()
                            && ItemStack.isSameItemSameComponents(existing, remaining);
                    if ((pass == 0 && !matching) || (pass == 1 && !existing.isEmpty())) {
                        continue;
                    }
                    if (!container.canPlaceItem(slot, remaining)) {
                        continue;
                    }

                    int limit = Math.min(container.getMaxStackSize(), remaining.getMaxStackSize());
                    int room = existing.isEmpty() ? limit : limit - existing.getCount();
                    if (room <= 0) {
                        continue;
                    }

                    int moved = Math.min(room, remaining.getCount());
                    if (!simulate) {
                        if (existing.isEmpty()) {
                            container.setItem(slot, remaining.copyWithCount(moved));
                        } else {
                            existing.grow(moved);
                        }
                        changed = true;
                    }
                    remaining.shrink(moved);
                }
            }

            if (changed) {
                container.setChanged();
            }
            return remaining;
        }
    }

    private record TransferStorageAccess(Storage<ItemVariant> storage) implements StorageAccess {
        @Override
        public Object identity() {
            return storage;
        }

        @Override
        public void collectSlots(BlockPos pos, int distanceSquared, List<SlotReference> output) {
            for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
                ItemStack stack = stackFromView(view);
                if (!stack.isEmpty()) {
                    output.add(new SlotReference(
                            pos,
                            new TransferSlotAccess(view),
                            distanceSquared
                    ));
                }
            }
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemVariant resource = ItemVariant.of(stack);
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted = storage.insert(resource, stack.getCount(), transaction);
                if (inserted <= 0L) {
                    return stack.copy();
                }

                if (!simulate) {
                    transaction.commit();
                }

                int insertedCount = (int) Math.min(inserted, stack.getCount());
                int remainingCount = stack.getCount() - insertedCount;
                return remainingCount <= 0
                        ? ItemStack.EMPTY
                        : stack.copyWithCount(remainingCount);
            }
        }
    }

    private interface SlotAccess {
        ItemStack currentStack();

        ItemStack extract(int amount, boolean simulate);
    }

    private record ContainerSlotAccess(Container container, int slot) implements SlotAccess {
        @Override
        public ItemStack currentStack() {
            return container.getItem(slot);
        }

        @Override
        public ItemStack extract(int amount, boolean simulate) {
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (simulate) {
                return stack.copyWithCount(Math.min(amount, stack.getCount()));
            }

            ItemStack extracted = container.removeItem(slot, amount);
            if (!extracted.isEmpty()) {
                container.setChanged();
            }
            return extracted;
        }
    }

    private record TransferSlotAccess(StorageView<ItemVariant> view) implements SlotAccess {
        @Override
        public ItemStack currentStack() {
            return stackFromView(view);
        }

        @Override
        public ItemStack extract(int amount, boolean simulate) {
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

    private record StorageReference(BlockPos pos, int distanceSquared) {
        StorageAccess currentStorage(Level level) {
            return resolveStorage(level, pos);
        }
    }

    private record ResolvedStorage(BlockPos pos, StorageAccess storage, int distanceSquared) {
    }

    private record SlotReference(BlockPos pos, SlotAccess access, int distanceSquared) {
        ItemStack currentStack() {
            return access.currentStack();
        }

        ItemStack extract(int amount, boolean simulate) {
            return access.extract(amount, simulate);
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
