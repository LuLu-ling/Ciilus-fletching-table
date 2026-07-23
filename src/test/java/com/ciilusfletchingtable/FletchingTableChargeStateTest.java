package com.ciilusfletchingtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FletchingTableChargeStateTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void chargeSurvivesSerializationWithRemainingAmountAndPotionData() {
		BlockPos position = new BlockPos(12, 64, -7);
		ItemStack result = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), Potions.LONG_SWIFTNESS);
		result.getOrCreateTag().putBoolean(FletchingRecipe.FULL_DURATION_TAG, true);

		FletchingTableChargeState state = new FletchingTableChargeState();
		state.loadCharge(position, result, 16);
		assertEquals(5, state.consume(position, 5));

		CompoundTag saved = state.save(new CompoundTag());
		FletchingTableChargeState restored = FletchingTableChargeState.load(saved);
		FletchingTableChargeState.Charge charge = restored.getCharge(position);

		assertEquals(11, charge.remaining());
		assertEquals(16, charge.total());
		assertEquals(Potions.LONG_SWIFTNESS, PotionUtils.getPotion(charge.result()));
		assertTrue(FletchingRecipe.hasFullDuration(charge.result()));
	}

	@Test
	void consumingTheLastChargeRemovesTheEntry() {
		BlockPos position = BlockPos.ZERO;
		FletchingTableChargeState state = new FletchingTableChargeState();
		state.loadCharge(position, new ItemStack(Items.SPECTRAL_ARROW), 8);

		assertEquals(3, state.consume(position, 3));
		assertEquals(5, state.getCharge(position).remaining());
		assertEquals(5, state.consume(position, 20));
		assertNull(state.getCharge(position));
	}
}
