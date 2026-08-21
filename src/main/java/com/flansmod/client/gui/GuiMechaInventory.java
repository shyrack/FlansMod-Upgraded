package com.flansmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.mechas.ContainerMechaInventory;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.network.PacketDriveableGUI;

public class GuiMechaInventory extends AbstractContainerScreen<ContainerMechaInventory>
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/mechainventory.png");
	
	public ContainerMechaInventory container;
	public Inventory inventory;
	public Level world;
	public int scroll;
	public int numItems;
	public int maxScroll;
	public EntityMecha mecha;
	private int anim = 0;
	private long lastTime;
	
	public GuiMechaInventory(ContainerMechaInventory menu, Inventory inventoryplayer, Component title)
	{
		super(menu, inventoryplayer, title, 350, 180);
		container = menu;
		mecha = menu.getMecha();
		inventory = inventoryplayer;
		world = menu.world;
		maxScroll = menu.maxScroll;
		numItems = menu.numItems;
	}
	
	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY)
	{
		if(mecha == null)
			return;
		extractor.text(font, mecha.getMechaType().name, 9, 9, 0x404040);
		extractor.text(font, "Inventory", 181, (imageHeight - 96) + 2, 0x404040);
	}
	
	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		super.extractBackground(extractor, mouseX, mouseY, partialTick);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 512, 256);
		int numRows = ((numItems + 7) / 8);
		for(int row = 0; row < (numRows > 3 ? 3 : numRows); row++)
		{
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 185, topPos + 24 + 19 * row, 181, 97, 18 * ((row + scroll + 1) * 8 <= numItems ? 8 : numItems % 8), 18, 512, 256);
		}
		if(scroll == 0)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 336, topPos + 41, 350, 0, 10, 10, 512, 256);
		if(scroll == maxScroll)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 336, topPos + 53, 350, 10, 10, 10, 512, 256);
		
		long newTime = Minecraft.getInstance().level.getLevelData().getGameTime();
		if(newTime > lastTime)
		{
			lastTime = newTime;
			if(newTime % 5 == 0)
				anim++;
		}
		if(mecha == null)
			return;
		int fuelTankSize = mecha.getMechaType().fuelTankSize;
		float fuelInTank = mecha.driveableData.fuelInTank;
		if(fuelInTank < fuelTankSize / 8 && (anim % 4) > 1)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, width / 2 - 14, height / 2 - 59, 360, 0, 6, 6, 512, 256);
		if(fuelInTank > 0)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, width / 2 - 18, height / 2 + 45 - (int)((94 * fuelInTank) / fuelTankSize), 350, 20, 15, (int)((94 * fuelInTank) / fuelTankSize), 512, 256);
	}
	
	@Override
	public void init()
	{
		super.init();
		addRenderableWidget(Button.builder(Component.literal("Passenger Guns"), b ->
		{
			FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.GUNS));
		}).bounds(width / 2 - 166, height / 2 + 63, 93, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Repair"), b ->
		{
			Minecraft.getInstance().setScreen(new GuiDriveableRepair(inventory.player));
		}).bounds(width / 2 - 68, height / 2 + 63, 68, 20).build());
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl)
	{
		super.mouseClicked(event, bl);
		int m = (int)event.x() - leftPos;
		int n = (int)event.y() - topPos;
		if(scroll > 0 && m > 336 && m < 346 && n > 41 && n < 51)
		{
			scroll--;
			container.updateScroll(scroll);
		}
		if(scroll < maxScroll && m > 336 & m < 346 && n > 53 && n < 63)
		{
			scroll++;
			container.updateScroll(scroll);
		}
		return true;
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
