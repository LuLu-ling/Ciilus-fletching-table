package com.ciilusfletchingtable;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;

public final class FletchingRecipe {
	/** Retained so arrows made by earlier releases keep their full-duration behavior. */
	public static final String FULL_DURATION_TAG = CiiluSFletchingTable.MOD_ID + ":full_duration";
	public static final String DURATION_MULTIPLIER_TAG = CiiluSFletchingTable.MOD_ID + ":duration_multiplier";
	public static final float FULL_DURATION_MULTIPLIER = 1.0F;
	public static final float VANILLA_LIKE_DURATION_MULTIPLIER = 0.5F;

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

		setDurationMultiplier(
			result.getOrCreateTag(),
			FletchingTableConfig.isVanillaLikeCrafting()
				? VANILLA_LIKE_DURATION_MULTIPLIER
				: FULL_DURATION_MULTIPLIER
		);
		return result;
	}

	public static boolean hasFullDuration(ItemStack stack) {
		return Float.compare(getDurationMultiplier(stack), FULL_DURATION_MULTIPLIER) == 0;
	}

	public static float getDurationMultiplier(ItemStack stack) {
		return getDurationMultiplier(stack.getTag());
	}

	public static float getDurationMultiplier(CompoundTag tag) {
		if (tag == null) {
			return 0.0F;
		}

		if (tag.contains(DURATION_MULTIPLIER_TAG, CompoundTag.TAG_ANY_NUMERIC)) {
			float multiplier = tag.getFloat(DURATION_MULTIPLIER_TAG);
			if (Float.isFinite(multiplier) && multiplier > 0.0F) {
				return Math.min(multiplier, FULL_DURATION_MULTIPLIER);
			}
		}

		return tag.getBoolean(FULL_DURATION_TAG) ? FULL_DURATION_MULTIPLIER : 0.0F;
	}

	public static void setDurationMultiplier(CompoundTag tag, float multiplier) {
		if (!Float.isFinite(multiplier) || multiplier <= 0.0F) {
			tag.remove(DURATION_MULTIPLIER_TAG);
			tag.remove(FULL_DURATION_TAG);
			return;
		}

		float normalized = Math.min(multiplier, FULL_DURATION_MULTIPLIER);
		tag.putFloat(DURATION_MULTIPLIER_TAG, normalized);
		if (Float.compare(normalized, FULL_DURATION_MULTIPLIER) == 0) {
			tag.putBoolean(FULL_DURATION_TAG, true);
		} else {
			tag.remove(FULL_DURATION_TAG);
		}
	}

	public static int scaleDuration(int duration, float multiplier) {
		if (duration <= 0 || multiplier <= 0.0F) {
			return duration;
		}

		return Math.max(1, (int) (duration * multiplier));
	}

	public static MobEffectInstance scaleEffectDuration(MobEffectInstance effect, float multiplier) {
		int duration = scaleDuration(effect.getDuration(), multiplier);
		if (duration == effect.getDuration()) {
			return effect;
		}

		return new MobEffectInstance(
			effect.getEffect(),
			duration,
			effect.getAmplifier(),
			effect.isAmbient(),
			effect.isVisible(),
			effect.showIcon()
		);
	}
}
