package com.ciilusfletchingtable.client;

import com.ciilusfletchingtable.CiiluSFletchingTable;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class CiiluSFletchingTableClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(CiiluSFletchingTable.FLETCHING_TABLE_MENU, FletchingTableScreen::new);
	}
}
