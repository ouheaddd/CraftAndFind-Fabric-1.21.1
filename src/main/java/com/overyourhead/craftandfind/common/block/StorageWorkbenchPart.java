package com.overyourhead.craftandfind.common.block;

import net.minecraft.util.StringRepresentable;

public enum StorageWorkbenchPart implements StringRepresentable {
    LOWER_MAIN("lower_main"),
    LOWER_SIDE("lower_side"),
    UPPER_MAIN("upper_main"),
    UPPER_SIDE("upper_side");

    private final String serializedName;

    StorageWorkbenchPart(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
