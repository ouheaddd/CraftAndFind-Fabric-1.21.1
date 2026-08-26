package com.overyourhead.craftandfind.client;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Optional recipe-viewer hooks that keep Craft & Find's core GUI free from
 * hard dependencies on JEI/EMI classes.
 */
public final class RecipeViewerHooks {
    @FunctionalInterface
    public interface KeyboardRecipeLookup {
        boolean handle(ItemStack stack, int keyCode, int scanCode);
    }

    private static @Nullable KeyboardRecipeLookup jeiKeyboardLookup;

    private RecipeViewerHooks() {
    }

    public static void setJeiKeyboardLookup(@Nullable KeyboardRecipeLookup lookup) {
        jeiKeyboardLookup = lookup;
    }

    public static boolean handleRecipePanelKey(ItemStack stack, int keyCode, int scanCode) {
        KeyboardRecipeLookup lookup = jeiKeyboardLookup;
        return lookup != null && !stack.isEmpty() && lookup.handle(stack, keyCode, scanCode);
    }
}
