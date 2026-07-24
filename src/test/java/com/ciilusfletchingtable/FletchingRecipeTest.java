package com.ciilusfletchingtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FletchingRecipeTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@AfterEach
	void resetConfig() {
		FletchingTableConfig.setBaseOutput(FletchingTableConfig.DEFAULT_BASE_OUTPUT);
	}

	@Test
	void glowstoneUsesRequestedOutputAmounts() {
		assertEquals(8, FletchingRecipe.getMaximumOutput(new ItemStack(Items.GLOWSTONE_DUST)));
		assertEquals(32, FletchingRecipe.getMaximumOutput(new ItemStack(Items.GLOWSTONE)));
		assertTrue(FletchingRecipe.createOutput(new ItemStack(Items.GLOWSTONE_DUST)).is(Items.SPECTRAL_ARROW));
	}

	@Test
	void strongPotionUsesTheConfiguredBaseOutput() {
		ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_SWIFTNESS);

		assertEquals(8, FletchingRecipe.getMaximumOutput(potion));
		ItemStack result = FletchingRecipe.createOutput(potion);
		assertTrue(result.is(Items.TIPPED_ARROW));
		assertEquals(Potions.STRONG_SWIFTNESS, PotionUtils.getPotion(result));
		assertEffectsMatch(potion, result);
		assertTrue(FletchingRecipe.hasFullDuration(result));
	}

	@Test
	void configuredOutputIsSharedAndGlowstoneBlocksMultiplyByNine() {
		FletchingTableConfig.setBaseOutput(64);
		ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS);

		assertEquals(64, FletchingRecipe.getMaximumOutput(potion));
		assertEquals(64, FletchingRecipe.getMaximumOutput(new ItemStack(Items.GLOWSTONE_DUST)));
		assertEquals(256, FletchingRecipe.getMaximumOutput(new ItemStack(Items.GLOWSTONE)));
	}

	@Test
	void configuredOutputIsClampedToOneThroughSixtyFour() {
		FletchingTableConfig.setBaseOutput(0);
		assertEquals(1, FletchingTableConfig.getBaseOutput());

		FletchingTableConfig.setBaseOutput(65);
		assertEquals(64, FletchingTableConfig.getBaseOutput());
	}

	@Test
	void normalPotionProducesEightArrows() {
		ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.SWIFTNESS);

		assertEquals(8, FletchingRecipe.getMaximumOutput(potion));
	}

	@Test
	void longPotionKeepsItsDurationData() {
		ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), Potions.LONG_SWIFTNESS);
		ItemStack result = FletchingRecipe.createOutput(potion);

		assertEquals(Potions.LONG_SWIFTNESS, PotionUtils.getPotion(result));
		assertEffectsMatch(potion, result);
		assertTrue(FletchingRecipe.hasFullDuration(result));
	}

	@Test
	void customPotionKeepsExactDurationAndAmplifier() {
		ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.EMPTY);
		PotionUtils.setCustomEffects(potion, List.of(new MobEffectInstance(MobEffects.POISON, 1234, 2)));

		ItemStack result = FletchingRecipe.createOutput(potion);

		assertEffectsMatch(potion, result);
		assertTrue(FletchingRecipe.hasFullDuration(result));
	}

	private static void assertEffectsMatch(ItemStack expected, ItemStack actual) {
		List<MobEffectInstance> expectedEffects = PotionUtils.getMobEffects(expected);
		List<MobEffectInstance> actualEffects = PotionUtils.getMobEffects(actual);
		assertEquals(expectedEffects.size(), actualEffects.size());
		for (int index = 0; index < expectedEffects.size(); index++) {
			MobEffectInstance expectedEffect = expectedEffects.get(index);
			MobEffectInstance actualEffect = actualEffects.get(index);
			assertEquals(expectedEffect.getEffect(), actualEffect.getEffect());
			assertEquals(expectedEffect.getDuration(), actualEffect.getDuration());
			assertEquals(expectedEffect.getAmplifier(), actualEffect.getAmplifier());
		}
	}
}
