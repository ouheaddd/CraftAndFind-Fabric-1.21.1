package com.overyourhead.craftandfind.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CompassButton extends AbstractWidget {
    private final Runnable onPress;

    public CompassButton(int x, int y, Runnable onPress) {
        super(x, y, 20, 18, Component.translatable("gui.craftandfind.open_storage"));
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int background = isHoveredOrFocused() ? 0xFFF0F0F0 : 0xFFC6C6C6;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, background);
        graphics.renderItem(new ItemStack(Items.COMPASS), getX() + 2, getY() + 1);
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
