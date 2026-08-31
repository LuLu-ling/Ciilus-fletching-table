package com.ciilusfletchingtable.fabric;

import com.ciilusfletchingtable.FletchingTableConfigData;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.autoconfig.AutoConfig;

public final class FletchingTableModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(
                FletchingTableConfigData.class,
                parent
        ).get();
    }

}
