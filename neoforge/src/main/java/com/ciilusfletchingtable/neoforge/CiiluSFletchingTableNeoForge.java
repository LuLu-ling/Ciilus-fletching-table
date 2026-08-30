package com.ciilusfletchingtable.neoforge;

import com.ciilusfletchingtable.CiiluSFletchingTable;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(CiiluSFletchingTable.MOD_ID)
public final class CiiluSFletchingTableNeoForge {

    public CiiluSFletchingTableNeoForge(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {
        CiiluSFletchingTable.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            CiiluSFletchingTableNeoForgeClient.init(modEventBus, modContainer);
        }
    }

}
