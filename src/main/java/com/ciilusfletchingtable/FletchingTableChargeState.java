package com.ciilusfletchingtable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;

public final class FletchingTableChargeState extends SavedData {
	private static final String DATA_NAME = CiiluSFletchingTable.MOD_ID + "_charges";
	private static final String TAG_CHARGES = "Charges";
	private static final String TAG_POSITION = "Position";
	private static final String TAG_REMAINING = "Remaining";
	private static final String TAG_TOTAL = "Total";
	private static final String TAG_RESULT = "Result";

	private final Map<Long, Charge> charges = new HashMap<>();

	public static FletchingTableChargeState get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(
			FletchingTableChargeState::load,
			FletchingTableChargeState::new,
			DATA_NAME
		);
	}

	@Nullable
	public static FletchingTableChargeState getIfPresent(ServerLevel level) {
		return level.getDataStorage().get(FletchingTableChargeState::load, DATA_NAME);
	}

	static FletchingTableChargeState load(CompoundTag root) {
		FletchingTableChargeState state = new FletchingTableChargeState();
		ListTag entries = root.getList(TAG_CHARGES, Tag.TAG_COMPOUND);

		for (int index = 0; index < entries.size(); index++) {
			CompoundTag entry = entries.getCompound(index);
			int remaining = entry.getInt(TAG_REMAINING);
			int total = Math.max(remaining, entry.getInt(TAG_TOTAL));
			ItemStack result = ItemStack.of(entry.getCompound(TAG_RESULT));
			if (remaining > 0 && total > 0 && isValidResult(result)) {
				state.charges.put(entry.getLong(TAG_POSITION), new Charge(result, remaining, total));
			}
		}

		return state;
	}

	@Nullable
	public Charge getCharge(BlockPos position) {
		return this.charges.get(position.asLong());
	}

	public void loadCharge(BlockPos position, ItemStack result, int total) {
		if (total <= 0 || !isValidResult(result)) {
			return;
		}

		this.charges.put(position.asLong(), new Charge(result, total, total));
		this.setDirty();
	}

	public int consume(BlockPos position, int requested) {
		if (requested <= 0) {
			return 0;
		}

		long key = position.asLong();
		Charge charge = this.charges.get(key);
		if (charge == null) {
			return 0;
		}

		int consumed = Math.min(requested, charge.remaining());
		int remaining = charge.remaining() - consumed;
		if (remaining == 0) {
			this.charges.remove(key);
		} else {
			this.charges.put(key, new Charge(charge.result(), remaining, charge.total()));
		}

		this.setDirty();
		return consumed;
	}

	public void remove(BlockPos position) {
		if (this.charges.remove(position.asLong()) != null) {
			this.setDirty();
		}
	}

	public void removeInvalidEntries(ServerLevel level) {
		boolean changed = false;
		Iterator<Map.Entry<Long, Charge>> iterator = this.charges.entrySet().iterator();
		while (iterator.hasNext()) {
			BlockPos position = BlockPos.of(iterator.next().getKey());
			if (level.hasChunkAt(position) && !level.getBlockState(position).is(Blocks.FLETCHING_TABLE)) {
				iterator.remove();
				changed = true;
			}
		}

		if (changed) {
			this.setDirty();
		}
	}

	@Override
	public CompoundTag save(CompoundTag root) {
		ListTag entries = new ListTag();
		for (Map.Entry<Long, Charge> mapEntry : this.charges.entrySet()) {
			Charge charge = mapEntry.getValue();
			CompoundTag entry = new CompoundTag();
			entry.putLong(TAG_POSITION, mapEntry.getKey());
			entry.putInt(TAG_REMAINING, charge.remaining());
			entry.putInt(TAG_TOTAL, charge.total());
			entry.put(TAG_RESULT, charge.result().save(new CompoundTag()));
			entries.add(entry);
		}

		root.put(TAG_CHARGES, entries);
		return root;
	}

	private static boolean isValidResult(ItemStack stack) {
		return stack.is(Items.TIPPED_ARROW) || stack.is(Items.SPECTRAL_ARROW);
	}

	public static final class Charge {
		private final ItemStack result;
		private final int remaining;
		private final int total;

		private Charge(ItemStack result, int remaining, int total) {
			this.result = result.copyWithCount(1);
			this.remaining = remaining;
			this.total = total;
		}

		public ItemStack result() {
			return this.result.copy();
		}

		public int remaining() {
			return this.remaining;
		}

		public int total() {
			return this.total;
		}
	}
}
