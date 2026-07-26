package com.ciilusfletchingtable.mixin;

import com.ciilusfletchingtable.FletchingRecipe;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
	private float ciilusFletchingTable$durationMultiplier;

	@Inject(method = "setEffectsFromItem", at = @At("TAIL"))
	private void ciilusFletchingTable$readDurationMarker(ItemStack stack, CallbackInfo callbackInfo) {
		this.ciilusFletchingTable$durationMultiplier = FletchingRecipe.getDurationMultiplier(stack);
	}

	@Redirect(
		method = "doPostHurtEffects",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/effect/MobEffectInstance;mapDuration(Lit/unimi/dsi/fastutil/ints/Int2IntFunction;)I"
		)
	)
	private int ciilusFletchingTable$applyPotionDuration(MobEffectInstance effect, Int2IntFunction mapper) {
		return this.ciilusFletchingTable$durationMultiplier > 0.0F
			? FletchingRecipe.scaleDuration(effect.getDuration(), this.ciilusFletchingTable$durationMultiplier)
			: effect.mapDuration(mapper);
	}

	@Redirect(
		method = "doPostHurtEffects",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			ordinal = 1
		)
	)
	private boolean ciilusFletchingTable$applyCustomPotionDuration(
		LivingEntity target, MobEffectInstance effect, Entity source
	) {
		MobEffectInstance appliedEffect = this.ciilusFletchingTable$durationMultiplier > 0.0F
			? FletchingRecipe.scaleEffectDuration(effect, this.ciilusFletchingTable$durationMultiplier)
			: effect;
		return target.addEffect(appliedEffect, source);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void ciilusFletchingTable$saveDurationMarker(CompoundTag tag, CallbackInfo callbackInfo) {
		if (this.ciilusFletchingTable$durationMultiplier > 0.0F) {
			FletchingRecipe.setDurationMultiplier(tag, this.ciilusFletchingTable$durationMultiplier);
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void ciilusFletchingTable$loadDurationMarker(CompoundTag tag, CallbackInfo callbackInfo) {
		this.ciilusFletchingTable$durationMultiplier = FletchingRecipe.getDurationMultiplier(tag);
	}

	@Inject(method = "getPickupItem", at = @At("RETURN"), cancellable = true)
	private void ciilusFletchingTable$markPickup(CallbackInfoReturnable<ItemStack> callbackInfo) {
		if (this.ciilusFletchingTable$durationMultiplier > 0.0F
			&& callbackInfo.getReturnValue().is(Items.TIPPED_ARROW)) {
			ItemStack pickup = callbackInfo.getReturnValue();
			FletchingRecipe.setDurationMultiplier(
				pickup.getOrCreateTag(),
				this.ciilusFletchingTable$durationMultiplier
			);
			callbackInfo.setReturnValue(pickup);
		}
	}
}
