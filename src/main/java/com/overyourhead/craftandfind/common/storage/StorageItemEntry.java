package com.overyourhead.craftandfind.common.storage;

import net.minecraft.world.item.ItemStack;

public record StorageItemEntry(ItemStack stack, int count) {
    public StorageItemEntry {
        stack = stack.copyWithCount(1);
        count = Math.max(0, count);
    }
}
