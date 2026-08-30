package com.ciilusfletchingtable.slot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

public class FletchingResultSlot extends Slot {

    private final IntSupplier availableCountSupplier;
    private final BiConsumer<Player, Integer> onTakeCallback;
    private int removeCount;

    public FletchingResultSlot(
            Container container,
            int index,
            int x,
            int y,
            IntSupplier availableCountSupplier,
            BiConsumer<Player, Integer> onTakeCallback
    ) {
        super(
                container,
                index,
                x,
                y
        );
        this.availableCountSupplier = availableCountSupplier;
        this.onTakeCallback = onTakeCallback;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.availableCountSupplier.getAsInt() > 0;
    }

    @Override
    public boolean allowModification(Player player) {
        return this.mayPickup(player);
    }

    @Override
    public ItemStack remove(int amount) {
        var allowed = Math.min(amount, this.availableCountSupplier.getAsInt());
        var removed = super.remove(allowed);
        this.removeCount += removed.getCount();
        return removed;
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.removeCount += amount;
    }

    @Override
    protected void onSwapCraft(int amount) {
        this.removeCount += amount;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        var crafted = this.removeCount;
        this.removeCount = 0;
        this.onTakeCallback.accept(player, crafted);
        super.onTake(player, stack);
    }

}
