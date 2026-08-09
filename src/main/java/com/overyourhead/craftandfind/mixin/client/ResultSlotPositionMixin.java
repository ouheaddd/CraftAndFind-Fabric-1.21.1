package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Slot coordinates are final in 1.21.1. Move only the result slot's visual
 * content, hover highlight and mouse hitbox without mutating the menu slot.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ResultSlotPositionMixin {
    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    protected abstract boolean isHovering(
            int x,
            int y,
            int width,
            int height,
            double mouseX,
            double mouseY
    );

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void craftandfind$pushResultSlotOffset(
            GuiGraphics graphics,
            Slot slot,
            CallbackInfo ci
    ) {
        if (craftandfind$isResultSlot(slot)) {
            graphics.pose().pushPose();
            graphics.pose().translate(
                    WorkbenchLayout.RESULT_SLOT_CONTENT_X_SHIFT,
                    WorkbenchLayout.RESULT_SLOT_CONTENT_Y_SHIFT,
                    0.0F
            );
        }
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void craftandfind$popResultSlotOffset(
            GuiGraphics graphics,
            Slot slot,
            CallbackInfo ci
    ) {
        if (craftandfind$isResultSlot(slot)) {
            graphics.pose().popPose();
        }
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V"
            ),
            index = 1
    )
    private int craftandfind$moveResultHighlightX(int x) {
        return craftandfind$isHoveredResultSlot()
                ? x + WorkbenchLayout.RESULT_SLOT_CONTENT_X_SHIFT
                : x;
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V"
            ),
            index = 2
    )
    private int craftandfind$moveResultHighlightY(int y) {
        return craftandfind$isHoveredResultSlot()
                ? y + WorkbenchLayout.RESULT_SLOT_CONTENT_Y_SHIFT
                : y;
    }

    @Inject(
            method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void craftandfind$moveResultHitbox(
            Slot slot,
            double mouseX,
            double mouseY,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!craftandfind$isResultSlot(slot)) {
            return;
        }

        cir.setReturnValue(isHovering(
                slot.x + WorkbenchLayout.RESULT_SLOT_CONTENT_X_SHIFT,
                slot.y + WorkbenchLayout.RESULT_SLOT_CONTENT_Y_SHIFT,
                16,
                16,
                mouseX,
                mouseY
        ));
    }

    @Unique
    private boolean craftandfind$isHoveredResultSlot() {
        return hoveredSlot != null && craftandfind$isResultSlot(hoveredSlot);
    }

    @Unique
    private boolean craftandfind$isResultSlot(Slot slot) {
        if (!((Object) this instanceof StorageWorkbenchScreen screen)) {
            return false;
        }
        return !screen.getMenu().slots.isEmpty() && screen.getMenu().getSlot(0) == slot;
    }
}
