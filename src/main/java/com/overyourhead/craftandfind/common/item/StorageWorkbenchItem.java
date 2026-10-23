package com.overyourhead.craftandfind.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class StorageWorkbenchItem extends BlockItem {
    public StorageWorkbenchItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // Short localized lines keep the description readable at normal GUI scales.
        for (int line = 1; line <= 6; line++) {
            tooltip.add(Component.translatable("tooltip.craftandfind.storage_workbench." + line)
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.craftandfind.storage_workbench.config")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
