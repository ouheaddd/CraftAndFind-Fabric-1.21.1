package com.overyourhead.craftandfind.client.gui.workbench;

/**
 * All pixel positions for the Craft & Find workbench GUI live here. Editing
 * this class changes layout without touching interaction or rendering logic.
 */
public final class WorkbenchLayout {
    public static final int MAIN_WIDTH = 176;
    public static final int MAIN_HEIGHT = 166;

    public static final int CRAFTING_ARROW_X = 88;
    public static final int CRAFTING_ARROW_Y = 36;
    public static final int CRAFTING_ARROW_WIDTH = 22;
    public static final int CRAFTING_ARROW_HEIGHT = 15;

    public static final int NORMAL_SLOT_SIZE = 18;
    public static final int RESULT_SLOT_SIZE = 26;
    public static final int NORMAL_SLOT_OFFSET = 1;
    public static final int RESULT_SLOT_OFFSET = 5;
    public static final int RESULT_SLOT_Y_ADJUST = 0;
    public static final int RESULT_SLOT_X_ADJUST = 0;
    public static final int RESULT_SLOT_CONTENT_X_SHIFT = -1;
    public static final int RESULT_SLOT_CONTENT_Y_SHIFT = 0;

    public static final int PANEL_WIDTH = 147;
    public static final int PANEL_HEIGHT = 166;
    public static final int PANEL_GAP = 2;

    public static final int SEARCH_BACKGROUND_X = 25;
    public static final int SEARCH_BACKGROUND_Y = 7;
    public static final int SEARCH_BACKGROUND_WIDTH = 87;
    public static final int SEARCH_BACKGROUND_HEIGHT = 18;
    public static final int SEARCH_TEXT_X = 30;
    public static final int SEARCH_TEXT_Y = 11;
    public static final int SEARCH_TEXT_WIDTH = 77;
    public static final int SEARCH_TEXT_HEIGHT = 12;
    public static final int SEARCH_ICON_X = 7;
    public static final int SEARCH_ICON_Y = 8;

    public static final int SORT_BUTTON_X = 114;
    public static final int SORT_BUTTON_Y = 7;
    public static final int SORT_BUTTON_WIDTH = 20;
    public static final int SORT_BUTTON_HEIGHT = 18;

    public static final int STORAGE_COLUMNS = 5;
    public static final int STORAGE_ROWS = 5;
    public static final int STORAGE_CELL = 25;
    public static final int STORAGE_GRID_X = 8;
    public static final int STORAGE_GRID_Y = 33;
    public static final int STORAGE_GRID_HEIGHT = STORAGE_ROWS * STORAGE_CELL;

    public static final int SCROLLBAR_X = 133;
    public static final int SCROLLBAR_WIDTH = 6;

    public static final int TAB_BUTTON_X = 8;
    public static final int TAB_COMPASS_Y = 27;
    public static final int TAB_RECIPE_Y = 48;
    public static final int TAB_WIDTH = 20;
    public static final int TAB_HEIGHT = 18;

    public static final int RECIPE_BOOK_ROW_X_OFFSET = 0;
    public static final int RECIPE_BOOK_SEARCH_SHIFT_X = 1;
    public static final int RECIPE_BOOK_SEARCH_SHIFT_Y = -2;
    public static final int RECIPE_BOOK_SEARCH_TEXT_Y_OFFSET = 1;
    public static final int RECIPE_BOOK_FILTER_SHIFT_Y = -2;
    public static final int RECIPE_BOOK_ICON_X_FROM_SEARCH = -19;
    public static final int RECIPE_BOOK_ICON_Y_FROM_SEARCH = -1;
    public static final int RECIPE_BOOK_BUTTONS_Y_OFFSET = 3;
    public static final int RECIPE_BOOK_PAGE_CONTROLS_Y_OFFSET = 2;
    public static final int RECIPE_BOOK_PAGE_TEXT_Y_OFFSET = 3;

    public static final int CRAFTING_LABEL_X_OFFSET = 7;
    public static final int LABEL_COLOR = 0xFFE6D5AC;

    private WorkbenchLayout() {
    }
}
