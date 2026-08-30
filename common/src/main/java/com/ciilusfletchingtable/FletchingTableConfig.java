package com.ciilusfletchingtable;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import org.jspecify.annotations.Nullable;

/**
 * Reading and writing the file, and the screen that edits it, are Cloth Config's job. This is the
 * rest of the mod's way in, so nothing else has to know that.
 */
public final class FletchingTableConfig {

    public static final int DEFAULT_BASE_OUTPUT = 8;
    public static final int MIN_BASE_OUTPUT = 1;
    public static final int MAX_BASE_OUTPUT = 64;
    public static final boolean DEFAULT_VANILLA_LIKE_CRAFTING = false;

    private static @Nullable ConfigHolder<FletchingTableConfigData> holder;
    /** Stands in until {@link #load()} runs, so reads outside a running game still work. */
    private static FletchingTableConfigData data = new FletchingTableConfigData();

    private FletchingTableConfig() {
    }

    public static void load() {
        holder = AutoConfig.register(FletchingTableConfigData.class, GsonConfigSerializer::new);
        data = holder.getConfig();
    }

    public static void save() {
        if (holder != null) {
            holder.save();
        }
    }

    public static int getBaseOutput() {
        return clampBaseOutput(data.baseOutput);
    }

    public static void setBaseOutput(int value) {
        data.baseOutput = clampBaseOutput(value);
    }

    public static boolean isVanillaLikeCrafting() {
        return data.vanillaLikeCrafting;
    }

    public static void setVanillaLikeCrafting(boolean value) {
        data.vanillaLikeCrafting = value;
    }

    public static void reset() {
        if (holder == null) {
            data = new FletchingTableConfigData();
            return;
        }

        holder.resetToDefault();
        data = holder.getConfig();
    }

    public static int clampBaseOutput(int value) {
        return Math.clamp(
                value,
                MIN_BASE_OUTPUT,
                MAX_BASE_OUTPUT
        );
    }

}
