package com.overyourhead.craftandfind.client.gui;

import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;

/** A fully texture-driven tab button used by the workbench side controls. */
public final class WorkbenchIconButton extends AbstractWidget {
    private final Runnable onPress;
    private final BooleanSupplier selected;
    private final ResourceLocation icon;

    public WorkbenchIconButton(
            int x,
            int y,
            ResourceLocation icon,
            Component message,
            BooleanSupplier selected,
            Runnable onPress
    ) {
        super(x, y, WorkbenchLayout.TAB_WIDTH, WorkbenchLayout.TAB_HEIGHT, message);
        this.icon = icon;
        this.selected = selected;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation background = selected.getAsBoolean()
                ? WorkbenchTextures.TAB_BUTTON_SELECTED
                : (isHoveredOrFocused()
                ? WorkbenchTextures.TAB_BUTTON_HOVERED
                : WorkbenchTextures.TAB_BUTTON);

        graphics.blit(background, getX(), getY(), 0, 0, width, height, width, height);
        graphics.blit(icon, getX() + 2, getY() + 1, 0, 0, 16, 16, 16, 16);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
