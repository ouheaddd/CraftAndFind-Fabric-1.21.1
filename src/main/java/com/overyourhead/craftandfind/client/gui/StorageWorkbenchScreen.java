package com.overyourhead.craftandfind.client.gui;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.client.RecipeViewerHooks;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import com.overyourhead.craftandfind.mixin.client.GhostIngredientAccessor;
import com.overyourhead.craftandfind.mixin.client.RecipeBookComponentAccessor;
import com.overyourhead.craftandfind.mixin.client.SlotPositionAccessor;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class StorageWorkbenchScreen extends CraftingScreen {
    private static boolean rememberedRecipeTabActive;

    private StoragePanel storagePanel;
    private WorkbenchRecipePanel recipePanel;
    private WorkbenchIconButton compassButton;
    private WorkbenchIconButton customRecipeButton;
    private ImageButton vanillaRecipeBookButton;
    private boolean resultSlotShiftApplied;
    private boolean recipeTabActive = rememberedRecipeTabActive;
    private int lastMouseX;
    private int lastMouseY;
    private ItemStack ghostResult = ItemStack.EMPTY;
    private final List<GhostPreviewIngredient> ghostPreviewIngredients = new ArrayList<>();

    public StorageWorkbenchScreen(CraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WorkbenchLayout.MAIN_WIDTH;
        imageHeight = WorkbenchLayout.MAIN_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        applyResultSlotShift();

        for (GuiEventListener child : children()) {
            if (child instanceof ImageButton imageButton
                    && imageButton.getWidth() == 20
                    && imageButton.getHeight() == 18) {
                vanillaRecipeBookButton = imageButton;
                vanillaRecipeBookButton.visible = false;
                vanillaRecipeBookButton.active = false;
                break;
            }
        }

        // Craft & Find owns its recipe panel. Keep Minecraft's recipe book hidden
        // even if another mod removes, redirects or disables the vanilla book.
        setRecipeBookVisible(false);

        storagePanel = new StoragePanel(font, menu.containerId, this::onClose);
        addWidget(storagePanel.searchBox());
        recipePanel = new WorkbenchRecipePanel(minecraft, font, menu);
        addWidget(recipePanel.searchBox());

        compassButton = addRenderableWidget(new WorkbenchIconButton(
                0,
                0,
                WorkbenchTextures.COMPASS_ICON,
                Component.translatable("gui.craftandfind.open_storage"),
                () -> storagePanel != null && storagePanel.isOpen(),
                this::toggleStoragePanel
        ));
        customRecipeButton = addRenderableWidget(new WorkbenchIconButton(
                0,
                0,
                WorkbenchTextures.RECIPE_BOOK_ICON,
                Component.translatable("gui.craftandfind.open_recipes"),
                () -> recipePanel != null && recipePanel.isOpen(),
                this::toggleRecipeBook
        ));
        applyActiveTabState();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The panel, every slot type and the crafting arrow are separate files.
        // Artists can replace one piece without repainting the whole screen.
        blitPart(
                graphics,
                WorkbenchTextures.MAIN_PANEL,
                leftPos,
                topPos,
                imageWidth,
                imageHeight
        );

        blitPart(
                graphics,
                WorkbenchTextures.CRAFTING_ARROW,
                leftPos + WorkbenchLayout.CRAFTING_ARROW_X,
                topPos + WorkbenchLayout.CRAFTING_ARROW_Y,
                WorkbenchLayout.CRAFTING_ARROW_WIDTH,
                WorkbenchLayout.CRAFTING_ARROW_HEIGHT
        );

        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (slotIndex == 0) {
                blitPart(
                        graphics,
                        WorkbenchTextures.RESULT_SLOT,
                        leftPos + slot.x
                                - WorkbenchLayout.RESULT_SLOT_OFFSET
                                + WorkbenchLayout.RESULT_SLOT_X_ADJUST,
                        topPos + slot.y
                                - WorkbenchLayout.RESULT_SLOT_OFFSET
                                + WorkbenchLayout.RESULT_SLOT_Y_ADJUST,
                        WorkbenchLayout.RESULT_SLOT_SIZE,
                        WorkbenchLayout.RESULT_SLOT_SIZE
                );
                continue;
            }

            ResourceLocation slotTexture = slotIndex <= 9
                    ? WorkbenchTextures.CRAFTING_SLOT
                    : WorkbenchTextures.INVENTORY_SLOT;
            blitPart(
                    graphics,
                    slotTexture,
                    leftPos + slot.x - WorkbenchLayout.NORMAL_SLOT_OFFSET,
                    topPos + slot.y - WorkbenchLayout.NORMAL_SLOT_OFFSET,
                    WorkbenchLayout.NORMAL_SLOT_SIZE,
                    WorkbenchLayout.NORMAL_SLOT_SIZE
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                title,
                titleLabelX + WorkbenchLayout.CRAFTING_LABEL_X_OFFSET,
                titleLabelY,
                WorkbenchLayout.LABEL_COLOR,
                true
        );
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                WorkbenchLayout.LABEL_COLOR,
                true
        );
    }

    private static void blitPart(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height
    ) {
        graphics.blit(texture, x, y, 0, 0, width, height, width, height);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (recipePanel != null) {
            recipePanel.tick();
        }
        updateCustomPositions();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        updateCustomPositions();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPreciseGhostPreview(graphics);
        storagePanel.render(graphics, mouseX, mouseY, partialTick);
        recipePanel.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (storagePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (recipePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Early mouse hook used on NeoForge so recipe-viewer mods cannot consume
     * a recipe-tile click before Craft & Find receives it.
     */
    public boolean handleRecipePanelPrimaryClick(double mouseX, double mouseY) {
        return recipePanel != null && recipePanel.clickRecipeEntry(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (storagePanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (storagePanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (storagePanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (recipePanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (storagePanel.isOpen() && storagePanel.searchBox().isFocused()) {
            storagePanel.searchBox().keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        // Recipe-result tiles belong to Craft & Find on mouse clicks. JEI is
        // intentionally invoked only from its keyboard Recipes/Uses bindings,
        // so a normal left click can immediately place/preview the recipe.
        if (recipePanel != null && recipePanel.isOpen() && !recipePanel.searchBox().isFocused()) {
            ItemStack hoveredRecipe = recipePanel.itemUnderMouse(lastMouseX, lastMouseY);
            if (RecipeViewerHooks.handleRecipePanelKey(hoveredRecipe, keyCode, scanCode)) {
                return true;
            }
        }

        if (recipePanel != null && recipePanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (storagePanel.isOpen() && storagePanel.searchBox().charTyped(codePoint, modifiers)) {
            return true;
        }
        if (recipePanel != null && recipePanel.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        // A real workbench close should drop the client snapshot immediately.
        // JEI/EMI recipe screens temporarily replace this screen without closing
        // the active crafting container, so that case is handled in removed().
        ClientStorageState.clear(menu.containerId);
        super.onClose();
    }

    @Override
    public void removed() {
        // Opening JEI/EMI Recipes/Uses calls removed() because another screen is
        // displayed, but the Storage Workbench menu itself is still active. Keep
        // the nearby-storage snapshot alive so the left panel is populated again
        // when the player returns. Clear only after the actual menu has changed.
        if (minecraft == null
                || minecraft.player == null
                || minecraft.player.containerMenu.containerId != menu.containerId) {
            ClientStorageState.clear(menu.containerId);
        }
        super.removed();
    }

    public boolean isStoragePanelOpen() {
        return storagePanel != null && storagePanel.isOpen();
    }

    public Rect2i storagePanelArea() {
        if (storagePanel == null) {
            return new Rect2i(0, 0, 0, 0);
        }
        return new Rect2i(storagePanel.left(), storagePanel.top(), StoragePanel.WIDTH, StoragePanel.HEIGHT);
    }

    public boolean isRecipePanelOpen() {
        return recipePanel != null && recipePanel.isOpen();
    }

    public Rect2i recipePanelArea() {
        if (recipePanel == null) {
            return new Rect2i(0, 0, 0, 0);
        }
        return new Rect2i(recipePanel.left() - 24, recipePanel.top(), WorkbenchRecipePanel.WIDTH + 24, WorkbenchRecipePanel.HEIGHT);
    }

    public ItemStack storageItemUnderMouse(double mouseX, double mouseY) {
        if (storagePanel == null) {
            return ItemStack.EMPTY;
        }
        return storagePanel.itemUnderMouse(mouseX, mouseY);
    }

    public ItemStack recipeItemUnderMouse(double mouseX, double mouseY) {
        if (recipePanel == null) {
            return ItemStack.EMPTY;
        }
        return recipePanel.itemUnderMouse(mouseX, mouseY);
    }

    public void onStorageUpdated() {
        if (storagePanel != null) {
            storagePanel.onStorageUpdated();
        }
        if (recipePanel != null) {
            recipePanel.onStorageUpdated();
        }

        // Keep vanilla's hidden ghost-recipe component in sync without using
        // its recipe collections as our source of recipes.
        super.recipesUpdated();
    }

    @Override
    public void recipesUpdated() {
        super.recipesUpdated();
        if (recipePanel != null) {
            recipePanel.recipesUpdated();
        }
    }

    /** Clears the hidden vanilla ghost recipe used by Craft & Find for previews. */
    public void clearGhostRecipe() {
        ((RecipeBookComponentAccessor) (Object) getRecipeBookComponent())
                .craftandfind$getGhostRecipe()
                .clear();
        ghostResult = ItemStack.EMPTY;
        ghostPreviewIngredients.clear();
    }

    /** Draws the ingredient layout for an unavailable recipe, like vanilla. */
    public void showGhostRecipe(ResourceLocation recipeId) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        // setupGhostRecipe appends ingredients to the existing GhostRecipe.
        // Always clear the previous preview first so rapidly selecting multiple
        // unavailable recipes cannot stack red ghost ingredients on each other.
        clearGhostRecipe();

        // Server-side recipe switching normally clears the real 3x3 grid before
        // this packet arrives. If a full inventory/storage prevented that, never
        // draw a ghost on top of real items: keep the persistent grid readable.
        for (int slotIndex = 1; slotIndex <= 9; slotIndex++) {
            if (!menu.getSlot(slotIndex).getItem().isEmpty()) {
                return;
            }
        }

        minecraft.level.getRecipeManager().byKey(recipeId).ifPresent(recipe -> {
            ghostResult = recipe.value().getResultItem(minecraft.level.registryAccess()).copy();

            // Let vanilla calculate the exact shaped/shapeless positions, copy
            // those positions, then immediately clear its GhostRecipe. Vanilla
            // renders the whole ghost (including the output) as unavailable,
            // which is not what Craft & Find wants.
            getRecipeBookComponent().setupGhostRecipe(recipe, menu.slots);
            GhostRecipe vanillaGhost = ((RecipeBookComponentAccessor) (Object) getRecipeBookComponent())
                    .craftandfind$getGhostRecipe();
            Slot resultSlot = menu.getSlot(0);

            for (int index = 0; index < vanillaGhost.size(); index++) {
                GhostRecipe.GhostIngredient ghost = vanillaGhost.get(index);

                // Some mappings/versions include the output among ghost slots.
                // Never treat it as an ingredient.
                if (ghost.getX() == resultSlot.x && ghost.getY() == resultSlot.y) {
                    continue;
                }

                Ingredient ingredient = ((GhostIngredientAccessor) (Object) ghost)
                        .craftandfind$getIngredient();
                if (!ingredient.isEmpty()) {
                    ghostPreviewIngredients.add(new GhostPreviewIngredient(
                            ingredient,
                            ghost.getX(),
                            ghost.getY()
                    ));
                }
            }

            // Stop Minecraft from drawing its own all-red ghost preview.
            vanillaGhost.clear();
        });
    }

    /**
     * Craft & Find renders the ghost itself so only genuinely missing inputs are
     * red. Inputs that already exist in the player inventory, crafting grid or
     * Nearby Storage stay normal. Because this preview is only shown for an
     * unavailable recipe, the result slot also gets the same red backing so it
     * cannot be mistaken for a currently craftable output.
     */
    private void renderPreciseGhostPreview(GuiGraphics graphics) {
        if (ghostResult.isEmpty() && ghostPreviewIngredients.isEmpty()) {
            return;
        }

        ItemStack[] matchedStacks = matchAvailableGhostIngredients();
        for (int index = 0; index < ghostPreviewIngredients.size(); index++) {
            GhostPreviewIngredient preview = ghostPreviewIngredients.get(index);
            ItemStack display = matchedStacks[index];
            boolean missing = display.isEmpty();

            if (missing) {
                ItemStack[] choices = preview.ingredient().getItems();
                if (choices.length > 0) {
                    display = choices[0].copyWithCount(1);
                }
            }
            if (display.isEmpty()) {
                continue;
            }

            int slotX = leftPos + preview.x();
            int slotY = topPos + preview.y();
            if (missing) {
                // Same visual idea as vanilla/JEI: only the missing input gets
                // the red backing, not every ingredient and never the result.
                graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x66FF0000);
            }
            graphics.renderItem(display, slotX, slotY);
        }

        if (!ghostResult.isEmpty()) {
            Slot resultSlot = menu.getSlot(0);
            int slotX = leftPos + resultSlot.x;
            int slotY = topPos + resultSlot.y;

            // A ghost preview is only shown when the selected recipe cannot be
            // placed. Mark the result slot too, otherwise a normal-looking
            // output can feel clickable/craftable while some inputs are missing.
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x66FF0000);
            graphics.renderItem(ghostResult, slotX, slotY);
            graphics.renderItemDecorations(font, ghostResult, slotX, slotY);
        }
    }

    /**
     * Maximum bipartite matching over at most nine recipe inputs. This matters
     * for tag/alternative ingredients: a greedy first-match can mark the wrong
     * slot missing when two ingredients can consume overlapping item choices.
     */
    private ItemStack[] matchAvailableGhostIngredients() {
        int ingredientCount = ghostPreviewIngredients.size();
        ItemStack[] matched = new ItemStack[ingredientCount];
        Arrays.fill(matched, ItemStack.EMPTY);
        if (ingredientCount == 0 || minecraft == null || minecraft.player == null) {
            return matched;
        }

        List<ItemStack> units = new ArrayList<>();
        for (ItemStack stack : minecraft.player.getInventory().items) {
            addGhostUnits(units, stack, stack.getCount());
        }
        for (int slotIndex = 1; slotIndex <= 9 && slotIndex < menu.slots.size(); slotIndex++) {
            ItemStack stack = menu.getSlot(slotIndex).getItem();
            addGhostUnits(units, stack, stack.getCount());
        }
        for (StorageItemEntry entry : ClientStorageState.entries(menu.containerId)) {
            addGhostUnits(units, entry.stack(), entry.count());
        }

        int[] unitOwner = new int[units.size()];
        Arrays.fill(unitOwner, -1);
        int[] ingredientUnit = new int[ingredientCount];
        Arrays.fill(ingredientUnit, -1);

        for (int ingredientIndex = 0; ingredientIndex < ingredientCount; ingredientIndex++) {
            boolean[] visited = new boolean[units.size()];
            tryMatchGhostIngredient(ingredientIndex, units, unitOwner, ingredientUnit, visited);
        }

        for (int ingredientIndex = 0; ingredientIndex < ingredientCount; ingredientIndex++) {
            int unitIndex = ingredientUnit[ingredientIndex];
            if (unitIndex >= 0) {
                matched[ingredientIndex] = units.get(unitIndex).copyWithCount(1);
            }
        }
        return matched;
    }

    private boolean tryMatchGhostIngredient(
            int ingredientIndex,
            List<ItemStack> units,
            int[] unitOwner,
            int[] ingredientUnit,
            boolean[] visited
    ) {
        Ingredient ingredient = ghostPreviewIngredients.get(ingredientIndex).ingredient();
        for (int unitIndex = 0; unitIndex < units.size(); unitIndex++) {
            if (visited[unitIndex] || !ingredient.test(units.get(unitIndex))) {
                continue;
            }
            visited[unitIndex] = true;

            int previousOwner = unitOwner[unitIndex];
            if (previousOwner == -1
                    || tryMatchGhostIngredient(previousOwner, units, unitOwner, ingredientUnit, visited)) {
                unitOwner[unitIndex] = ingredientIndex;
                ingredientUnit[ingredientIndex] = unitIndex;
                return true;
            }
        }
        return false;
    }

    private static void addGhostUnits(List<ItemStack> units, ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            return;
        }

        // A crafting-table recipe can consume at most nine units in one craft.
        int amount = Math.min(count, 9);
        for (int index = 0; index < amount; index++) {
            units.add(stack.copyWithCount(1));
        }
    }

    private record GhostPreviewIngredient(Ingredient ingredient, int x, int y) {
    }

    private void toggleStoragePanel() {
        if (!recipeTabActive && storagePanel.isOpen()) {
            return;
        }

        recipeTabActive = false;
        rememberedRecipeTabActive = false;
        applyActiveTabState();
    }

    private void toggleRecipeBook() {
        if (recipeTabActive && recipePanel != null && recipePanel.isOpen()) {
            return;
        }

        recipeTabActive = true;
        rememberedRecipeTabActive = true;
        applyActiveTabState();
    }

    private void applyActiveTabState() {
        // The vanilla component remains hidden permanently. It is retained only
        // for vanilla ghost-recipe rendering, never as Craft & Find's recipe UI.
        if (getRecipeBookComponent().isVisible()) {
            setRecipeBookVisible(false);
        }

        if (recipeTabActive) {
            storagePanel.setOpen(false);
            recipePanel.setOpen(true);
        } else {
            recipePanel.setOpen(false);
            storagePanel.setOpen(true);
        }

        leftPos = mainLeftForActiveTab();
        updateCustomPositions();
    }

    private void setRecipeBookVisible(boolean visible) {
        ((RecipeBookComponentAccessor) (Object) getRecipeBookComponent())
                .craftandfind$setVisible(visible);
    }

    private int mainLeftForActiveTab() {
        if (width < 379) {
            return (width - imageWidth) / 2;
        }
        if (!recipeTabActive) {
            return (width - imageWidth) / 2 + 77;
        }

        // This is exactly the old themed vanilla recipe-book geometry. Keeping
        // the formula avoids even a one-pixel parity shift when switching tabs.
        int recipePanelX = (width - WorkbenchLayout.PANEL_WIDTH) / 2 - 86;
        return recipePanelX + WorkbenchLayout.PANEL_WIDTH + WorkbenchLayout.PANEL_GAP;
    }

    private void updateCustomPositions() {
        if (storagePanel == null || recipePanel == null || compassButton == null || customRecipeButton == null) {
            return;
        }

        leftPos = mainLeftForActiveTab();

        int buttonX = leftPos + WorkbenchLayout.TAB_BUTTON_X;
        compassButton.setX(buttonX);
        compassButton.setY(topPos + WorkbenchLayout.TAB_COMPASS_Y);
        customRecipeButton.setX(buttonX);
        customRecipeButton.setY(topPos + WorkbenchLayout.TAB_RECIPE_Y);
        storagePanel.updatePosition(leftPos, width, height);
        recipePanel.updatePosition(leftPos, width, height);
    }

    private void applyResultSlotShift() {
        if (resultSlotShiftApplied || menu.slots.isEmpty()) {
            return;
        }

        Slot resultSlot = menu.getSlot(0);
        SlotPositionAccessor position = (SlotPositionAccessor) resultSlot;
        position.craftandfind$setX(resultSlot.x + WorkbenchLayout.RESULT_SLOT_CONTENT_X_SHIFT);
        position.craftandfind$setY(resultSlot.y + WorkbenchLayout.RESULT_SLOT_CONTENT_Y_SHIFT);
        resultSlotShiftApplied = true;
    }

}
