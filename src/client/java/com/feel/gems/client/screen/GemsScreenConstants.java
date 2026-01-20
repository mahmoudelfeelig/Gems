package com.feel.gems.client.screen;

/**
 * Shared UI constants for consistent look and feel across all Gems screens.
 */
public final class GemsScreenConstants {
    private GemsScreenConstants() {
    }

    // Panel dimensions
    public static final int PANEL_WIDTH = 300;
    public static final int PANEL_MIN_MARGIN = 32;

    // Button dimensions
    public static final int BUTTON_HEIGHT = 20;
    public static final int ENTRY_HEIGHT = 24;
    public static final int SPACING = 4;

    // Tab dimensions
    public static final int TAB_WIDTH = 90;
    public static final int TAB_HEIGHT = 20;
    public static final int TAB_GAP = 4;

    // Pagination
    public static final int ENTRIES_PER_PAGE = 8;
    public static final int NAV_BUTTON_WIDTH = 70;
    public static final int CLOSE_BUTTON_WIDTH = 100;

    // Vertical layout
    public static final int TITLE_Y = 12;
    public static final int SUBTITLE_Y = 24;
    public static final int CONTENT_START_Y = 40;
    public static final int BOTTOM_MARGIN = 25;
    public static final int NAV_MARGIN = 50;

    // Colors
    public static final int COLOR_WHITE = 0xFFFFFFFF;
    public static final int COLOR_GRAY = 0xFFA0A0A0;
    public static final int COLOR_DARK_GRAY = 0xFF606060;
    public static final int COLOR_PANEL_BG = 0xAA101010;
    public static final int COLOR_PANEL_BORDER = 0x44FFFFFF;

    /**
     * Calculate a panel width that fits within the screen bounds.
     */
    public static int panelWidth(int screenWidth) {
        return Math.min(PANEL_WIDTH, screenWidth - PANEL_MIN_MARGIN);
    }

    /**
     * Calculate the left edge of a centered panel.
     */
    public static int panelLeft(int screenWidth) {
        return (screenWidth - panelWidth(screenWidth)) / 2;
    }

    /**
     * Calculate the Y position for the close button.
     */
    public static int closeButtonY(int screenHeight) {
        return screenHeight - BOTTOM_MARGIN;
    }

    /**
     * Calculate the Y position for navigation buttons (prev/next).
     */
    public static int navButtonY(int screenHeight) {
        return screenHeight - NAV_MARGIN;
    }

    /**
     * Calculate total pages for pagination.
     */
    public static int totalPages(int entryCount, int entriesPerPage) {
        return Math.max(1, (entryCount + entriesPerPage - 1) / entriesPerPage);
    }

    /**
     * Clamp page number to valid range.
     */
    public static int clampPage(int page, int totalPages) {
        return Math.max(0, Math.min(page, totalPages - 1));
    }
}
