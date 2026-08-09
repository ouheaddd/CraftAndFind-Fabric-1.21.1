package com.overyourhead.craftandfind.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Client-only accessor used to align a slot with a custom GUI frame. Slot
 * coordinates are final in 1.21.1, so they must be changed through Mixin.
 */
@Mixin(Slot.class)
public interface SlotPositionAccessor {
    @Mutable
    @Accessor("x")
    void craftandfind$setX(int x);

    @Mutable
    @Accessor("y")
    void craftandfind$setY(int y);
}
