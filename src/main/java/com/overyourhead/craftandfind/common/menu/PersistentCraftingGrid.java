package com.overyourhead.craftandfind.common.menu;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.common.block.StorageWorkbenchBlock;
import com.overyourhead.craftandfind.common.data.ModDataAttachments;
import com.overyourhead.craftandfind.common.block.StorageWorkbenchPart;
import com.overyourhead.craftandfind.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores a private crafting grid for every player, dimension and concrete
 * Storage Workbench position. Two players using the same block never share
 * ingredients, and one player can keep different layouts in different tables.
 */
public final class PersistentCraftingGrid {
    private static final String DATA_KEY = CraftAndFindMod.MOD_ID + "_crafting_grids";
    private static final String VERSION_KEY = "WorkbenchVersion";
    private static final int FIRST_INPUT_SLOT = 1;
    private static final int INPUT_SLOT_COUNT = 9;

    private PersistentCraftingGrid() {
    }

    public static void load(ServerPlayer player, BlockPos workbenchPos, StorageWorkbenchMenu menu) {
        ServerLevel level = player.serverLevel();
        if (!isWorkbenchPresent(level, workbenchPos)) {
            remove(player, level.dimension(), workbenchPos);
            return;
        }

        CompoundTag gridTag = findGridTag(player, level.dimension(), workbenchPos);
        if (gridTag == null) {
            return;
        }

        int savedVersion = gridTag.contains(VERSION_KEY, Tag.TAG_INT)
                ? gridTag.getInt(VERSION_KEY)
                : 0;
        int currentVersion = WorkbenchGridVersionData.get(level).version(workbenchPos);
        if (savedVersion != currentVersion) {
            remove(player, level.dimension(), workbenchPos);
            return;
        }

        NonNullList<ItemStack> savedItems = NonNullList.withSize(INPUT_SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(gridTag, savedItems, player.registryAccess());

        for (int index = 0; index < INPUT_SLOT_COUNT; index++) {
            menu.getSlot(FIRST_INPUT_SLOT + index).set(savedItems.get(index).copy());
        }
    }

    public static void save(ServerPlayer player, BlockPos workbenchPos, StorageWorkbenchMenu menu) {
        ServerLevel level = player.serverLevel();
        if (!isWorkbenchPresent(level, workbenchPos)) {
            remove(player, level.dimension(), workbenchPos);
            return;
        }

        NonNullList<ItemStack> items = NonNullList.withSize(INPUT_SLOT_COUNT, ItemStack.EMPTY);
        boolean hasAnyItem = false;

        for (int index = 0; index < INPUT_SLOT_COUNT; index++) {
            ItemStack stack = menu.getSlot(FIRST_INPUT_SLOT + index).getItem().copy();
            items.set(index, stack);
            hasAnyItem |= !stack.isEmpty();
        }

        if (!hasAnyItem) {
            remove(player, level.dimension(), workbenchPos);
            return;
        }

        CompoundTag playerData = ModDataAttachments.get(player);
        CompoundTag allGrids = playerData.contains(DATA_KEY, Tag.TAG_COMPOUND)
                ? playerData.getCompound(DATA_KEY)
                : new CompoundTag();
        String dimensionKey = dimensionKey(level.dimension());
        CompoundTag dimensionGrids = allGrids.contains(dimensionKey, Tag.TAG_COMPOUND)
                ? allGrids.getCompound(dimensionKey)
                : new CompoundTag();

        CompoundTag gridTag = new CompoundTag();
        ContainerHelper.saveAllItems(gridTag, items, player.registryAccess());
        gridTag.putInt(VERSION_KEY, WorkbenchGridVersionData.get(level).version(workbenchPos));
        dimensionGrids.put(positionKey(workbenchPos), gridTag);
        allGrids.put(dimensionKey, dimensionGrids);
        playerData.put(DATA_KEY, allGrids);
        ModDataAttachments.set(player, playerData);
    }

    /**
     * Starts a fresh lifetime for a newly placed workbench. This also prevents
     * grids left by older mod versions from being restored at a reused position.
     */
    public static void beginNewWorkbench(ServerLevel level, BlockPos workbenchPos) {
        invalidate(level, workbenchPos);
    }

    /**
     * Invalidates the position for every player, including players that are
     * currently offline. Online entries are removed immediately; offline
     * entries are rejected later through the persisted position generation.
     */
    public static void invalidate(ServerLevel level, BlockPos workbenchPos) {
        WorkbenchGridVersionData.get(level).invalidate(workbenchPos);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            remove(player, level.dimension(), workbenchPos);
        }
    }

    /** Copies only Craft & Find grid data to the replacement player entity. */
    public static void copyToClone(Player original, Player clone) {
        CompoundTag originalData = ModDataAttachments.get(original);
        if (originalData.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag cloneData = ModDataAttachments.get(clone);
            cloneData.put(DATA_KEY, originalData.getCompound(DATA_KEY).copy());
            ModDataAttachments.set(clone, cloneData);
        }
    }

    private static CompoundTag findGridTag(
            ServerPlayer player,
            ResourceKey<Level> dimension,
            BlockPos workbenchPos
    ) {
        CompoundTag playerData = ModDataAttachments.get(player);
        if (!playerData.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag allGrids = playerData.getCompound(DATA_KEY);
        String dimensionKey = dimensionKey(dimension);
        if (!allGrids.contains(dimensionKey, Tag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag dimensionGrids = allGrids.getCompound(dimensionKey);
        String positionKey = positionKey(workbenchPos);
        return dimensionGrids.contains(positionKey, Tag.TAG_COMPOUND)
                ? dimensionGrids.getCompound(positionKey)
                : null;
    }

    private static void remove(
            ServerPlayer player,
            ResourceKey<Level> dimension,
            BlockPos workbenchPos
    ) {
        CompoundTag playerData = ModDataAttachments.get(player);
        if (!playerData.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag allGrids = playerData.getCompound(DATA_KEY);
        String dimensionKey = dimensionKey(dimension);
        if (!allGrids.contains(dimensionKey, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag dimensionGrids = allGrids.getCompound(dimensionKey);
        dimensionGrids.remove(positionKey(workbenchPos));

        if (dimensionGrids.isEmpty()) {
            allGrids.remove(dimensionKey);
        } else {
            allGrids.put(dimensionKey, dimensionGrids);
        }

        if (allGrids.isEmpty()) {
            playerData.remove(DATA_KEY);
        } else {
            playerData.put(DATA_KEY, allGrids);
        }
        ModDataAttachments.set(player, playerData);
    }

    private static boolean isWorkbenchPresent(ServerLevel level, BlockPos workbenchPos) {
        BlockState state = level.getBlockState(workbenchPos);
        return state.is(ModBlocks.STORAGE_WORKBENCH)
                && state.hasProperty(StorageWorkbenchBlock.PART)
                && state.getValue(StorageWorkbenchBlock.PART) == StorageWorkbenchPart.LOWER_MAIN;
    }

    private static String dimensionKey(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }

    private static String positionKey(BlockPos position) {
        return Long.toString(position.asLong());
    }
}
