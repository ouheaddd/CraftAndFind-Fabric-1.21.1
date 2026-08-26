package com.overyourhead.craftandfind.client.gui;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import com.overyourhead.craftandfind.common.network.payload.HighlightRequestPayload;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StoragePanel {
    private static String rememberedSearch = "";
    private static boolean rememberedSortByQuantity;

    public static final int WIDTH = WorkbenchLayout.PANEL_WIDTH;
    public static final int HEIGHT = WorkbenchLayout.PANEL_HEIGHT;

    private final Font font;
    private final int containerId;
    private final EditBox searchBox;
    private final Component searchHint = Component.translatable("gui.craftandfind.search");
    private final Runnable closeWorkbench;
    private final List<StorageItemEntry> filteredEntries = new ArrayList<>();

    private boolean open;
    private int x;
    private int y;
    private float scrollProgress;
    private boolean draggingScrollbar;
    private boolean sortByQuantity;
    private int scrollbarGrabOffset;

    public StoragePanel(Font font, int containerId, Runnable closeWorkbench) {
        this.font = font;
        this.containerId = containerId;
        this.closeWorkbench = closeWorkbench;
        this.searchBox = new EditBox(
                font,
                0,
                0,
                WorkbenchLayout.SEARCH_TEXT_WIDTH,
                WorkbenchLayout.SEARCH_TEXT_HEIGHT,
                searchHint
        );
        this.sortByQuantity = rememberedSortByQuantity;
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setHint(Component.empty());
        this.searchBox.setValue(rememberedSearch);
        this.searchBox.setResponder(value -> {
            rememberedSearch = value;
            rebuildFilter(true);
        });
        this.searchBox.visible = false;
        rebuildFilter(true);
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
        if (!open) {
            return ItemStack.EMPTY;
        }

        StorageItemEntry entry = entryAt(mouseX, mouseY);
        return entry == null ? ItemStack.EMPTY : entry.stack().copy();
    }

    public void toggle() {
        setOpen(!open);
    }

    public void setOpen(boolean open) {
        this.open = open;
        searchBox.visible = open;
        searchBox.setFocused(false);
        if (!open) {
            draggingScrollbar = false;
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

    public void onStorageUpdated() {
        rebuildFilter(false);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!open) {
            return;
        }

        graphics.blit(
                WorkbenchTextures.STORAGE_PANEL,
                x,
                y,
                0,
                0,
                WIDTH,
                HEIGHT,
                WIDTH,
                HEIGHT
        );

        renderSearchBox(graphics, mouseX, mouseY, partialTick);
        renderSortButton(graphics, mouseX, mouseY);

        if (filteredEntries.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.craftandfind.no_items"),
                    x + WIDTH / 2,
                    y + 78,
                    0xFFFFFFFF
            );
            renderSortTooltip(graphics, mouseX, mouseY);
            return;
        }

        int firstIndex = firstVisibleRow() * WorkbenchLayout.STORAGE_COLUMNS;
        int maxVisible = WorkbenchLayout.STORAGE_COLUMNS * WorkbenchLayout.STORAGE_ROWS;
        int endIndex = Math.min(filteredEntries.size(), firstIndex + maxVisible);

        for (int index = firstIndex; index < endIndex; index++) {
            int visibleIndex = index - firstIndex;
            int column = visibleIndex % WorkbenchLayout.STORAGE_COLUMNS;
            int row = visibleIndex / WorkbenchLayout.STORAGE_COLUMNS;
            int itemX = x + WorkbenchLayout.STORAGE_GRID_X + column * WorkbenchLayout.STORAGE_CELL;
            int itemY = y + WorkbenchLayout.STORAGE_GRID_Y + row * WorkbenchLayout.STORAGE_CELL;
            StorageItemEntry entry = filteredEntries.get(index);

            boolean hovered = mouseX >= itemX && mouseX < itemX + WorkbenchLayout.STORAGE_CELL
                    && mouseY >= itemY && mouseY < itemY + WorkbenchLayout.STORAGE_CELL;
            renderSlotFrame(graphics, itemX, itemY, hovered);
            graphics.renderItem(entry.stack(), itemX + 4, itemY + 4);
            graphics.renderItemDecorations(font, entry.stack(), itemX + 4, itemY + 4, null);
            renderCount(graphics, entry, itemX, itemY);
        }

        renderScrollbar(graphics, mouseX, mouseY);

        StorageItemEntry hoveredEntry = entryAt(mouseX, mouseY);
        if (hoveredEntry != null) {
            graphics.renderTooltip(font, hoveredEntry.stack(), mouseX, mouseY);
        } else {
            renderSortTooltip(graphics, mouseX, mouseY);
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

        if (button == 0 && isInsideSortButton(mouseX, mouseY)) {
            sortByQuantity = !sortByQuantity;
            rememberedSortByQuantity = sortByQuantity;
            rebuildFilter(true);
            return true;
        }

        if (button == 0 && handleScrollbarClick(mouseX, mouseY)) {
            return true;
        }

        if (button == 0) {
            StorageItemEntry entry = entryAt(mouseX, mouseY);
            if (entry != null) {
                ClientPlayNetworking.send(new HighlightRequestPayload(containerId, entry.stack()));
                closeWorkbench.run();
                return true;
            }
        }

        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!open || button != 0 || !draggingScrollbar) {
            return false;
        }

        updateScrollFromMouse(mouseY);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!open || mouseX < x || mouseX >= x + WIDTH || mouseY < y || mouseY >= y + HEIGHT) {
            return false;
        }

        if (scrollY < 0) {
            setVisibleRow(firstVisibleRow() + 1);
        } else if (scrollY > 0) {
            setVisibleRow(firstVisibleRow() - 1);
        }
        return true;
    }

    private void renderSearchBox(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation background = searchBox.isFocused()
                ? WorkbenchTextures.SEARCH_BOX_FOCUSED
                : WorkbenchTextures.SEARCH_BOX;
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

    private void renderSlotFrame(GuiGraphics graphics, int itemX, int itemY, boolean hovered) {
        ResourceLocation texture = hovered
                ? WorkbenchTextures.STORAGE_SLOT_HOVERED
                : WorkbenchTextures.STORAGE_SLOT;
        graphics.blit(
                texture,
                itemX,
                itemY,
                0,
                0,
                WorkbenchLayout.STORAGE_CELL,
                WorkbenchLayout.STORAGE_CELL,
                WorkbenchLayout.STORAGE_CELL,
                WorkbenchLayout.STORAGE_CELL
        );
    }

    private void renderCount(GuiGraphics graphics, StorageItemEntry entry, int itemX, int itemY) {
        String text = entry.count() > 99_999 ? "99999+" : Integer.toString(entry.count());
        int textWidth = font.width(text);
        float scale = textWidth <= 16 ? 1.0F : 16.0F / textWidth;

        graphics.pose().pushPose();
        graphics.pose().translate(
                itemX + WorkbenchLayout.STORAGE_CELL - 3.0F,
                itemY + WorkbenchLayout.STORAGE_CELL - 4.0F,
                200.0F
        );
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -textWidth, -8, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    private void renderSortButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int buttonX = x + WorkbenchLayout.SORT_BUTTON_X;
        int buttonY = y + WorkbenchLayout.SORT_BUTTON_Y;
        boolean hovered = isInsideSortButton(mouseX, mouseY);
        ResourceLocation texture = sortByQuantity
                ? WorkbenchTextures.SORT_BUTTON_SELECTED
                : (hovered ? WorkbenchTextures.SORT_BUTTON_HOVERED : WorkbenchTextures.SORT_BUTTON);

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

        String label = sortByQuantity ? "64" : "A";
        int textX = buttonX + (WorkbenchLayout.SORT_BUTTON_WIDTH - font.width(label)) / 2;
        graphics.drawString(font, label, textX, buttonY + 5, 0xFF2A2A2A, false);
    }

    private void renderSortTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isInsideSortButton(mouseX, mouseY)) {
            return;
        }
        Component tooltip = Component.translatable(
                sortByQuantity ? "gui.craftandfind.sort_alphabetical" : "gui.craftandfind.sort_quantity"
        );
        graphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private boolean isInsideSearchBox(double mouseX, double mouseY) {
        int fieldX = x + WorkbenchLayout.SEARCH_BACKGROUND_X;
        int fieldY = y + WorkbenchLayout.SEARCH_BACKGROUND_Y;
        return mouseX >= fieldX && mouseX < fieldX + WorkbenchLayout.SEARCH_BACKGROUND_WIDTH
                && mouseY >= fieldY && mouseY < fieldY + WorkbenchLayout.SEARCH_BACKGROUND_HEIGHT;
    }

    private boolean isInsideSortButton(double mouseX, double mouseY) {
        int buttonX = x + WorkbenchLayout.SORT_BUTTON_X;
        int buttonY = y + WorkbenchLayout.SORT_BUTTON_Y;
        return mouseX >= buttonX && mouseX < buttonX + WorkbenchLayout.SORT_BUTTON_WIDTH
                && mouseY >= buttonY && mouseY < buttonY + WorkbenchLayout.SORT_BUTTON_HEIGHT;
    }

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (maxScrollRows() <= 0) {
            return;
        }

        int trackX = x + WorkbenchLayout.SCROLLBAR_X;
        int trackY = y + WorkbenchLayout.STORAGE_GRID_Y;
        int thumbHeight = scrollbarThumbHeight();
        int thumbY = scrollbarThumbY();
        boolean hovered = mouseX >= trackX && mouseX < trackX + WorkbenchLayout.SCROLLBAR_WIDTH
                && mouseY >= thumbY && mouseY < thumbY + thumbHeight;

        graphics.blit(
                WorkbenchTextures.SCROLLBAR_TRACK,
                trackX,
                trackY,
                0,
                0,
                WorkbenchLayout.SCROLLBAR_WIDTH,
                WorkbenchLayout.STORAGE_GRID_HEIGHT,
                WorkbenchLayout.SCROLLBAR_WIDTH,
                WorkbenchLayout.STORAGE_GRID_HEIGHT
        );
        ResourceLocation thumb = hovered || draggingScrollbar
                ? WorkbenchTextures.SCROLLBAR_THUMB_HOVERED
                : WorkbenchTextures.SCROLLBAR_THUMB;
        graphics.blit(
                thumb,
                trackX,
                thumbY,
                0,
                0,
                WorkbenchLayout.SCROLLBAR_WIDTH,
                thumbHeight,
                WorkbenchLayout.SCROLLBAR_WIDTH,
                thumbHeight
        );
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (maxScrollRows() <= 0) {
            return false;
        }

        int trackX = x + WorkbenchLayout.SCROLLBAR_X;
        int trackY = y + WorkbenchLayout.STORAGE_GRID_Y;
        if (mouseX < trackX || mouseX >= trackX + WorkbenchLayout.SCROLLBAR_WIDTH
                || mouseY < trackY || mouseY >= trackY + WorkbenchLayout.STORAGE_GRID_HEIGHT) {
            return false;
        }

        int thumbY = scrollbarThumbY();
        int thumbHeight = scrollbarThumbHeight();
        if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
            scrollbarGrabOffset = (int) mouseY - thumbY;
        } else {
            scrollbarGrabOffset = thumbHeight / 2;
        }

        draggingScrollbar = true;
        updateScrollFromMouse(mouseY);
        return true;
    }

    private void updateScrollFromMouse(double mouseY) {
        int thumbHeight = scrollbarThumbHeight();
        int movablePixels = WorkbenchLayout.STORAGE_GRID_HEIGHT - thumbHeight;
        if (maxScrollRows() <= 0 || movablePixels <= 0) {
            scrollProgress = 0.0F;
            return;
        }

        double relative = mouseY
                - (y + WorkbenchLayout.STORAGE_GRID_Y)
                - scrollbarGrabOffset;
        relative = Math.max(0.0D, Math.min(movablePixels, relative));
        scrollProgress = (float) (relative / movablePixels);
    }

    private int scrollbarThumbHeight() {
        return 18;
    }

    private int scrollbarThumbY() {
        int movablePixels = WorkbenchLayout.STORAGE_GRID_HEIGHT - scrollbarThumbHeight();
        if (maxScrollRows() <= 0 || movablePixels <= 0) {
            return y + WorkbenchLayout.STORAGE_GRID_Y;
        }
        return y + WorkbenchLayout.STORAGE_GRID_Y + Math.round(movablePixels * scrollProgress);
    }

    private int totalRows() {
        return (filteredEntries.size() + WorkbenchLayout.STORAGE_COLUMNS - 1)
                / WorkbenchLayout.STORAGE_COLUMNS;
    }

    private int maxScrollRows() {
        return Math.max(0, totalRows() - WorkbenchLayout.STORAGE_ROWS);
    }

    private int firstVisibleRow() {
        int maxRows = maxScrollRows();
        if (maxRows <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(maxRows, Math.round(scrollProgress * maxRows)));
    }

    private void setVisibleRow(int row) {
        int maxRows = maxScrollRows();
        if (maxRows <= 0) {
            scrollProgress = 0.0F;
            return;
        }

        int clamped = Math.max(0, Math.min(maxRows, row));
        scrollProgress = (float) clamped / (float) maxRows;
    }

    private StorageItemEntry entryAt(double mouseX, double mouseY) {
        int relativeX = (int) mouseX - (x + WorkbenchLayout.STORAGE_GRID_X);
        int relativeY = (int) mouseY - (y + WorkbenchLayout.STORAGE_GRID_Y);
        if (relativeX < 0 || relativeY < 0) {
            return null;
        }

        int column = relativeX / WorkbenchLayout.STORAGE_CELL;
        int row = relativeY / WorkbenchLayout.STORAGE_CELL;
        if (column >= WorkbenchLayout.STORAGE_COLUMNS || row >= WorkbenchLayout.STORAGE_ROWS) {
            return null;
        }

        int index = (firstVisibleRow() + row) * WorkbenchLayout.STORAGE_COLUMNS + column;
        return index >= 0 && index < filteredEntries.size() ? filteredEntries.get(index) : null;
    }

    private void rebuildFilter(boolean resetScroll) {
        float previousProgress = scrollProgress;
        String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        filteredEntries.clear();

        for (StorageItemEntry entry : ClientStorageState.entries(containerId)) {
            String displayName = entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.stack().getItem());
            String registryName = itemId.toString().toLowerCase(Locale.ROOT);
            String readableRegistryName = registryName.replace('_', ' ');

            if (query.isEmpty()
                    || displayName.contains(query)
                    || registryName.contains(query)
                    || readableRegistryName.contains(query)) {
                filteredEntries.add(entry);
            }
        }

        Comparator<StorageItemEntry> alphabetical = Comparator
                .comparing((StorageItemEntry entry) -> entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT))
                .thenComparing(entry -> BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString());

        if (sortByQuantity) {
            filteredEntries.sort(
                    Comparator.comparingInt(StorageItemEntry::count)
                            .reversed()
                            .thenComparing(alphabetical)
            );
        } else {
            filteredEntries.sort(alphabetical);
        }

        if (resetScroll || maxScrollRows() <= 0) {
            scrollProgress = 0.0F;
            draggingScrollbar = false;
        } else {
            scrollProgress = Math.max(0.0F, Math.min(1.0F, previousProgress));
        }
    }
}
