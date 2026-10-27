package com.overyourhead.craftandfind;

import com.overyourhead.craftandfind.common.data.ModDataAttachments;
import com.overyourhead.craftandfind.common.event.CommonEvents;
import com.overyourhead.craftandfind.common.network.CraftAndFindNetwork;
import com.overyourhead.craftandfind.config.CraftAndFindServerConfig;
import com.overyourhead.craftandfind.core.ModBlocks;
import com.overyourhead.craftandfind.core.ModItems;
import com.overyourhead.craftandfind.core.ModCreativeTabs;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;

public final class CraftAndFindMod implements ModInitializer {
    public static final String MOD_ID = "craftandfind";

    @Override
    public void onInitialize() {
        ModDataAttachments.bootstrap();
        ModBlocks.register();
        ModItems.register();
        ModCreativeTabs.register();
        CraftAndFindServerConfig.load();
        CraftAndFindNetwork.register();
        CommonEvents.register();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            // Keep the usual position when the anchor is available in both lists.
            boolean hasDisplayAnchor = entries.getDisplayStacks().stream()
                    .anyMatch(stack -> stack.is(Blocks.ENCHANTING_TABLE.asItem()));
            boolean hasSearchAnchor = entries.getSearchTabStacks().stream()
                    .anyMatch(stack -> stack.is(Blocks.ENCHANTING_TABLE.asItem()));
            if (hasDisplayAnchor && hasSearchAnchor) {
                entries.addAfter(Blocks.ENCHANTING_TABLE, ModItems.STORAGE_WORKBENCH_ITEM);
            } else {
                entries.accept(ModItems.STORAGE_WORKBENCH_ITEM);
            }
        });
    }
}
