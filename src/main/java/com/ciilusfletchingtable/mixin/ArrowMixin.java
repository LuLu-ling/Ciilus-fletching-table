package com.ciilusfletchingtable.mixin;

import com.ciilusfletchingtable.FletchingRecipe;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Arrow.class)
public abstract class ArrowMixin {
	@Unique
	private boolean ciilusFletchingTable$fullDuration;

	@Inject(method = "setEffectsFromItem", at = @At("TAIL"))
	private void ciilusFletchingTable$readDurationMarker(ItemStack stack, CallbackInfo callbackInfo) {
		this.ciilusFletchingTable$fullDuration = FletchingRecipe.hasFullDuration(stack);
	}

	@Redirect(
		method = "doPostHurtEffects",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/effect/MobEffectInstance;mapDuration(Lit/unimi/dsi/fastutil/ints/Int2IntFunction;)I"
		)
	)
	private int ciilusFletchingTable$keepPotionDuration(MobEffectInstance effect, Int2IntFunction mapper) {
		return this.ciilusFletchingTable$fullDuration ? effect.getDuration() : effect.mapDuration(mapper);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void ciilusFletchingTable$saveDurationMarker(CompoundTag tag, CallbackInfo callbackInfo) {
		if (this.ciilusFletchingTable$fullDuration) {
			tag.putBoolean(FletchingRecipe.FULL_DURATION_TAG, true);
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void ciilusFletchingTable$loadDurationMarker(CompoundTag tag, CallbackInfo callbackInfo) {
		this.ciilusFletchingTable$fullDuration = tag.getBoolean(FletchingRecipe.FULL_DURATION_TAG);
	}

	@Inject(method = "getPickupItem", at = @At("RETURN"), cancellable = true)
	private void ciilusFletchingTable$markPickup(CallbackInfoReturnable<ItemStack> callbackInfo) {
		if (this.ciilusFletchingTable$fullDuration && callbackInfo.getReturnValue().is(Items.TIPPED_ARROW)) {
			ItemStack pickup = callbackInfo.getReturnValue();
			pickup.getOrCreateTag().putBoolean(FletchingRecipe.FULL_DURATION_TAG, true);
			callbackInfo.setReturnValue(pickup);
		}
	}
}
