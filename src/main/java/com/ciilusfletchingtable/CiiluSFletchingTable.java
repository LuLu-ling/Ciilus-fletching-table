package com.ciilusfletchingtable;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiiluSFletchingTable implements ModInitializer {
	public static final String MOD_ID = "ciilus-fletching-table";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final MenuType<FletchingTableMenu> FLETCHING_TABLE_MENU = Registry.register(
		BuiltInRegistries.MENU,
		id("fletching_table"),
		new MenuType<>(FletchingTableMenu::new, FeatureFlags.VANILLA_SET)
	);

	@Override
	public void onInitialize() {
		FletchingTableConfig.load();

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (!level.getBlockState(hitResult.getBlockPos()).is(Blocks.FLETCHING_TABLE) || player.isSpectator()) {
				return InteractionResult.PASS;
			}

			if (!level.isClientSide) {
				ContainerLevelAccess access = ContainerLevelAccess.create(level, hitResult.getBlockPos());
				player.openMenu(new SimpleMenuProvider(
					(containerId, inventory, menuPlayer) -> new FletchingTableMenu(containerId, inventory, access),
					Component.translatable("container.ciilus-fletching-table.fletching_table")
				));
			}

			return InteractionResult.sidedSuccess(level.isClientSide);
		});

		PlayerBlockBreakEvents.AFTER.register((level, player, position, state, blockEntity) -> {
			if (state.is(Blocks.FLETCHING_TABLE) && level instanceof ServerLevel serverLevel) {
				FletchingTableChargeState.get(serverLevel).remove(position);
			}
		});

		ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (level.getGameTime() % 200 == 0) {
				FletchingTableChargeState state = FletchingTableChargeState.getIfPresent(level);
				if (state != null) {
					state.removeInvalidEntries(level);
				}
			}
		});

		LOGGER.info("Registered the fletching table arrow workshop");
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
