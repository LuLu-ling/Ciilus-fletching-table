package com.ciilusfletchingtable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

public final class FletchingTableChargeState extends SavedData {

    private static final String DATA_NAME = CiiluSFletchingTable.MOD_ID + "_charges";
    private static final String TAG_CHARGES = "Charges";
    private static final String TAG_POSITION = "Position";
    private static final String TAG_REMAINING = "Remaining";
    private static final String TAG_TOTAL = "Total";
    private static final String TAG_RESULT = "Result";

    public static final SavedData.Factory<FletchingTableChargeState> FACTORY = new SavedData.Factory<>(
            FletchingTableChargeState::new,
            FletchingTableChargeState::load,
            null
    );

    private final Map<Long, Charge> charges = new HashMap<>();

    public static FletchingTableChargeState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    @Nullable
    public static FletchingTableChargeState getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(FACTORY, DATA_NAME);
    }

    public static FletchingTableChargeState load(
            CompoundTag root,
            HolderLookup.Provider registries
    ) {
        var state = new FletchingTableChargeState();
        var entries = root.getList(TAG_CHARGES, Tag.TAG_COMPOUND);

        IntStream.range(0, entries.size())
                .mapToObj(entries::getCompound)
                .forEachOrdered(entry -> {
                    var remaining = entry.getInt(TAG_REMAINING);
                    var total = Math.max(
                            remaining,
                            entry.getInt(TAG_TOTAL)
                    );
                    var result = ItemStack.parseOptional(
                            registries,
                            entry.getCompound(TAG_RESULT)
                    );
                    if (remaining > 0 && total > 0 && isValidResult(result)) {
                        state.charges.put(
                                entry.getLong(TAG_POSITION),
                                new Charge(result, remaining, total)
                        );
                    }
                });

        return state;
    }

    public @Nullable Charge getCharge(BlockPos position) {
        return this.charges.get(position.asLong());
    }

    public void loadCharge(
            BlockPos position,
            ItemStack result,
            int total
    ) {
        if (total <= 0 || !isValidResult(result)) {
            return;
        }

        this.charges.put(
                position.asLong(),
                new Charge(result, total, total)
        );
        this.setDirty();
    }

    public int consume(BlockPos position, int requested) {
        if (requested <= 0) {
            return 0;
        }

        var key = position.asLong();
        var charge = this.charges.get(key);
        if (charge == null) {
            return 0;
        }

        var consumed = Math.min(requested, charge.remaining());
        var remaining = charge.remaining() - consumed;
        if (remaining == 0) {
            this.charges.remove(key);
        } else {
            this.charges.put(
                    key,
                    new Charge(
                            charge.result(),
                            remaining,
                            charge.total()
                    )
            );
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
        var changed = false;
        var iterator = this.charges.entrySet().iterator();
        while (iterator.hasNext()) {
            var position = BlockPos.of(iterator.next().getKey());
            if (
                    level.hasChunk(
                            SectionPos.blockToSectionCoord(position.getX()),
                            SectionPos.blockToSectionCoord(position.getZ())
                    ) && !level.getBlockState(position).is(Blocks.FLETCHING_TABLE)
            ) {
                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            this.setDirty();
        }
    }

    @Override
    public CompoundTag save(
            CompoundTag root,
            HolderLookup.Provider registries
    ) {
        var entries = new ListTag();
        this.charges.forEach((key, charge) -> {
            var entry = new CompoundTag();
            entry.putLong(TAG_POSITION, key);
            entry.putInt(TAG_REMAINING, charge.remaining());
            entry.putInt(TAG_TOTAL, charge.total());
            entry.put(TAG_RESULT, charge.result().save(registries));
            entries.add(entry);
        });

        root.put(TAG_CHARGES, entries);
        return root;
    }

    private static boolean isValidResult(ItemStack stack) {
        return stack.is(Items.TIPPED_ARROW)
                || stack.is(Items.SPECTRAL_ARROW);
    }

    public record Charge(
            ItemStack result,
            int remaining,
            int total
    ) {

        public Charge(
                ItemStack result,
                int remaining,
                int total
        ) {
            this.result = result.copyWithCount(1);
            this.remaining = remaining;
            this.total = total;
        }

        @Override
        public ItemStack result() {
            return this.result.copy();
        }

    }

}
