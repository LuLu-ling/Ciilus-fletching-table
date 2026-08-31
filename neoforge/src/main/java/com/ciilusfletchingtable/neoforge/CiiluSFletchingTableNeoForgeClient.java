package com.ciilusfletchingtable.neoforge;

import com.ciilusfletchingtable.CiiluSFletchingTable;
import com.ciilusfletchingtable.FletchingTableConfigData;
import com.ciilusfletchingtable.client.FletchingTableScreen;

import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class CiiluSFletchingTableNeoForgeClient {

    private CiiluSFletchingTableNeoForgeClient() {
    }

    public static void init(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {
        modEventBus.addListener(
                RegisterMenuScreensEvent.class,
                event -> event.register(
                        CiiluSFletchingTable.FLETCHING_TABLE_MENU.get(),
                        FletchingTableScreen::new
                )
        );
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, screen) -> AutoConfig.getConfigScreen(
                        FletchingTableConfigData.class,
                        screen
                ).get()
        );
    }

}
