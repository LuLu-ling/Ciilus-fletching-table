package com.ciilusfletchingtable.fabric;

import com.ciilusfletchingtable.client.CiiluSFletchingTableClient;

import net.fabricmc.api.ClientModInitializer;

public final class CiiluSFletchingTableFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CiiluSFletchingTableClient.init();
    }

}
