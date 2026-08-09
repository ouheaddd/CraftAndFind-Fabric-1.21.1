package com.overyourhead.craftandfind.core;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.common.block.StorageWorkbenchBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.core.Registry;

public final class ModBlocks {
    public static final StorageWorkbenchBlock STORAGE_WORKBENCH = new StorageWorkbenchBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
    );

    private ModBlocks() {
    }

    public static void register() {
        Registry.register(
                BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "storage_workbench"),
                STORAGE_WORKBENCH
        );
    }
}
