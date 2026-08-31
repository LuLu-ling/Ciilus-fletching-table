package com.ciilusfletchingtable;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CiiluSFletchingTable {

    public static final String MOD_ID = "ciilusfletchingtable";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(
            MOD_ID,
            Registries.MENU
    );

    public static final RegistrySupplier<MenuType<FletchingTableMenu>> FLETCHING_TABLE_MENU = MENUS.register(
            "fletching_table",
            () -> new MenuType<>(
                    FletchingTableMenu::new,
                    FeatureFlags.VANILLA_SET
            )
    );

    private CiiluSFletchingTable() {
    }

    public static void init() {
        MENUS.register();
        FletchingTableConfig.load();

        InteractionEvent.RIGHT_CLICK_BLOCK.register((
                player,
                hand,
                pos,
                face
        ) -> {
            if (!player.level().getBlockState(pos).is(Blocks.FLETCHING_TABLE)
                    || player.isSpectator()
            ) {
                return EventResult.pass();
            }

            if (player instanceof ServerPlayer serverPlayer) {
                var access = ContainerLevelAccess.create(
                        serverPlayer.level(),
                        pos
                );
                MenuRegistry.openMenu(
                        serverPlayer,
                        new SimpleMenuProvider(
                                (
                                        containerId,
                                        inventory,
                                        menuPlayer
                                ) -> new FletchingTableMenu(
                                        containerId,
                                        inventory,
                                        access
                                ),
                                Component.translatable(
                                        "container.ciilusfletchingtable.fletching_table")
                        )
                );
            }

            return EventResult.interruptTrue();
        });

        BlockEvent.BREAK.register((
                level,
                pos,
                state,
                player,
                xp
        ) -> {
            if (state.is(Blocks.FLETCHING_TABLE)
                    && level instanceof ServerLevel serverLevel
            ) {
                FletchingTableChargeState.get(serverLevel).remove(pos);
            }
            return EventResult.pass();
        });

        TickEvent.SERVER_LEVEL_POST.register(level -> {
            if (level.getGameTime() % 200 == 0) {
                var state = FletchingTableChargeState.getIfPresent(level);
                if (state != null) {
                    state.removeInvalidEntries(level);
                }
            }
        });

        LOGGER.info("Registered the fletching table arrow workshop");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

}
