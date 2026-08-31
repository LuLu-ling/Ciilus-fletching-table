package com.ciilusfletchingtable.mixin;

import com.ciilusfletchingtable.FletchingRecipe;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Arrow.class)
public abstract class ArrowMixin {

    @Unique
    private float ciilusFletchingTable$durationMultiplier;

    @Inject(
            method = "setPickupItemStack",
            at = @At("TAIL")
    )
    private void ciilusFletchingTable$readDurationMarker(
            ItemStack itemStack,
            CallbackInfo callbackInfo
    ) {
        this.ciilusFletchingTable$durationMultiplier = FletchingRecipe.getDurationMultiplier(
                itemStack
        );
    }

    @Redirect(
            method = "doPostHurtEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffectInstance;mapDuration(Lit/unimi/dsi/fastutil/ints/Int2IntFunction;)I"
            )
    )
    private int ciilusFletchingTable$applyPotionDuration(
            MobEffectInstance mobEffectInstance,
            Int2IntFunction int2IntFunction
    ) {
        return this.ciilusFletchingTable$durationMultiplier > 0.0F
                ?
                FletchingRecipe.scaleDuration(
                        mobEffectInstance.getDuration(),
                        this.ciilusFletchingTable$durationMultiplier
                )
                : mobEffectInstance.mapDuration(int2IntFunction);
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
            LivingEntity target,
            MobEffectInstance mobEffectInstance,
            Entity entity
    ) {
        var appliedEffect = this.ciilusFletchingTable$durationMultiplier > 0.0F
                ?
                FletchingRecipe.scaleEffectDuration(
                        mobEffectInstance,
                        this.ciilusFletchingTable$durationMultiplier
                )
                : mobEffectInstance;
        return target.addEffect(appliedEffect, entity);
    }

}
