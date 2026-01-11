package com.feel.gems.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Standardized tooltip colors for consistent UI across all Gems items.
 * 
 * Color usage guidelines:
 * - DESCRIPTION (GRAY): Primary item description, main functionality
 * - DETAIL (DARK_GRAY): Secondary details, usage instructions, mechanics
 * - HIGHLIGHT (GOLD): Important features, special effects, legendary qualities
 * - SECTION (GRAY): Section headers in tooltips
 * - LIST_ITEM (DARK_GRAY): Individual items in lists (passives, abilities)
 * - INSTRUCTION (AQUA): Keybinds, controls, how-to-use info
 * - SUB_ITEM (DARK_AQUA): Sub-items, detailed values, statistics
 * - ERROR (RED): Warnings, disabled features, cooldowns
 * - SUCCESS (GREEN): Active effects, enabled features
 */
public final class TooltipColors {
    /** Primary description color - main functionality */
    public static final Formatting DESCRIPTION = Formatting.GRAY;
    
    /** Secondary details color - usage instructions, mechanics */
    public static final Formatting DETAIL = Formatting.DARK_GRAY;
    
    /** Important feature highlight color - special effects, legendary qualities */
    public static final Formatting HIGHLIGHT = Formatting.GOLD;
    
    /** Section header color */
    public static final Formatting SECTION = Formatting.GRAY;
    
    /** List item color - passives, abilities in lists */
    public static final Formatting LIST_ITEM = Formatting.DARK_GRAY;
    
    /** Instruction color - keybinds, controls */
    public static final Formatting INSTRUCTION = Formatting.AQUA;
    
    /** Sub-item color - detailed values, statistics */
    public static final Formatting SUB_ITEM = Formatting.DARK_AQUA;
    
    /** Error/warning color */
    public static final Formatting ERROR = Formatting.RED;
    
    /** Success/active color */
    public static final Formatting SUCCESS = Formatting.GREEN;
    
    private TooltipColors() {
    }
    
    /**
     * Format text as primary description.
     */
    public static MutableText description(String key) {
        return Text.translatable(key).formatted(DESCRIPTION);
    }
    
    /**
     * Format text as secondary detail.
     */
    public static MutableText detail(String key) {
        return Text.translatable(key).formatted(DETAIL);
    }
    
    /**
     * Format text as important highlight.
     */
    public static MutableText highlight(String key) {
        return Text.translatable(key).formatted(HIGHLIGHT);
    }
    
    /**
     * Format text as section header.
     */
    public static MutableText section(String key) {
        return Text.translatable(key).formatted(SECTION);
    }
    
    /**
     * Format text as instruction.
     */
    public static MutableText instruction(String key) {
        return Text.translatable(key).formatted(INSTRUCTION);
    }
    
    /**
     * Format literal text as list item with prefix.
     */
    public static MutableText listItem(String text) {
        return Text.literal(" - " + text).formatted(LIST_ITEM);
    }
    
    /**
     * Format literal text as sub-item with prefix.
     */
    public static MutableText subItem(String text) {
        return Text.literal(" - " + text).formatted(SUB_ITEM);
    }
}
