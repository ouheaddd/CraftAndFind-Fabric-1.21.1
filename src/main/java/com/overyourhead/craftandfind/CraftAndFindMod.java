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

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries ->
                entries.addAfter(Blocks.ENCHANTING_TABLE, ModItems.STORAGE_WORKBENCH_ITEM)
        );
    }
}
