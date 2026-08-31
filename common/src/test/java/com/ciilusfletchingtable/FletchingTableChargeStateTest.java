package com.ciilusfletchingtable;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FletchingTableChargeStateTest {

    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void chargeSurvivesSerializationWithRemainingAmountAndPotionData() {
        var position = new BlockPos(12, 64, -7);
        var result = PotionContents.createItemStack(
                Items.TIPPED_ARROW,
                Potions.LONG_SWIFTNESS
        );
        FletchingRecipe.setDurationMultiplier(
                result,
                FletchingRecipe.FULL_DURATION_MULTIPLIER
        );

        var state = new FletchingTableChargeState();
        state.loadCharge(position, result, 16);
        assertEquals(5, state.consume(position, 5));

        var saved = state.save(new CompoundTag(), registries);
        var restored = FletchingTableChargeState.load(saved, registries);
        var charge = restored.getCharge(position);

        assertEquals(
                11,
                Objects.requireNonNull(charge).remaining()
        );
        assertEquals(
                16,
                charge.total()
        );
        assertEquals(
                Optional.of(Potions.LONG_SWIFTNESS),
                charge.result()
                        .getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                        .potion()
        );
        assertTrue(FletchingRecipe.hasFullDuration(charge.result()));
    }

    @Test
    void consumingTheLastChargeRemovesTheEntry() {
        var position = BlockPos.ZERO;
        var state = new FletchingTableChargeState();
        state.loadCharge(position, new ItemStack(Items.SPECTRAL_ARROW), 8);

        assertEquals(
                3,
                state.consume(position, 3)
        );
        assertEquals(
                5,
                Objects.requireNonNull(state.getCharge(position)).remaining()
        );
        assertEquals(
                5,
                state.consume(position, 20)
        );
        assertNull(state.getCharge(position));
    }

}
