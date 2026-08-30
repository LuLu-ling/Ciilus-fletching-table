package com.ciilusfletchingtable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

public class FletchingWorkshop {

    public static final int INGREDIENT_SLOT = 0;
    public static final int ARROW_SLOT = 1;

    public static final int DATA_REMAINING = 0;
    public static final int DATA_TOTAL = 1;
    public static final int DATA_COUNT = 2;

    private final Container inputInventory;
    private final Container resultInventory;
    private final Container previewInventory;
    private final ContainerData data;
    private boolean updating;

    public FletchingWorkshop(
            Container inputInventory,
            Container resultInventory,
            Container previewInventory,
            ContainerData data
    ) {
        this.inputInventory = inputInventory;
        this.resultInventory = resultInventory;
        this.previewInventory = previewInventory;
        this.data = data;
    }

    public void refreshState(
            @Nullable ServerLevel serverLevel,
            @Nullable BlockPos tablePosition
    ) {
        if (serverLevel == null
                || tablePosition == null
                || this.updating
        ) {
            return;
        }

        this.updating = true;
        try {
            var state = FletchingTableChargeState.getIfPresent(serverLevel);
            var charge = state == null
                    ? null
                    : state.getCharge(tablePosition);

            this.syncChargeState(charge);
            this.updateResultPreview(charge);
        } finally {
            this.updating = false;
        }
    }

    public void syncChargeState(FletchingTableChargeState.@Nullable Charge charge) {
        var preview = charge == null
                ? ItemStack.EMPTY
                : charge.result();
        if (!ItemStack.matches(this.previewInventory.getItem(0), preview)) {
            this.previewInventory.setItem(0, preview);
        }

        this.data.set(
                DATA_REMAINING,
                charge == null
                        ? 0
                        : charge.remaining()
        );
        this.data.set(
                DATA_TOTAL,
                charge == null
                        ? 0
                        : charge.total()
        );
    }

    public void updateResultPreview(FletchingTableChargeState.@Nullable Charge charge) {
        var result = ItemStack.EMPTY;
        var arrows = this.inputInventory.getItem(ARROW_SLOT);
        var template = this.getPreviewTemplate(charge);
        var available = this.getPreviewCapacity(charge);
        if (!template.isEmpty() && FletchingRecipe.isArrow(arrows) && !arrows.isEmpty()) {
            var count = Math.min(
                    available,
                    Math.min(arrows.getCount(), template.getMaxStackSize())
            );
            if (count > 0) {
                result = template.copyWithCount(count);
            }
        }

        if (!ItemStack.matches(this.resultInventory.getItem(0), result)) {
            this.resultInventory.setItem(0, result);
        }
    }

    public int getAvailableResultCount(
            @Nullable ServerLevel serverLevel,
            @Nullable BlockPos tablePosition
    ) {
        var result = this.resultInventory.getItem(0);
        if (result.isEmpty()) {
            return 0;
        }

        if (serverLevel == null || tablePosition == null) {
            return result.getCount();
        }

        var state = FletchingTableChargeState.getIfPresent(serverLevel);
        var charge = state == null
                ? null
                : state.getCharge(tablePosition);
        var arrows = this.inputInventory.getItem(ARROW_SLOT);
        var template = this.getPreviewTemplate(charge);
        var available = this.getPreviewCapacity(charge);
        if (template.isEmpty() || !FletchingRecipe.isArrow(arrows)) {
            return 0;
        }

        if (!ItemStack.isSameItemSameComponents(result, template)) {
            return 0;
        }

        return Math.min(result.getCount(), Math.min(available, arrows.getCount()));
    }

    public int craftResult(
            @Nullable ServerLevel serverLevel,
            @Nullable BlockPos tablePosition,
            Player player,
            int requested
    ) {
        if (requested <= 0
                || serverLevel == null
                || tablePosition == null
        ) {
            return 0;
        }

        var state = FletchingTableChargeState.getIfPresent(serverLevel);
        var charge = state == null
                ? null
                : state.getCharge(tablePosition);
        var arrows = this.inputInventory.getItem(ARROW_SLOT);
        var template = this.getPreviewTemplate(charge);
        var available = this.getPreviewCapacity(charge);
        if (template.isEmpty() || !FletchingRecipe.isArrow(arrows)) {
            this.refreshState(serverLevel, tablePosition);
            return 0;
        }

        var allowed = Math.min(
                requested,
                Math.min(
                        available,
                        arrows.getCount()
                )
        );
        if (allowed <= 0) {
            this.refreshState(serverLevel, tablePosition);
            return 0;
        }

        var craftedCount = 0;
        this.updating = true;
        try {
            if (charge == null) {
                var committedIngredient = this.inputInventory.removeItem(INGREDIENT_SLOT, 1);
                if (!committedIngredient.isEmpty()) {
                    state = FletchingTableChargeState.get(serverLevel);
                    state.loadCharge(tablePosition, template, available);
                    charge = state.getCharge(tablePosition);
                }
            }

            if (state != null && charge != null) {
                var consumed = state.consume(tablePosition, allowed);
                if (consumed > 0) {
                    this.inputInventory.removeItem(ARROW_SLOT, consumed);
                    var crafted = template.copyWithCount(consumed);
                    crafted.onCraftedBy(serverLevel, player, consumed);
                    craftedCount = consumed;
                }
            }
        } finally {
            this.updating = false;
        }

        this.refreshState(serverLevel, tablePosition);
        return craftedCount;
    }

    public ItemStack getPreviewTemplate(FletchingTableChargeState.@Nullable Charge charge) {
        if (charge != null) {
            return charge.result();
        }

        var ingredient = this.inputInventory.getItem(INGREDIENT_SLOT);
        return FletchingRecipe.isIngredient(ingredient)
                ? FletchingRecipe.createOutput(ingredient)
                : ItemStack.EMPTY;
    }

    public int getPreviewCapacity(FletchingTableChargeState.@Nullable Charge charge) {
        return charge != null
                ? charge.remaining()
                : FletchingRecipe.getMaximumOutput(this.inputInventory.getItem(INGREDIENT_SLOT));
    }

    public ItemStack getIngredient() {
        return this.inputInventory.getItem(INGREDIENT_SLOT);
    }

    public ItemStack getResult() {
        return this.resultInventory.getItem(0);
    }

    public boolean hasActiveCharge() {
        return this.getRemainingOutput() > 0 && !this.getActiveResult().isEmpty();
    }

    public ItemStack getActiveResult() {
        return this.previewInventory.getItem(0);
    }

    public ItemStack getDisplayedResult() {
        if (this.hasActiveCharge()) {
            return this.getActiveResult();
        }

        var ingredient = this.getIngredient();
        return FletchingRecipe.isIngredient(ingredient)
                ? FletchingRecipe.createOutput(ingredient)
                : ItemStack.EMPTY;
    }

    public int getRemainingOutput() {
        return this.data.get(DATA_REMAINING);
    }

    public int getTotalOutput() {
        return this.data.get(DATA_TOTAL);
    }

    public int getDisplayedRemainingOutput() {
        return this.hasActiveCharge()
                ? this.getRemainingOutput()
                : FletchingRecipe.getMaximumOutput(this.getIngredient());
    }

    public int getDisplayedTotalOutput() {
        return this.hasActiveCharge()
                ? this.getTotalOutput()
                : FletchingRecipe.getMaximumOutput(this.getIngredient());
    }

    public boolean isUpdating() {
        return this.updating;
    }

}
