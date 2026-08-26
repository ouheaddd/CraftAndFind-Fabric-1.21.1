package com.overyourhead.craftandfind.client.gui;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import com.overyourhead.craftandfind.common.network.payload.RecipeTransferPayload;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Craft & Find's own recipe browser for the Storage Workbench.
 *
 * It deliberately does not read ClientRecipeBook/RecipeCollection. Mods such as
 * Not Enough Recipe Book are therefore free to disable Minecraft's recipe book
 * without also removing the Storage Workbench's recipe tab.
 */
public final class WorkbenchRecipePanel {
    private static final int COLUMNS = 5;
    private static final int ROWS = 4;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int CELL = 25;
    private static final int GRID_X = 11;
    private static final int GRID_Y = 34;
    private static final int PAGE_Y = 139;
    private static final int PREVIOUS_X = 33;
    private static final int NEXT_X = 94;
    private static final int PAGE_BUTTON_WIDTH = 20;
    private static final int PAGE_BUTTON_HEIGHT = 18;

    private static String rememberedSearch = "";
    private static boolean rememberedCraftableOnly;
    private static int rememberedPage;

    public static final int WIDTH = WorkbenchLayout.PANEL_WIDTH;
    public static final int HEIGHT = WorkbenchLayout.PANEL_HEIGHT;

    private final Minecraft minecraft;
    private final Font font;
    private final CraftingMenu menu;
    private final EditBox searchBox;
    private final Component searchHint = Component.translatable("gui.craftandfind.search");
    private final List<RecipeEntry> recipes = new ArrayList<>();
    private final List<RecipeEntry> filteredRecipes = new ArrayList<>();

    private boolean open;
    private boolean craftableOnly = rememberedCraftableOnly;
    private int x;
    private int y;
    private int page = rememberedPage;
    private int lastInventoryVersion = Integer.MIN_VALUE;
    private int lastGridFingerprint = Integer.MIN_VALUE;
    private int lastStorageFingerprint = Integer.MIN_VALUE;

    public WorkbenchRecipePanel(Minecraft minecraft, Font font, CraftingMenu menu) {
        this.minecraft = minecraft;
        this.font = font;
        this.menu = menu;
        this.searchBox = new EditBox(
                font,
                0,
                0,
                WorkbenchLayout.SEARCH_TEXT_WIDTH,
                WorkbenchLayout.SEARCH_TEXT_HEIGHT,
                searchHint
        );
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setHint(Component.empty());
        searchBox.setValue(rememberedSearch);
        searchBox.setResponder(value -> {
            rememberedSearch = value;
            rebuildFilter(true);
        });
        searchBox.visible = false;
        reloadRecipes();
    }

    public EditBox searchBox() {
        return searchBox;
    }

    public boolean isOpen() {
        return open;
    }

    public int left() {
        return x;
    }

    public int top() {
        return y;
    }

    public ItemStack itemUnderMouse(double mouseX, double mouseY) {
        RecipeEntry entry = entryAt(mouseX, mouseY);
        return entry == null ? ItemStack.EMPTY : entry.result().copy();
    }

    public void setOpen(boolean open) {
        this.open = open;
        searchBox.visible = open;
        searchBox.setFocused(false);
        if (open) {
            refreshAvailability(true);
        }
    }

    public void updatePosition(int mainLeft, int screenWidth, int screenHeight) {
        x = screenWidth < 379
                ? mainLeft
                : mainLeft - WIDTH - WorkbenchLayout.PANEL_GAP;
        y = (screenHeight - HEIGHT) / 2;
        searchBox.setX(x + WorkbenchLayout.SEARCH_TEXT_X);
        searchBox.setY(y + WorkbenchLayout.SEARCH_TEXT_Y);
    }

    public void tick() {
        if (!open || minecraft.player == null) {
            return;
        }

        int inventoryVersion = minecraft.player.getInventory().getTimesChanged();
        int gridFingerprint = gridFingerprint();
        int storageFingerprint = storageFingerprint();
        if (inventoryVersion != lastInventoryVersion
                || gridFingerprint != lastGridFingerprint
                || storageFingerprint != lastStorageFingerprint) {
            refreshAvailability(false);
        }
    }

    public void onStorageUpdated() {
        refreshAvailability(false);
    }

    public void recipesUpdated() {
        reloadRecipes();
    }

    public void reloadRecipes() {
        recipes.clear();
        if (minecraft.level == null) {
            filteredRecipes.clear();
            return;
        }

        var registryAccess = minecraft.level.registryAccess();
        for (RecipeHolder<CraftingRecipe> holder
                : minecraft.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            try {
                CraftingRecipe recipe = holder.value();
                if (recipe.isSpecial() || !recipe.canCraftInDimensions(3, 3) || recipe.isIncomplete()) {
                    continue;
                }

                ItemStack result = recipe.getResultItem(registryAccess).copy();
                if (result.isEmpty()) {
                    continue;
                }

                recipes.add(new RecipeEntry(holder, result, false));
            } catch (RuntimeException ignored) {
                // A broken third-party recipe must never make the workbench unusable.
            }
        }

        refreshAvailability(true);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!open) {
            return;
        }

        graphics.blit(
                WorkbenchTextures.RECIPE_PANEL,
                x,
                y,
                0,
                0,
                WIDTH,
                HEIGHT,
                256,
                256
        );

        renderSearchBox(graphics, mouseX, mouseY, partialTick);
        renderFilterButton(graphics, mouseX, mouseY);

        if (filteredRecipes.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.craftandfind.no_recipes"),
                    x + WIDTH / 2,
                    y + 78,
                    0xFFFFFFFF
            );
            renderFilterTooltip(graphics, mouseX, mouseY);
            return;
        }

        clampPage();
        int first = page * PAGE_SIZE;
        int end = Math.min(filteredRecipes.size(), first + PAGE_SIZE);
        for (int index = first; index < end; index++) {
            int visible = index - first;
            int column = visible % COLUMNS;
            int row = visible / COLUMNS;
            int slotX = x + GRID_X + column * CELL;
            int slotY = y + GRID_Y + row * CELL;
            RecipeEntry entry = filteredRecipes.get(index);

            ResourceLocation background = entry.craftable()
                    ? WorkbenchTextures.RECIPE_SLOT_AVAILABLE_SPRITE
                    : WorkbenchTextures.RECIPE_SLOT_UNAVAILABLE_SPRITE;
            graphics.blitSprite(background, slotX, slotY, CELL, CELL);
            graphics.renderItem(entry.result(), slotX + 4, slotY + 4);
            graphics.renderItemDecorations(font, entry.result(), slotX + 4, slotY + 4, null);
        }

        renderPageControls(graphics, mouseX, mouseY);

        RecipeEntry hovered = entryAt(mouseX, mouseY);
        if (hovered != null) {
            graphics.renderTooltip(font, hovered.result(), mouseX, mouseY);
        } else {
            renderFilterTooltip(graphics, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) {
            return false;
        }

        if (button == 0 && isInsideSearchBox(mouseX, mouseY)) {
            searchBox.setFocused(true);
            searchBox.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (button == 0 && searchBox.isFocused()) {
            searchBox.setFocused(false);
        }

        if (button == 0 && isInsideFilterButton(mouseX, mouseY)) {
            craftableOnly = !craftableOnly;
            rememberedCraftableOnly = craftableOnly;
            rebuildFilter(true);
            return true;
        }

        if (button == 0 && page > 0 && isInsidePrevious(mouseX, mouseY)) {
            page--;
            rememberedPage = page;
            return true;
        }

        if (button == 0 && page + 1 < pageCount() && isInsideNext(mouseX, mouseY)) {
            page++;
            rememberedPage = page;
            return true;
        }

        if (button == 0 && clickRecipeEntry(mouseX, mouseY)) {
            return true;
        }

        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    /**
     * Handles a primary click on a recipe tile without using Minecraft's
     * recipe-book placement packet. NERB intentionally empties the vanilla
     * recipe book, so that packet may be rejected even though the recipe exists.
     */
    public boolean clickRecipeEntry(double mouseX, double mouseY) {
        if (!open) {
            return false;
        }

        RecipeEntry entry = entryAt(mouseX, mouseY);
        if (entry == null) {
            return false;
        }

        if (minecraft.screen instanceof StorageWorkbenchScreen storageScreen) {
            storageScreen.clearGhostRecipe();
        }

        ClientPlayNetworking.send(new RecipeTransferPayload(
                menu.containerId,
                entry.holder().id(),
                Screen.hasShiftDown()
        ));
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!open || mouseX < x || mouseX >= x + WIDTH || mouseY < y || mouseY >= y + HEIGHT) {
            return false;
        }

        if (scrollY < 0 && page + 1 < pageCount()) {
            page++;
            rememberedPage = page;
        } else if (scrollY > 0 && page > 0) {
            page--;
            rememberedPage = page;
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return open && searchBox.isFocused() && searchBox.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return open && searchBox.isFocused() && searchBox.charTyped(codePoint, modifiers);
    }

    private void refreshAvailability(boolean resetPage) {
        if (minecraft.player == null) {
            return;
        }

        StackedContents contents = new StackedContents();
        minecraft.player.getInventory().fillStackedContents(contents);

        // Do not call menu.fillCraftSlotsStackedContents here: Craft & Find's
        // client mixin already appends nearby storage to that vanilla method.
        // Account the real 3x3 grid ourselves so storage is counted exactly once.
        for (int slotIndex = 1; slotIndex <= 9 && slotIndex < menu.slots.size(); slotIndex++) {
            ItemStack stack = menu.getSlot(slotIndex).getItem();
            if (!stack.isEmpty()) {
                contents.accountStack(stack);
            }
        }
        for (StorageItemEntry entry : ClientStorageState.entries(menu.containerId)) {
            accountStorageEntry(contents, entry);
        }

        for (int index = 0; index < recipes.size(); index++) {
            RecipeEntry old = recipes.get(index);
            boolean craftable;
            try {
                craftable = contents.canCraft(old.holder().value(), null);
            } catch (RuntimeException ignored) {
                craftable = false;
            }
            recipes.set(index, new RecipeEntry(old.holder(), old.result(), craftable));
        }

        lastInventoryVersion = minecraft.player.getInventory().getTimesChanged();
        lastGridFingerprint = gridFingerprint();
        lastStorageFingerprint = storageFingerprint();
        rebuildFilter(resetPage);
    }


    private static void accountStorageEntry(StackedContents contents, StorageItemEntry entry) {
        if (entry.count() <= 0 || entry.stack().isEmpty()) {
            return;
        }

        // One 3x3 craft can consume at most nine ingredient units. Capping
        // avoids walking thousands of virtual stacks from huge modded storage.
        int remaining = Math.min(entry.count(), 64);
        int stackLimit = Math.max(1, entry.stack().getMaxStackSize());
        while (remaining > 0) {
            int amount = Math.min(remaining, stackLimit);
            contents.accountStack(entry.stack().copyWithCount(amount));
            remaining -= amount;
        }
    }

    private void rebuildFilter(boolean resetPage) {
        filteredRecipes.clear();
        String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);

        for (RecipeEntry entry : recipes) {
            if (craftableOnly && !entry.craftable()) {
                continue;
            }

            if (!query.isEmpty()) {
                String name = entry.result().getHoverName().getString().toLowerCase(Locale.ROOT);
                String id = entry.holder().id().toString().toLowerCase(Locale.ROOT);
                if (!name.contains(query) && !id.contains(query)) {
                    continue;
                }
            }
            filteredRecipes.add(entry);
        }

        if (resetPage) {
            page = 0;
        }
        clampPage();
        rememberedPage = page;
    }

    private void renderSearchBox(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation background = searchBox.isFocused()
                ? WorkbenchTextures.RECIPE_SEARCH_BOX_FOCUSED
                : WorkbenchTextures.RECIPE_SEARCH_BOX;
        int fieldX = x + WorkbenchLayout.SEARCH_BACKGROUND_X;
        int fieldY = y + WorkbenchLayout.SEARCH_BACKGROUND_Y;

        graphics.blit(
                background,
                fieldX,
                fieldY,
                0,
                0,
                WorkbenchLayout.SEARCH_BACKGROUND_WIDTH,
                WorkbenchLayout.SEARCH_BACKGROUND_HEIGHT,
                WorkbenchLayout.SEARCH_BACKGROUND_WIDTH,
                WorkbenchLayout.SEARCH_BACKGROUND_HEIGHT
        );
        graphics.blit(
                WorkbenchTextures.SEARCH_ICON,
                x + WorkbenchLayout.SEARCH_ICON_X,
                y + WorkbenchLayout.SEARCH_ICON_Y,
                0,
                0,
                16,
                16,
                16,
                16
        );
        searchBox.render(graphics, mouseX, mouseY, partialTick);

        if (!searchBox.isFocused() && searchBox.getValue().isEmpty()) {
            graphics.drawString(
                    font,
                    searchHint.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    x + WorkbenchLayout.SEARCH_TEXT_X,
                    y + WorkbenchLayout.SEARCH_TEXT_Y + 1,
                    0xFF808080,
                    false
            );
        }
    }

    private void renderFilterButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int buttonX = x + WorkbenchLayout.SORT_BUTTON_X;
        int buttonY = y + WorkbenchLayout.SORT_BUTTON_Y;
        ResourceLocation texture = craftableOnly
                ? WorkbenchTextures.RECIPE_FILTER_BUTTON_SELECTED
                : (isInsideFilterButton(mouseX, mouseY)
                ? WorkbenchTextures.RECIPE_FILTER_BUTTON_HOVERED
                : WorkbenchTextures.RECIPE_FILTER_BUTTON);
        graphics.blit(
                texture,
                buttonX,
                buttonY,
                0,
                0,
                WorkbenchLayout.SORT_BUTTON_WIDTH,
                WorkbenchLayout.SORT_BUTTON_HEIGHT,
                WorkbenchLayout.SORT_BUTTON_WIDTH,
                WorkbenchLayout.SORT_BUTTON_HEIGHT
        );
    }

    private void renderFilterTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isInsideFilterButton(mouseX, mouseY)) {
            return;
        }
        graphics.renderTooltip(
                font,
                Component.translatable(craftableOnly
                        ? "gui.craftandfind.show_all_recipes"
                        : "gui.craftandfind.show_craftable_recipes"),
                mouseX,
                mouseY
        );
    }

    private void renderPageControls(GuiGraphics graphics, int mouseX, int mouseY) {
        int pages = pageCount();
        if (pages <= 1) {
            return;
        }

        if (page > 0) {
            ResourceLocation previous = isInsidePrevious(mouseX, mouseY)
                    ? WorkbenchTextures.PAGE_PREVIOUS_HOVERED_SPRITE
                    : WorkbenchTextures.PAGE_PREVIOUS_SPRITE;
            graphics.blitSprite(previous, x + PREVIOUS_X, y + PAGE_Y, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);
        }
        if (page + 1 < pages) {
            ResourceLocation next = isInsideNext(mouseX, mouseY)
                    ? WorkbenchTextures.PAGE_NEXT_HOVERED_SPRITE
                    : WorkbenchTextures.PAGE_NEXT_SPRITE;
            graphics.blitSprite(next, x + NEXT_X, y + PAGE_Y, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);
        }

        String pageText = (page + 1) + "/" + pages;
        graphics.drawCenteredString(font, pageText, x + WIDTH / 2, y + PAGE_Y + 5, 0xFFFFFFFF);
    }

    private RecipeEntry entryAt(double mouseX, double mouseY) {
        if (!open) {
            return null;
        }

        int relativeX = (int) mouseX - (x + GRID_X);
        int relativeY = (int) mouseY - (y + GRID_Y);
        if (relativeX < 0 || relativeY < 0) {
            return null;
        }

        int column = relativeX / CELL;
        int row = relativeY / CELL;
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) {
            return null;
        }

        int localX = relativeX % CELL;
        int localY = relativeY % CELL;
        if (localX < 0 || localY < 0 || localX >= CELL || localY >= CELL) {
            return null;
        }

        int index = page * PAGE_SIZE + row * COLUMNS + column;
        return index >= 0 && index < filteredRecipes.size() ? filteredRecipes.get(index) : null;
    }

    private int gridFingerprint() {
        int hash = 1;
        for (int slotIndex = 1; slotIndex <= 9 && slotIndex < menu.slots.size(); slotIndex++) {
            ItemStack stack = menu.getSlot(slotIndex).getItem();
            hash = 31 * hash + stack.getItem().hashCode();
            hash = 31 * hash + stack.getCount();
            hash = 31 * hash + stack.getComponents().hashCode();
        }
        return hash;
    }

    private int storageFingerprint() {
        int hash = 1;
        for (StorageItemEntry entry : ClientStorageState.entries(menu.containerId)) {
            hash = 31 * hash + entry.stack().getItem().hashCode();
            hash = 31 * hash + entry.stack().getComponents().hashCode();
            hash = 31 * hash + entry.count();
        }
        return hash;
    }

    private int pageCount() {
        return Math.max(1, (filteredRecipes.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void clampPage() {
        page = Math.max(0, Math.min(page, pageCount() - 1));
    }

    private boolean isInsideSearchBox(double mouseX, double mouseY) {
        int fieldX = x + WorkbenchLayout.SEARCH_BACKGROUND_X;
        int fieldY = y + WorkbenchLayout.SEARCH_BACKGROUND_Y;
        return mouseX >= fieldX && mouseX < fieldX + WorkbenchLayout.SEARCH_BACKGROUND_WIDTH
                && mouseY >= fieldY && mouseY < fieldY + WorkbenchLayout.SEARCH_BACKGROUND_HEIGHT;
    }

    private boolean isInsideFilterButton(double mouseX, double mouseY) {
        int buttonX = x + WorkbenchLayout.SORT_BUTTON_X;
        int buttonY = y + WorkbenchLayout.SORT_BUTTON_Y;
        return mouseX >= buttonX && mouseX < buttonX + WorkbenchLayout.SORT_BUTTON_WIDTH
                && mouseY >= buttonY && mouseY < buttonY + WorkbenchLayout.SORT_BUTTON_HEIGHT;
    }

    private boolean isInsidePrevious(double mouseX, double mouseY) {
        return mouseX >= x + PREVIOUS_X && mouseX < x + PREVIOUS_X + PAGE_BUTTON_WIDTH
                && mouseY >= y + PAGE_Y && mouseY < y + PAGE_Y + PAGE_BUTTON_HEIGHT;
    }

    private boolean isInsideNext(double mouseX, double mouseY) {
        return mouseX >= x + NEXT_X && mouseX < x + NEXT_X + PAGE_BUTTON_WIDTH
                && mouseY >= y + PAGE_Y && mouseY < y + PAGE_Y + PAGE_BUTTON_HEIGHT;
    }

    private record RecipeEntry(
            RecipeHolder<CraftingRecipe> holder,
            ItemStack result,
            boolean craftable
    ) {
    }
}
