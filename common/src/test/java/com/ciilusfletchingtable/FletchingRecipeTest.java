package com.ciilusfletchingtable;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FletchingRecipeTest {

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
    void glowstoneUsesRequestedOutputAmounts() {
        assertEquals(
                8,
                FletchingRecipe.getMaximumOutput(new ItemStack(Items.GLOWSTONE_DUST))
        );
        assertEquals(
                32,
                FletchingRecipe.getMaximumOutput(new ItemStack(Items.GLOWSTONE))
        );
        assertTrue(
                FletchingRecipe
                        .createOutput(new ItemStack(Items.GLOWSTONE_DUST))
                        .is(Items.SPECTRAL_ARROW)
        );
    }

    @Test
    void strongPotionUsesTheConfiguredBaseOutput() {
        var potion = PotionContents.createItemStack(
                Items.POTION,
                Potions.STRONG_SWIFTNESS
        );

        assertEquals(8, FletchingRecipe.getMaximumOutput(potion));
        var result = FletchingRecipe.createOutput(potion);
        assertTrue(result.is(Items.TIPPED_ARROW));
        assertEquals(
                Optional.of(Potions.STRONG_SWIFTNESS),
                result.getOrDefault(
                        DataComponents.POTION_CONTENTS,
                        PotionContents.EMPTY
                ).potion()
        );
        assertEffectsMatch(potion, result);
        assertTrue(FletchingRecipe.hasFullDuration(result));
    }

    @Test
    void configuredOutputIsSharedAndGlowstoneBlocksMultiplyByNine() {
        FletchingTableConfig.setBaseOutput(64);
        var potion = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);

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
        var potion = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.SWIFTNESS);

        assertEquals(8, FletchingRecipe.getMaximumOutput(potion));
    }

    @Test
    void longPotionKeepsItsDurationData() {
        var potion = PotionContents.createItemStack(
                Items.LINGERING_POTION,
                Potions.LONG_SWIFTNESS
        );
        var result = FletchingRecipe.createOutput(potion);

        assertEquals(
                Optional.of(Potions.LONG_SWIFTNESS),
                result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion()
        );
        assertEffectsMatch(potion, result);
        assertTrue(FletchingRecipe.hasFullDuration(result));
    }

    @Test
    void customPotionKeepsExactDurationAndAmplifier() {
        var potion = new ItemStack(Items.POTION);
        potion.set(
                DataComponents.POTION_CONTENTS,
                new PotionContents(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(new MobEffectInstance(MobEffects.POISON, 1234, 2))
                )
        );

        var result = FletchingRecipe.createOutput(potion);

        assertEffectsMatch(potion, result);
        assertTrue(FletchingRecipe.hasFullDuration(result));
    }

    @Test
    void vanillaLikeCraftingUsesHalfOfTheInputPotionDuration() {
        FletchingTableConfig.setVanillaLikeCrafting(true);
        var potion = new ItemStack(Items.POTION);
        potion.set(
                DataComponents.POTION_CONTENTS,
                new PotionContents(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(new MobEffectInstance(MobEffects.POISON, 1235, 2))
                )
        );

        var result = FletchingRecipe.createOutput(potion);

        assertEquals(
                FletchingRecipe.VANILLA_LIKE_DURATION_MULTIPLIER,
                FletchingRecipe.getDurationMultiplier(result)
        );
        var contents = result.getOrDefault(
                DataComponents.POTION_CONTENTS,
                PotionContents.EMPTY
        );
        var customEffects = contents.customEffects();
        var scaledEffect = FletchingRecipe.scaleEffectDuration(
                customEffects.getFirst(),
                FletchingRecipe.getDurationMultiplier(result)
        );
        assertEquals(617, scaledEffect.getDuration());
        assertEquals(2, scaledEffect.getAmplifier());
        assertEffectsMatch(potion, result);
    }

    private static void assertEffectsMatch(ItemStack expected, ItemStack actual) {
        List<MobEffectInstance> expectedEffects = new ArrayList<>();
        expected.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .getAllEffects()
                .forEach(expectedEffects::add);

        List<MobEffectInstance> actualEffects = new ArrayList<>();
        actual.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .getAllEffects()
                .forEach(actualEffects::add);

        assertEquals(expectedEffects.size(), actualEffects.size());
        IntStream.range(0, expectedEffects.size())
                .forEach(index -> {
                    var expectedEffect = expectedEffects.get(index);
                    var actualEffect = actualEffects.get(index);
                    assertEquals(expectedEffect.getEffect(), actualEffect.getEffect());
                    assertEquals(expectedEffect.getDuration(), actualEffect.getDuration());
                    assertEquals(expectedEffect.getAmplifier(), actualEffect.getAmplifier());
                });
    }

}
