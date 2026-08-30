package com.ciilusfletchingtable.slot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FletchingPreviewSlot extends Slot {

    public FletchingPreviewSlot(
			Container container,
			int index
	) {
        super(container, index, 0, 0);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

}
