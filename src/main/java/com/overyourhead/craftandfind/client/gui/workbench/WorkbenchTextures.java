package com.overyourhead.craftandfind.client.gui.workbench;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.resources.ResourceLocation;

/**
 * One resource location per independently redrawable GUI part. Resource packs
 * can theme Craft & Find deliberately through the craftandfind namespace,
 * while ordinary vanilla GUI packs no longer replace these textures.
 */
public final class WorkbenchTextures {
    public static final ResourceLocation MAIN_PANEL = texture("workbench_panel.png");
    public static final ResourceLocation CRAFTING_SLOT = texture("crafting_slot.png");
    public static final ResourceLocation INVENTORY_SLOT = texture("inventory_slot.png");
    public static final ResourceLocation RESULT_SLOT = texture("result_slot.png");
    public static final ResourceLocation CRAFTING_ARROW = texture("crafting_arrow.png");
    public static final ResourceLocation STORAGE_PANEL = texture("storage_panel.png");
    public static final ResourceLocation RECIPE_PANEL = texture("recipe_panel.png");

    public static final ResourceLocation SEARCH_BOX = texture("search_box.png");
    public static final ResourceLocation SEARCH_BOX_FOCUSED = texture("search_box_focused.png");
    public static final ResourceLocation SEARCH_ICON = texture("icons/search.png");

    public static final ResourceLocation RECIPE_SEARCH_BOX = texture("recipe_search_box.png");
    public static final ResourceLocation RECIPE_SEARCH_BOX_FOCUSED = texture("recipe_search_box_focused.png");
    public static final ResourceLocation RECIPE_FILTER_BUTTON = texture("recipe_filter_button.png");
    public static final ResourceLocation RECIPE_FILTER_BUTTON_HOVERED = texture("recipe_filter_button_hovered.png");
    public static final ResourceLocation RECIPE_FILTER_BUTTON_SELECTED = texture("recipe_filter_button_selected.png");

    public static final ResourceLocation SORT_BUTTON = texture("sort_button.png");
    public static final ResourceLocation SORT_BUTTON_HOVERED = texture("sort_button_hovered.png");
    public static final ResourceLocation SORT_BUTTON_SELECTED = texture("sort_button_selected.png");

    public static final ResourceLocation STORAGE_SLOT = texture("storage_slot.png");
    public static final ResourceLocation STORAGE_SLOT_HOVERED = texture("storage_slot_hovered.png");

    public static final ResourceLocation SCROLLBAR_TRACK = texture("scrollbar_track.png");
    public static final ResourceLocation SCROLLBAR_THUMB = texture("scrollbar_thumb.png");
    public static final ResourceLocation SCROLLBAR_THUMB_HOVERED = texture("scrollbar_thumb_hovered.png");

    public static final ResourceLocation TAB_BUTTON = texture("tab_button.png");
    public static final ResourceLocation TAB_BUTTON_HOVERED = texture("tab_button_hovered.png");
    public static final ResourceLocation TAB_BUTTON_SELECTED = texture("tab_button_selected.png");
    public static final ResourceLocation COMPASS_ICON = texture("icons/compass.png");
    public static final ResourceLocation RECIPE_BOOK_ICON = texture("icons/recipe_book.png");

    // Recipe-book texture files and GUI-atlas sprites used by our screen.
    public static final ResourceLocation RECIPE_SLOT = texture("recipe_slot.png");
    public static final ResourceLocation RECIPE_SLOT_AVAILABLE = texture("recipe_slot_available.png");
    public static final ResourceLocation RECIPE_SLOT_UNAVAILABLE = texture("recipe_slot_unavailable.png");
    public static final ResourceLocation RECIPE_SLOT_SELECTED = texture("recipe_slot_selected.png");
    public static final ResourceLocation PAGE_NEXT = texture("page_next.png");
    public static final ResourceLocation PAGE_NEXT_HOVERED = texture("page_next_hovered.png");
    public static final ResourceLocation PAGE_PREVIOUS = texture("page_previous.png");
    public static final ResourceLocation PAGE_PREVIOUS_HOVERED = texture("page_previous_hovered.png");
    public static final ResourceLocation CATEGORY_TAB = texture("category_tab.png");
    public static final ResourceLocation CATEGORY_TAB_SELECTED = texture("category_tab_selected.png");


    // GUI-atlas sprite IDs used by vanilla recipe widgets inside our screen.
    public static final ResourceLocation RECIPE_FILTER_BUTTON_SPRITE = sprite("recipe_filter_button");
    public static final ResourceLocation RECIPE_FILTER_BUTTON_HOVERED_SPRITE = sprite("recipe_filter_button_hovered");
    public static final ResourceLocation RECIPE_FILTER_BUTTON_SELECTED_SPRITE = sprite("recipe_filter_button_selected");
    public static final ResourceLocation RECIPE_SLOT_AVAILABLE_SPRITE = sprite("recipe_slot_available");
    public static final ResourceLocation RECIPE_SLOT_UNAVAILABLE_SPRITE = sprite("recipe_slot_unavailable");
    public static final ResourceLocation RECIPE_SLOT_MANY_AVAILABLE_SPRITE = sprite("recipe_slot_many_available");
    public static final ResourceLocation RECIPE_SLOT_MANY_UNAVAILABLE_SPRITE = sprite("recipe_slot_many_unavailable");
    public static final ResourceLocation CATEGORY_TAB_SPRITE = sprite("category_tab");
    public static final ResourceLocation CATEGORY_TAB_SELECTED_SPRITE = sprite("category_tab_selected");
    public static final ResourceLocation PAGE_NEXT_SPRITE = sprite("page_next");
    public static final ResourceLocation PAGE_NEXT_HOVERED_SPRITE = sprite("page_next_hovered");
    public static final ResourceLocation PAGE_PREVIOUS_SPRITE = sprite("page_previous");
    public static final ResourceLocation PAGE_PREVIOUS_HOVERED_SPRITE = sprite("page_previous_hovered");

    private WorkbenchTextures() {
    }

    private static ResourceLocation texture(String file) {
        return ResourceLocation.fromNamespaceAndPath(
                CraftAndFindMod.MOD_ID,
                "textures/gui/workbench/" + file
        );
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                CraftAndFindMod.MOD_ID,
                "workbench/" + name
        );
    }
}
