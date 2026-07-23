package com.ciilusfletchingtable.client;

import com.ciilusfletchingtable.FletchingTableConfig;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class FletchingTableConfigScreen extends Screen {
	private final Screen parent;
	private BaseOutputSlider baseOutputSlider;
	private boolean closing;

	public FletchingTableConfigScreen(Screen parent) {
		super(Component.translatable("config.ciilus-fletching-table.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		this.baseOutputSlider = this.addRenderableWidget(new BaseOutputSlider(
			centerX - 100,
			this.height / 2 - 20,
			200,
			20,
			FletchingTableConfig.getBaseOutput()
		));
		this.addRenderableWidget(Button.builder(
			Component.translatable("gui.done"),
			button -> this.saveAndClose()
		).bounds(centerX - 100, this.height / 2 + 14, 200, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFFFFFFF);
		graphics.drawCenteredString(
			this.font,
			Component.translatable("config.ciilus-fletching-table.description"),
			this.width / 2,
			this.height / 2 - 48,
			0xFFA0A0A0
		);
	}

	@Override
	public void onClose() {
		this.saveAndClose();
	}

	private void saveAndClose() {
		if (this.closing) {
			return;
		}

		this.closing = true;
		if (this.baseOutputSlider != null) {
			FletchingTableConfig.setBaseOutput(this.baseOutputSlider.getOutput());
		}
		FletchingTableConfig.save();
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	private static final class BaseOutputSlider extends AbstractSliderButton {
		private BaseOutputSlider(int x, int y, int width, int height, int output) {
			super(
				x,
				y,
				width,
				height,
				Component.empty(),
				(output - FletchingTableConfig.MIN_BASE_OUTPUT)
					/ (double) (FletchingTableConfig.MAX_BASE_OUTPUT - FletchingTableConfig.MIN_BASE_OUTPUT)
			);
			this.updateMessage();
		}

		private int getOutput() {
			return Math.max(
				FletchingTableConfig.MIN_BASE_OUTPUT,
				Math.min(
					FletchingTableConfig.MAX_BASE_OUTPUT,
					FletchingTableConfig.MIN_BASE_OUTPUT + (int) Math.round(this.value *
						(FletchingTableConfig.MAX_BASE_OUTPUT - FletchingTableConfig.MIN_BASE_OUTPUT))
				)
			);
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.translatable("config.ciilus-fletching-table.base_output", this.getOutput()));
		}

		@Override
		protected void applyValue() {
			this.updateMessage();
		}
	}
}
