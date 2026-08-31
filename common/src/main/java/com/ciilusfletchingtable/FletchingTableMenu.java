package com.ciilusfletchingtable;

import com.ciilusfletchingtable.slot.FletchingArrowSlot;
import com.ciilusfletchingtable.slot.FletchingIngredientSlot;
import com.ciilusfletchingtable.slot.FletchingPreviewSlot;
import com.ciilusfletchingtable.slot.FletchingResultSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import org.jspecify.annotations.Nullable;

public class FletchingTableMenu extends AbstractContainerMenu {

    public static final int INGREDIENT_SLOT = 0;
    public static final int ARROW_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int PREVIEW_SLOT = 3;

    private static final int INPUT_SLOT_COUNT = 2;
    private static final int MACHINE_SLOT_COUNT = 4;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final SimpleContainer inputInventory = new SimpleContainer(INPUT_SLOT_COUNT);
    private final SimpleContainer resultInventory = new SimpleContainer(1);
    private final FletchingWorkshop workshop;
    private final @Nullable ServerLevel serverLevel;
    private final @Nullable BlockPos tablePosition;

    public FletchingTableMenu(
            int containerId,
            Inventory playerInventory
    ) {
        this(
                containerId,
                playerInventory,
                ContainerLevelAccess.NULL
        );
    }

    public FletchingTableMenu(
            int containerId,
            Inventory playerInventory,
            ContainerLevelAccess access
    ) {
        super(
                CiiluSFletchingTable.FLETCHING_TABLE_MENU.get(),
                containerId
        );
        this.access = access;
        this.serverLevel = playerInventory.player.level() instanceof ServerLevel level
                ? level
                : null;
        this.tablePosition = access.evaluate(
                (level, position) -> position.immutable(),
                null
        );

        var previewInventory = new SimpleContainer(1);
        var data = new SimpleContainerData(FletchingWorkshop.DATA_COUNT);
        this.workshop = new FletchingWorkshop(
                this.inputInventory,
                this.resultInventory,
                previewInventory,
                data
        );

        this.inputInventory.addListener(this::slotsChanged);

        this.addSlot(new FletchingIngredientSlot(
                this.inputInventory,
                INGREDIENT_SLOT,
                27,
                20
        ));
        this.addSlot(new FletchingArrowSlot(
                this.inputInventory,
                ARROW_SLOT,
                76,
                20
        ));
        this.addSlot(new FletchingResultSlot(
                this.resultInventory,
                0,
                134,
                20,
                () -> this.workshop.getAvailableResultCount(
                        this.serverLevel,
                        this.tablePosition
                ),
                (player, count) -> {
                    this.workshop.craftResult(
                            this.serverLevel,
                            this.tablePosition,
                            player,
                            count
                    );
                    this.broadcastChanges();
                }
        ));
        this.addSlot(new FletchingPreviewSlot(previewInventory, 0));

        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        7 + column * 18,
                        88 + row * 18
                ));
            }
        }

        for (var column = 0; column < 9; column++) {
            this.addSlot(new Slot(
                    playerInventory,
                    column,
                    7 + column * 18,
                    146
            ));
        }

        this.addDataSlots(data);
        this.workshop.refreshState(
                this.serverLevel,
                this.tablePosition
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Blocks.FLETCHING_TABLE);
    }

    @Override
    public void clicked(
            int slotId,
            int button,
            ClickType clickType,
            Player player
    ) {
        if (slotId == RESULT_SLOT && !player.level().isClientSide) {
            this.workshop.refreshState(
                    this.serverLevel,
                    this.tablePosition
            );
        }

        super.clicked(
                slotId,
                button,
                clickType,
                player
        );
    }

    @Override
    public void slotsChanged(Container container) {
        if (this.workshop.isUpdating()) {
            return;
        }

        this.workshop.refreshState(
                this.serverLevel,
                this.tablePosition
        );
        super.broadcastChanges();
    }

    @Override
    public void broadcastChanges() {
        this.workshop.refreshState(
                this.serverLevel,
                this.tablePosition
        );
        super.broadcastChanges();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultInventory
                && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0
                || index >= this.slots.size()
                || index == PREVIEW_SLOT
        ) {
            return ItemStack.EMPTY;
        }

        if (index == RESULT_SLOT
                && !player.level().isClientSide
        ) {
            this.workshop.refreshState(
                    this.serverLevel,
                    this.tablePosition
            );
        }

        var slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var source = slot.getItem();
        var original = source.copy();
        if (index == RESULT_SLOT) {
            if (!this.moveItemStackTo(
                    source,
                    PLAYER_INVENTORY_START,
                    HOTBAR_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(source, original);
        } else if (index < RESULT_SLOT) {
            if (!this.moveItemStackTo(
                    source,
                    PLAYER_INVENTORY_START,
                    HOTBAR_END,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (FletchingRecipe.isIngredient(source)) {
            if (!this.moveItemStackTo(
                    source,
                    INGREDIENT_SLOT,
                    INGREDIENT_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (FletchingRecipe.isArrow(source)) {
            if (!this.moveItemStackTo(
                    source,
                    ARROW_SLOT,
                    ARROW_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!this.moveItemStackTo(
                    source,
                    PLAYER_INVENTORY_END,
                    HOTBAR_END,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(
                source,
                PLAYER_INVENTORY_START,
                PLAYER_INVENTORY_END,
                false
        )) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (source.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, source);
        this.broadcastChanges();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            this.clearContainer(player, this.inputInventory);
        }
    }

    public ItemStack getDisplayedResult() {
        return this.workshop.getDisplayedResult();
    }

    public int getDisplayedRemainingOutput() {
        return this.workshop.getDisplayedRemainingOutput();
    }

    public int getDisplayedTotalOutput() {
        return this.workshop.getDisplayedTotalOutput();
    }

}
