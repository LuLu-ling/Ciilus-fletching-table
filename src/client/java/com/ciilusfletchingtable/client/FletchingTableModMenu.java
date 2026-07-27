package com.ciilusfletchingtable.client;

import com.ciilusfletchingtable.FletchingTableConfigData;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.autoconfig.AutoConfig;

public final class FletchingTableModMenu implements ModMenuApi {
	/**
	 * Newer Cloth Config marks its generated screen for removal, but so is the registry needed to
	 * build one by hand, so there is nothing to move to yet. Revisit once it offers a successor;
	 * until then this is the supported way in.
	 */
	@SuppressWarnings("removal")
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> AutoConfig.getConfigScreen(FletchingTableConfigData.class, parent).get();
	}
}
