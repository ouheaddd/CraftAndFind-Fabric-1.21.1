package com.overyourhead.craftandfind.core;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public final class ModCreativeTabs {
    public static final CreativeModeTab CRAFT_AND_FIND = FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.craftandfind"))
            .icon(() -> ModItems.STORAGE_WORKBENCH_ITEM.getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(ModItems.STORAGE_WORKBENCH_ITEM))
            .build();

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "craftandfind"), CRAFT_AND_FIND);
    }

    private ModCreativeTabs() {
    }
}
