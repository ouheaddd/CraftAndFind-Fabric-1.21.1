package com.overyourhead.craftandfind.core;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final BlockItem STORAGE_WORKBENCH_ITEM = new BlockItem(
            ModBlocks.STORAGE_WORKBENCH,
            new Item.Properties()
    );

    private ModItems() {
    }

    public static void register() {
        Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "storage_workbench"),
                STORAGE_WORKBENCH_ITEM
        );
    }
}
