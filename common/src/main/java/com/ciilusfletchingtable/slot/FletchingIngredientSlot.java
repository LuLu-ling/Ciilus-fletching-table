package com.ciilusfletchingtable.slot;

import com.ciilusfletchingtable.FletchingRecipe;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FletchingIngredientSlot extends Slot {

    public FletchingIngredientSlot(
			Container container,
			int index,
			int x,
			int y
	) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return FletchingRecipe.isIngredient(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

}
