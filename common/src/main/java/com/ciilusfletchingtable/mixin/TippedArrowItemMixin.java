package com.ciilusfletchingtable.mixin;

import com.ciilusfletchingtable.FletchingRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TippedArrowItem.class)
public abstract class TippedArrowItemMixin {

    @Inject(
            method = "appendHoverText",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ciilusFletchingTable$showFullDuration(
            ItemStack itemStack,
            Item.TooltipContext tooltipContext,
            List<Component> list,
            TooltipFlag tooltipFlag,
            CallbackInfo callbackInfo
    ) {
        var durationMultiplier = FletchingRecipe.getDurationMultiplier(itemStack);
        if (durationMultiplier > 0.0F) {
            var potionContents = itemStack.getOrDefault(
                    DataComponents.POTION_CONTENTS,
                    PotionContents.EMPTY
            );
            potionContents.addPotionTooltip(
                    list::add,
                    durationMultiplier,
                    tooltipContext.tickRate()
            );
            callbackInfo.cancel();
        }
    }

}
