package com.ciilusfletchingtable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import org.jetbrains.annotations.Nullable;

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

	private static final int DATA_REMAINING = 0;
	private static final int DATA_TOTAL = 1;
	private static final int DATA_COUNT = 2;

	private final ContainerLevelAccess access;
	private final SimpleContainer inputInventory = new SimpleContainer(INPUT_SLOT_COUNT);
	private final SimpleContainer resultInventory = new SimpleContainer(1);
	private final SimpleContainer previewInventory = new SimpleContainer(1);
	private final SimpleContainerData data = new SimpleContainerData(DATA_COUNT);
	@Nullable
	private final ServerLevel serverLevel;
	@Nullable
	private final BlockPos tablePosition;
	private boolean updatingMachine;

	public FletchingTableMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, ContainerLevelAccess.NULL);
	}

	public FletchingTableMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
		super(CiiluSFletchingTable.FLETCHING_TABLE_MENU, containerId);
		this.access = access;
		this.serverLevel = playerInventory.player.level() instanceof ServerLevel level ? level : null;
		this.tablePosition = access.evaluate((level, position) -> position.immutable(), null);
		this.inputInventory.addListener(this::slotsChanged);

		this.addSlot(new IngredientSlot(this.inputInventory, INGREDIENT_SLOT, 27, 20));
		this.addSlot(new ArrowSlot(this.inputInventory, ARROW_SLOT, 76, 20));
		this.addSlot(new OutputSlot(this.resultInventory, 0, 134, 20));
		this.addSlot(new PreviewSlot(this.previewInventory, 0));

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 7 + column * 18, 88 + row * 18));
			}
		}

		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(playerInventory, column, 7 + column * 18, 146));
		}

		this.addDataSlots(this.data);
		this.refreshMachineState();
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, Blocks.FLETCHING_TABLE);
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (slotId == RESULT_SLOT && !player.level().isClientSide) {
			this.refreshMachineState();
		}

		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public void slotsChanged(Container container) {
		if (this.updatingMachine) {
			return;
		}

		this.refreshMachineState();
		super.broadcastChanges();
	}

	@Override
	public void broadcastChanges() {
		this.refreshMachineState();
		super.broadcastChanges();
	}

	private void refreshMachineState() {
		if (this.serverLevel == null || this.tablePosition == null || this.updatingMachine) {
			return;
		}

		this.updatingMachine = true;
		try {
			FletchingTableChargeState state = FletchingTableChargeState.getIfPresent(this.serverLevel);
			FletchingTableChargeState.Charge charge = state == null ? null : state.getCharge(this.tablePosition);

			this.syncChargeState(charge);
			this.updateResultPreview(charge);
		} finally {
			this.updatingMachine = false;
		}
	}

	private void syncChargeState(@Nullable FletchingTableChargeState.Charge charge) {
		ItemStack preview = charge == null ? ItemStack.EMPTY : charge.result();
		if (!ItemStack.matches(this.previewInventory.getItem(0), preview)) {
			this.previewInventory.setItem(0, preview);
		}

		this.data.set(DATA_REMAINING, charge == null ? 0 : charge.remaining());
		this.data.set(DATA_TOTAL, charge == null ? 0 : charge.total());
	}

	private void updateResultPreview(@Nullable FletchingTableChargeState.Charge charge) {
		ItemStack result = ItemStack.EMPTY;
		ItemStack arrows = this.inputInventory.getItem(ARROW_SLOT);
		ItemStack template = this.getPreviewTemplate(charge);
		int available = this.getPreviewCapacity(charge);
		if (!template.isEmpty() && FletchingRecipe.isArrow(arrows) && !arrows.isEmpty()) {
			int count = Math.min(available, Math.min(arrows.getCount(), template.getMaxStackSize()));
			if (count > 0) {
				result = template.copyWithCount(count);
			}
		}

		if (!ItemStack.matches(this.resultInventory.getItem(0), result)) {
			this.resultInventory.setItem(0, result);
		}
	}

	@Nullable
	private FletchingTableChargeState.Charge getServerCharge() {
		if (this.serverLevel == null || this.tablePosition == null) {
			return null;
		}

		FletchingTableChargeState state = FletchingTableChargeState.getIfPresent(this.serverLevel);
		return state == null ? null : state.getCharge(this.tablePosition);
	}

	private int getAvailableResultCount() {
		ItemStack result = this.resultInventory.getItem(0);
		if (result.isEmpty()) {
			return 0;
		}

		if (this.serverLevel == null || this.tablePosition == null) {
			return result.getCount();
		}

		FletchingTableChargeState.Charge charge = this.getServerCharge();
		ItemStack arrows = this.inputInventory.getItem(ARROW_SLOT);
		ItemStack template = this.getPreviewTemplate(charge);
		int available = this.getPreviewCapacity(charge);
		if (template.isEmpty() || !FletchingRecipe.isArrow(arrows)) {
			return 0;
		}

		if (!ItemStack.isSameItemSameTags(result, template)) {
			return 0;
		}

		return Math.min(result.getCount(), Math.min(available, arrows.getCount()));
	}

	private void takeResult(Player player, int requested) {
		if (requested <= 0 || this.serverLevel == null || this.tablePosition == null) {
			return;
		}

		FletchingTableChargeState state = FletchingTableChargeState.getIfPresent(this.serverLevel);
		FletchingTableChargeState.Charge charge = state == null ? null : state.getCharge(this.tablePosition);
		ItemStack arrows = this.inputInventory.getItem(ARROW_SLOT);
		ItemStack template = this.getPreviewTemplate(charge);
		int available = this.getPreviewCapacity(charge);
		if (template.isEmpty() || !FletchingRecipe.isArrow(arrows)) {
			this.refreshMachineState();
			return;
		}

		int allowed = Math.min(requested, Math.min(available, arrows.getCount()));
		if (allowed <= 0) {
			this.refreshMachineState();
			return;
		}

		this.updatingMachine = true;
		try {
			if (charge == null) {
				ItemStack committedIngredient = this.inputInventory.removeItem(INGREDIENT_SLOT, 1);
				if (!committedIngredient.isEmpty()) {
					state = FletchingTableChargeState.get(this.serverLevel);
					state.loadCharge(this.tablePosition, template, available);
					charge = state.getCharge(this.tablePosition);
				}
			}

			if (state != null && charge != null) {
				int consumed = state.consume(this.tablePosition, allowed);
				if (consumed > 0) {
					this.inputInventory.removeItem(ARROW_SLOT, consumed);
					ItemStack crafted = template.copyWithCount(consumed);
					crafted.onCraftedBy(this.serverLevel, player, consumed);
				}
			}
		} finally {
			this.updatingMachine = false;
		}

		this.refreshMachineState();
		this.broadcastChanges();
	}

	private ItemStack getPreviewTemplate(@Nullable FletchingTableChargeState.Charge charge) {
		if (charge != null) {
			return charge.result();
		}

		ItemStack ingredient = this.inputInventory.getItem(INGREDIENT_SLOT);
		return FletchingRecipe.isIngredient(ingredient) ? FletchingRecipe.createOutput(ingredient) : ItemStack.EMPTY;
	}

	private int getPreviewCapacity(@Nullable FletchingTableChargeState.Charge charge) {
		if (charge != null) {
			return charge.remaining();
		}

		return FletchingRecipe.getMaximumOutput(this.inputInventory.getItem(INGREDIENT_SLOT));
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.container != this.resultInventory && super.canTakeItemForPickAll(stack, slot);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= this.slots.size() || index == PREVIEW_SLOT) {
			return ItemStack.EMPTY;
		}

		if (index == RESULT_SLOT && !player.level().isClientSide) {
			this.refreshMachineState();
		}

		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack source = slot.getItem();
		ItemStack original = source.copy();
		if (index == RESULT_SLOT) {
			if (!this.moveItemStackTo(source, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(source, original);
		} else if (index < RESULT_SLOT) {
			if (!this.moveItemStackTo(source, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (FletchingRecipe.isIngredient(source)) {
			if (!this.moveItemStackTo(source, INGREDIENT_SLOT, INGREDIENT_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (FletchingRecipe.isArrow(source)) {
			if (!this.moveItemStackTo(source, ARROW_SLOT, ARROW_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (index < PLAYER_INVENTORY_END) {
			if (!this.moveItemStackTo(source, PLAYER_INVENTORY_END, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(source, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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

		ItemStack ingredient = this.getIngredient();
		return FletchingRecipe.isIngredient(ingredient) ? FletchingRecipe.createOutput(ingredient) : ItemStack.EMPTY;
	}

	public int getRemainingOutput() {
		return this.data.get(DATA_REMAINING);
	}

	public int getTotalOutput() {
		return this.data.get(DATA_TOTAL);
	}

	public int getDisplayedRemainingOutput() {
		if (this.hasActiveCharge()) {
			return this.getRemainingOutput();
		}

		return FletchingRecipe.getMaximumOutput(this.getIngredient());
	}

	public int getDisplayedTotalOutput() {
		if (this.hasActiveCharge()) {
			return this.getTotalOutput();
		}

		return FletchingRecipe.getMaximumOutput(this.getIngredient());
	}

	private static final class IngredientSlot extends Slot {
		private IngredientSlot(Container container, int index, int x, int y) {
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

	private static final class ArrowSlot extends Slot {
		private ArrowSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return FletchingRecipe.isArrow(stack);
		}
	}

	private final class OutputSlot extends Slot {
		private int removeCount;

		private OutputSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return FletchingTableMenu.this.getAvailableResultCount() > 0;
		}

		@Override
		public boolean allowModification(Player player) {
			return this.mayPickup(player);
		}

		@Override
		public ItemStack remove(int amount) {
			int allowed = Math.min(amount, FletchingTableMenu.this.getAvailableResultCount());
			ItemStack removed = super.remove(allowed);
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
			int crafted = this.removeCount;
			this.removeCount = 0;
			FletchingTableMenu.this.takeResult(player, crafted);
			super.onTake(player, stack);
		}
	}

	private static final class PreviewSlot extends Slot {
		private PreviewSlot(Container container, int index) {
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
}
