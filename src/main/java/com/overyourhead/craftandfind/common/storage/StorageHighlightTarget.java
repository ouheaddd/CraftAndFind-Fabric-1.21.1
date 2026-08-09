package com.overyourhead.craftandfind.common.storage;

import net.minecraft.core.BlockPos;

/**
 * A visual storage region and the amount of the selected stack inside it.
 *
 * <p>Most targets occupy one block. A vanilla double chest occupies two
 * horizontally adjacent blocks and is transmitted as one target.</p>
 */
public record StorageHighlightTarget(BlockPos minPos, BlockPos maxPos, int count) {
    public StorageHighlightTarget {
        int minX = Math.min(minPos.getX(), maxPos.getX());
        int minY = Math.min(minPos.getY(), maxPos.getY());
        int minZ = Math.min(minPos.getZ(), maxPos.getZ());
        int maxX = Math.max(minPos.getX(), maxPos.getX());
        int maxY = Math.max(minPos.getY(), maxPos.getY());
        int maxZ = Math.max(minPos.getZ(), maxPos.getZ());

        minPos = new BlockPos(minX, minY, minZ);
        maxPos = new BlockPos(maxX, maxY, maxZ);
        count = Math.max(0, count);

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        if (height != 1 || width > 2 || depth > 2 || width * depth > 2) {
            throw new IllegalArgumentException(
                    "Invalid storage highlight area: " + minPos + " -> " + maxPos
            );
        }
    }

    public StorageHighlightTarget(BlockPos pos, int count) {
        this(pos, pos, count);
    }

    public boolean isDoubleChest() {
        return horizontalBlockCount() == 2;
    }

    public int horizontalBlockCount() {
        return width() * depth();
    }

    public int width() {
        return maxPos.getX() - minPos.getX() + 1;
    }

    public int depth() {
        return maxPos.getZ() - minPos.getZ() + 1;
    }

    public double centerX() {
        return (minPos.getX() + maxPos.getX() + 1.0D) * 0.5D;
    }

    public double centerY() {
        return minPos.getY() + 0.5D;
    }

    public double centerZ() {
        return (minPos.getZ() + maxPos.getZ() + 1.0D) * 0.5D;
    }
}
