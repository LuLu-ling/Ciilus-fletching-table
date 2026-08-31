package com.ciilusfletchingtable.client;

import com.ciilusfletchingtable.CiiluSFletchingTable;

import dev.architectury.registry.menu.MenuRegistry;

public final class CiiluSFletchingTableClient {

    private CiiluSFletchingTableClient() {
    }

    public static void init() {
        MenuRegistry.registerScreenFactory(
                CiiluSFletchingTable.FLETCHING_TABLE_MENU.get(),
                FletchingTableScreen::new
        );
    }

}
