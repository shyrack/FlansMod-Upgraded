package com.flansmod.client.gui.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Minimal replacement for the Forge config GUI. Flan's Mod currently has no
 * editable options, so this screen just shows a note and a close button.
 */
public class ModGuiConfig extends Screen
{
	private final Screen parent;

	public ModGuiConfig(Screen parent)
	{
		super(Component.literal("Flan's Mod Config"));
		this.parent = parent;
	}

	@Override
	protected void init()
	{
		addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose()).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
		extractor.text(this.font, "Flan's Mod has no editable config options.", this.width / 2 - 120, 40, 0xFFFFFF);
	}

	@Override
	public void onClose()
	{
		Minecraft.getInstance().setScreen(parent);
	}
}
