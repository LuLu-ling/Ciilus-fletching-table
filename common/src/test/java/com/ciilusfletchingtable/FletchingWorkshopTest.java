package com.ciilusfletchingtable;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FletchingWorkshopTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void resetConfig() {
        FletchingTableConfig.reset();
    }

    @Test
    void previewIsCalculatedCorrectlyFromIngredientAndArrows() {
        var input = new SimpleContainer(2);
        var result = new SimpleContainer(1);
        var preview = new SimpleContainer(1);
        var data = new SimpleContainerData(FletchingWorkshop.DATA_COUNT);

        var workshop = new FletchingWorkshop(input, result, preview, data);

        // No inputs
        workshop.updateResultPreview(null);
        assertTrue(workshop.getResult().isEmpty());

        // Place potion but no arrows
        var potion = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
        input.setItem(FletchingWorkshop.INGREDIENT_SLOT, potion);
        workshop.updateResultPreview(null);
        assertTrue(workshop.getResult().isEmpty());

        // Place 4 arrows
        input.setItem(FletchingWorkshop.ARROW_SLOT, new ItemStack(Items.ARROW, 4));
        workshop.updateResultPreview(null);
        assertEquals(4, workshop.getResult().getCount());
        assertTrue(workshop.getResult().is(Items.TIPPED_ARROW));

        // Place 16 arrows (base output is 8)
        input.setItem(FletchingWorkshop.ARROW_SLOT, new ItemStack(Items.ARROW, 16));
        workshop.updateResultPreview(null);
        assertEquals(8, workshop.getResult().getCount());
    }

    @Test
    void activeChargeSynchronizesDataSlotsAndPreviewContainer() {
        var input = new SimpleContainer(2);
        var result = new SimpleContainer(1);
        var preview = new SimpleContainer(1);
        var data = new SimpleContainerData(FletchingWorkshop.DATA_COUNT);

        var workshop = new FletchingWorkshop(input, result, preview, data);

        var arrowResult = PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.HEALING);
        var charge = new FletchingTableChargeState.Charge(arrowResult, 5, 8);

        workshop.syncChargeState(charge);

        assertTrue(workshop.hasActiveCharge());
        assertEquals(5, workshop.getRemainingOutput());
        assertEquals(8, workshop.getTotalOutput());
        assertEquals(5, workshop.getDisplayedRemainingOutput());
        assertEquals(8, workshop.getDisplayedTotalOutput());
        assertEquals(
                Optional.of(Potions.HEALING),
                workshop.getActiveResult()
                        .getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                        .potion()
        );
    }

    @Test
    void spectralArrowPreviewFromGlowstone() {
        var input = new SimpleContainer(2);
        var result = new SimpleContainer(1);
        var preview = new SimpleContainer(1);
        var data = new SimpleContainerData(FletchingWorkshop.DATA_COUNT);

        var workshop = new FletchingWorkshop(input, result, preview, data);

        input.setItem(
                FletchingWorkshop.INGREDIENT_SLOT,
                new ItemStack(Items.GLOWSTONE, 1)
        );
        input.setItem(
                FletchingWorkshop.ARROW_SLOT,
                new ItemStack(Items.ARROW, 64)
        );

        workshop.updateResultPreview(null);
        assertEquals(32, workshop.getResult().getCount());
        assertTrue(workshop.getResult().is(Items.SPECTRAL_ARROW));
        assertFalse(workshop.hasActiveCharge());
        assertEquals(32, workshop.getDisplayedRemainingOutput());
        assertEquals(32, workshop.getDisplayedTotalOutput());
    }

}
