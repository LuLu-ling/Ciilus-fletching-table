package com.ciilusfletchingtable;

import com.google.gson.annotations.SerializedName;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * The shape Cloth Config reads, writes and builds the settings screen from. The serialized names
 * are spelled out so the file keeps the keys it had before this was handed over to Cloth Config.
 */
@Config(name = CiiluSFletchingTable.MOD_ID)
public final class FletchingTableConfigData implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(
            min = FletchingTableConfig.MIN_BASE_OUTPUT,
            max = FletchingTableConfig.MAX_BASE_OUTPUT
    )
    @SerializedName("base_output")
    public int baseOutput = FletchingTableConfig.DEFAULT_BASE_OUTPUT;

    @ConfigEntry.Gui.Tooltip
    @SerializedName("vanilla_like_crafting")
    public boolean vanillaLikeCrafting = FletchingTableConfig.DEFAULT_VANILLA_LIKE_CRAFTING;

    @Override
    public void validatePostLoad() {
        this.baseOutput = FletchingTableConfig.clampBaseOutput(this.baseOutput);
    }

}
