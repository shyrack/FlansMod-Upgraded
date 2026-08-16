package com.flansmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import com.flansmod.common.driveables.ContainerDriveableMenu;
import com.flansmod.common.driveables.EntityDriveable;


public class GuiDriveableFuel extends AbstractContainerScreen<ContainerDriveableMenu>
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/planefuel.png");

	public Level world;
	public Inventory inventory;
	public EntityDriveable plane;
	private int anim = 0;
	private long lastTime;
	
	public GuiDriveableFuel(Inventory inventoryplayer, Level world1, EntityDriveable entPlane)
	{
		super(new ContainerDriveableMenu(inventoryplayer, world1, true, entPlane), inventoryplayer, Component.literal(""), 176, 161);
		plane = entPlane;
		world = world1;
		inventory = inventoryplayer;
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY)
	{
		extractor.text(font, plane.getDriveableType().name + " - Fuel", 6, 6, 0x404040);
		extractor.text(font, "Inventory", 8, (imageHeight - 96) + 2, 0x404040);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		super.extractBackground(extractor, mouseX, mouseY, partialTick);
		long newTime = Minecraft.getInstance().level.getLevelData().getGameTime();
		if(newTime > lastTime)
		{
			lastTime = newTime;
			if(newTime % 5 == 0)
				anim++;
		}

		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
		int fuelTankSize = plane.getDriveableType().fuelTankSize;
		float fuelInTank = plane.driveableData.fuelInTank;
		if(plane.fuelling)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 15, topPos + 44, 176 + 15 * (anim % 4), 0, 15, 16, 256, 256);
		if(fuelInTank < fuelTankSize / 8 && (anim % 4) > 1)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 16, topPos + 25, 176, 16, 6, 6, 256, 256);
		if(fuelInTank > 0)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 26, topPos + 21, 0, 161, (int)((129 * fuelInTank) / fuelTankSize), 15, 256, 256);
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl)
	{
		super.mouseClicked(event, bl);
		int m = (int)event.x() - leftPos;
		int n = (int)event.y() - topPos;
		if(m > 161 && m < 171 && n > 5 && n < 15)
		{
			Minecraft.getInstance().setScreen(new GuiDriveableMenu(inventory, world, plane));
		}
		return true;
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
