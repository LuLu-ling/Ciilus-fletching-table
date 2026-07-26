package com.ciilusfletchingtable.client;

import java.util.List;

import com.ciilusfletchingtable.FletchingTableMenu;
import com.ciilusfletchingtable.FletchingRecipe;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;

@Environment(EnvType.CLIENT)
public class FletchingTableScreen extends AbstractContainerScreen<FletchingTableMenu> {
	private static final int PANEL_COLOR = 0xFFC6C6C6;
	private static final int PANEL_LIGHT = 0xFFFFFFFF;
	private static final int PANEL_DARK = 0xFF555555;
	private static final int SLOT_COLOR = 0xFF8B8B8B;
	private static final int TEXT_COLOR = 0xFF3F3F3F;
	private static final ResourceLocation ANVIL_TEXTURE = new ResourceLocation("textures/gui/container/anvil.png");
	private static final int ICON_CHANGE_TICK_RATE = 30;
	private static final int ICON_TRANSITION_TICK_DURATION = 4;
	private static final String[] POTION_PLACEHOLDER = {
		"               ",
		"               ",
		"      ####      ",
		"     ######     ",
		"     ######     ",
		"      # .#      ",
		"      # .#      ",
		"     #. ..#     ",
		"    #.#. ..#    ",
		"   #.#. ....#   ",
		"   #.#. ....#   ",
		"   #... ..#.#   ",
		"   #... ..#.#   ",
		"    #.. .#.#    ",
		"     ######     ",
		"                "
	};
	private static final String[] GLOWSTONE_DUST_PLACEHOLDER = {
		"                ",
		"                ",
		"                ",
		"       ##       ",
		"      #..#      ",
		"     #....#     ",
		"    #......#    ",
		"   #........#   ",
		"  #..........#  ",
		"  #..........#  ",
		"  #..........#  ",
		"   #........#   ",
		"    ##....##    ",
		"      ####      ",
		"                ",
		"                "
	};

	private int ingredientIconTick;
	private int ingredientIconIndex;

	public FletchingTableScreen(FletchingTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 170;
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = 77;
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		this.ingredientIconTick++;
		if (this.ingredientIconTick % ICON_CHANGE_TICK_RATE == 0) {
			this.ingredientIconIndex = (this.ingredientIconIndex + 1) % 2;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;
		drawBeveledPanel(graphics, x, y, this.imageWidth, this.imageHeight);
		drawInsetPanel(graphics, x + 7, y + 40, 162, 33);

		drawSlot(graphics, x + 27, y + 20);
		drawSlot(graphics, x + 76, y + 20);
		drawSlot(graphics, x + 134, y + 20);
		drawAnvilSymbols(graphics, x, y);
		this.drawIngredientPlaceholder(graphics, partialTick, x + 27, y + 20);

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, x + 7 + column * 18, y + 88 + row * 18);
			}
		}

		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, x + 7 + column * 18, y + 146);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);
		this.renderStatus(graphics);
	}

	@Override
	protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderTooltip(graphics, mouseX, mouseY);
		if (this.hoveredSlot == null || this.hoveredSlot.hasItem()) {
			return;
		}

		if (this.hoveredSlot == this.menu.getSlot(FletchingTableMenu.INGREDIENT_SLOT)) {
			graphics.renderTooltip(
				this.font,
				Component.translatable("screen.ciilus-fletching-table.ingredient_tooltip"),
				mouseX,
				mouseY
			);
		} else if (this.hoveredSlot == this.menu.getSlot(FletchingTableMenu.ARROW_SLOT)) {
			graphics.renderTooltip(
				this.font,
				Component.translatable("screen.ciilus-fletching-table.arrow_tooltip"),
				mouseX,
				mouseY
			);
		}
	}

	private void renderStatus(GuiGraphics graphics) {
		ItemStack activeResult = this.menu.getDisplayedResult();
		Component effect = effectSummary(activeResult);
		Component effectLine = Component.translatable("screen.ciilus-fletching-table.effect_line", effect);
		drawFitted(graphics, effectLine, 12, 45, 154, TEXT_COLOR);

		Component count = Component.translatable(
			"screen.ciilus-fletching-table.craft_count",
			this.menu.getDisplayedRemainingOutput(),
			this.menu.getDisplayedTotalOutput()
		);
		drawFitted(graphics, count, 12, 59, 154, TEXT_COLOR);
	}

	private static Component effectSummary(ItemStack result) {
		if (result.isEmpty()) {
			return Component.translatable("screen.ciilus-fletching-table.no_ingredient");
		}

		if (result.is(Items.SPECTRAL_ARROW)) {
			return Component.translatable("screen.ciilus-fletching-table.spectral");
		}

		List<MobEffectInstance> effects = PotionUtils.getMobEffects(result);
		if (effects.isEmpty()) {
			return Component.translatable("effect.none");
		}

		MutableComponent summary = Component.empty();
		float durationMultiplier = FletchingRecipe.getDurationMultiplier(result);
		if (durationMultiplier <= 0.0F) {
			durationMultiplier = FletchingRecipe.FULL_DURATION_MULTIPLIER;
		}
		int displayed = Math.min(2, effects.size());
		for (int index = 0; index < displayed; index++) {
			if (index > 0) {
				summary.append(Component.literal(", "));
			}
			summary.append(effectName(effects.get(index), durationMultiplier));
		}
		if (effects.size() > displayed) {
			summary.append(Component.literal(" +" + (effects.size() - displayed)));
		}
		return summary;
	}

	private static Component effectName(MobEffectInstance effect, float durationMultiplier) {
		MutableComponent name = Component.translatable(effect.getDescriptionId());
		if (effect.getAmplifier() > 0) {
			name = Component.translatable(
				"potion.withAmplifier",
				name,
				Component.translatable("potion.potency." + effect.getAmplifier())
			);
		}

		if (FletchingRecipe.scaleDuration(effect.getDuration(), durationMultiplier) > 20) {
			name = Component.translatable(
				"potion.withDuration",
				name,
				MobEffectUtil.formatDuration(effect, durationMultiplier)
			);
		}

		return name;
	}

	private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
		int textWidth = this.font.width(text);
		if (textWidth <= width) {
			graphics.drawString(this.font, text, x, y, color, false);
			return;
		}

		float scale = width / (float) textWidth;
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0.0D);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.drawString(this.font, text, 0, 0, color, false);
		graphics.pose().popPose();
	}

	private static void drawAnvilSymbols(GuiGraphics graphics, int x, int y) {
		graphics.blit(ANVIL_TEXTURE, x + 52, y + 21, 52, 48, 15, 15);
		graphics.blit(ANVIL_TEXTURE, x + 101, y + 20, 101, 47, 24, 17);
	}

	private void drawIngredientPlaceholder(GuiGraphics graphics, float partialTick, int x, int y) {
		if (this.menu.getSlot(FletchingTableMenu.INGREDIENT_SLOT).hasItem()) {
			return;
		}

		float transition = Math.min(
			ICON_TRANSITION_TICK_DURATION,
			(this.ingredientIconTick % ICON_CHANGE_TICK_RATE) + partialTick
		) / (float) ICON_TRANSITION_TICK_DURATION;
		String[] previousIcon = this.ingredientIconIndex == 0 ? GLOWSTONE_DUST_PLACEHOLDER : POTION_PLACEHOLDER;
		String[] currentIcon = this.ingredientIconIndex == 0 ? POTION_PLACEHOLDER : GLOWSTONE_DUST_PLACEHOLDER;
		drawPixelIcon(graphics, previousIcon, x, y, 1.0F - transition);
		drawPixelIcon(graphics, currentIcon, x, y, transition);
	}

	private static void drawPixelIcon(GuiGraphics graphics, String[] icon, int x, int y, float alpha) {
		if (alpha <= 0.0F) {
			return;
		}

		int color = ((int) (alpha * 255.0F) << 24) | 0x555555;
		for (int row = 0; row < icon.length; row++) {
			String pixels = icon[row];
			for (int column = 0; column < pixels.length(); column++) {
				if (pixels.charAt(column) == '#') {
					graphics.fill(x + column, y + row, x + column + 1, y + row + 1, color);
				}
			}
		}
	}

	private static void drawBeveledPanel(GuiGraphics graphics, int x, int y, int width, int height) {
		graphics.fill(x, y, x + width, y + height, PANEL_DARK);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_LIGHT);
		graphics.fill(x + 2, y + 2, x + width - 1, y + height - 1, PANEL_COLOR);
	}

	private static void drawInsetPanel(GuiGraphics graphics, int x, int y, int width, int height) {
		graphics.fill(x, y, x + width, y + height, PANEL_DARK);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFE3E3E3);
		graphics.fill(x + 2, y + 2, x + width - 1, y + height - 1, 0xFFB5B5B5);
	}

	private static void drawSlot(GuiGraphics graphics, int x, int y) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, PANEL_DARK);
		graphics.fill(x, y, x + 17, y + 17, PANEL_LIGHT);
		graphics.fill(x, y, x + 16, y + 16, SLOT_COLOR);
	}
}
