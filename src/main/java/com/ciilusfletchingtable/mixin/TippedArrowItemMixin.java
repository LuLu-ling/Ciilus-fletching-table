package com.ciilusfletchingtable.mixin;

import java.util.List;

import com.ciilusfletchingtable.FletchingRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TippedArrowItem.class)
public abstract class TippedArrowItemMixin {
	@Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
	private void ciilusFletchingTable$showFullDuration(
		ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag, CallbackInfo callbackInfo
	) {
		if (FletchingRecipe.hasFullDuration(stack)) {
			PotionUtils.addPotionTooltip(stack, tooltip, 1.0F);
			callbackInfo.cancel();
		}
	}
}
