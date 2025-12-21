package com.feel.gems.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GemItemGlintTest {

    @Disabled("Requires Minecraft runtime; covered by gametest glintAppliesOnlyToActiveGemAtCap")
    @Test
    void setGlintFlagTogglesComponent() throws Exception {
        ItemStack stack = new ItemStack(new Item(new Item.Settings()));

        Method setter = GemItemGlint.class.getDeclaredMethod("setGlintFlag", ItemStack.class, boolean.class);
        setter.setAccessible(true);

        setter.invoke(null, stack, true);
        assertTrue(stack.contains(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE));

        setter.invoke(null, stack, false);
        assertFalse(stack.contains(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE));
    }
}
