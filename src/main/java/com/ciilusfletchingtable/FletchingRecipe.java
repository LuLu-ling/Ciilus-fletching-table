package com.ciilusfletchingtable;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;

public final class FletchingRecipe {
	public static final String FULL_DURATION_TAG = CiiluSFletchingTable.MOD_ID + ":full_duration";

	private static final int GLOWSTONE_BLOCK_MULTIPLIER = 4;

	private FletchingRecipe() {
	}

	public static boolean isIngredient(ItemStack stack) {
		return stack.getItem() instanceof PotionItem || stack.is(Items.GLOWSTONE_DUST) || stack.is(Items.GLOWSTONE);
	}

	public static boolean isPotion(ItemStack stack) {
		return stack.getItem() instanceof PotionItem;
	}

	public static boolean isArrow(ItemStack stack) {
		return stack.is(Items.ARROW);
	}

	public static int getMaximumOutput(ItemStack ingredient) {
		if (ingredient.is(Items.GLOWSTONE)) {
			return FletchingTableConfig.getBaseOutput() * GLOWSTONE_BLOCK_MULTIPLIER;
		}

		if (ingredient.is(Items.GLOWSTONE_DUST) || isPotion(ingredient)) {
			return FletchingTableConfig.getBaseOutput();
		}

		return 0;
	}

	public static ItemStack createOutput(ItemStack ingredient) {
		if (ingredient.is(Items.GLOWSTONE_DUST) || ingredient.is(Items.GLOWSTONE)) {
			return new ItemStack(Items.SPECTRAL_ARROW);
		}

		if (!isPotion(ingredient)) {
			return ItemStack.EMPTY;
		}

		ItemStack result = new ItemStack(Items.TIPPED_ARROW);
		PotionUtils.setPotion(result, PotionUtils.getPotion(ingredient));

		List<MobEffectInstance> customEffects = PotionUtils.getCustomEffects(ingredient);
		PotionUtils.setCustomEffects(result, customEffects);

		CompoundTag ingredientTag = ingredient.getTag();
		if (ingredientTag != null && ingredientTag.contains(PotionUtils.TAG_CUSTOM_POTION_COLOR, CompoundTag.TAG_ANY_NUMERIC)) {
			result.getOrCreateTag().putInt(PotionUtils.TAG_CUSTOM_POTION_COLOR, ingredientTag.getInt(PotionUtils.TAG_CUSTOM_POTION_COLOR));
		}

		result.getOrCreateTag().putBoolean(FULL_DURATION_TAG, true);
		return result;
	}

	public static boolean hasFullDuration(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean(FULL_DURATION_TAG);
	}
}
