package com.flansmod.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.ContainerDriveableInventory;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.network.PacketDriveableGUI;

public class GuiDriveableInventory extends AbstractContainerScreen<ContainerDriveableInventory>
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/planeinventory.png");

	public ContainerDriveableInventory container;
	public Inventory inventory;
	public Level world;
	public int scroll;
	public int numItems;
	public int maxScroll;
	public EntityDriveable driveable;
	public int screen; //0 = Guns, 1 = Bombs, 2 = Cargo, 3 = Missiles
	
	public GuiDriveableInventory(ContainerDriveableInventory menu, Inventory inventoryplayer, Component title)
	{
		super(menu, inventoryplayer, title, 176, 180);
		container = menu;
		driveable = menu.getDriveable();
		inventory = inventoryplayer;
		world = menu.world;
		screen = menu.screen;
		maxScroll = menu.maxScroll;
		numItems = menu.numItems;
	}
	
	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY)
	{
		if(driveable == null)
			return;
		String title = " - Guns";
		if(screen == 1) title = " - " + driveable.getBombInventoryName();
		if(screen == 2) title = " - Cargo";
		if(screen == 3) title = " - " + driveable.getMissileInventoryName();
		extractor.text(font, driveable.getDriveableType().name + title, 6, 6, 0x404040);
		extractor.text(font, "Inventory", 8, (imageHeight - 96) + 2, 0x404040);

		if(screen == 0)
		{
			int slotsDone = 0;
			for(int i = 0; i < driveable.getDriveableType().seats.length; i++)
			{
				if(slotsDone >= 3 + scroll)
					continue;
				if(driveable.getDriveableType().seats[i].gunType != null)
				{
					if(slotsDone >= scroll)
					{
						extractor.text(font, driveable.getDriveableType().seats[i].gunName, 53, 29 + 19 * (slotsDone - scroll), 0x000000);
						drawStack(extractor, new ItemStack(driveable.getDriveableType().seats[i].gunType.getItem()), 10, 25 + 19 * (slotsDone - scroll));
					}
					slotsDone++;
				}
			}
			for(int i = 0; i < driveable.getDriveableType().pilotGuns.size(); i++)
			{
				if(slotsDone >= 3 + scroll)
					continue;
				if(driveable.getDriveableType().pilotGuns.get(i).type != null)
				{
					if(slotsDone >= scroll)
					{
						extractor.text(font, "Driver's gun " + (i + 1), 53, 29 + 19 * (slotsDone - scroll), 0x000000);
						drawStack(extractor, new ItemStack(driveable.getDriveableType().pilotGuns.get(i).type.getItem()), 10, 25 + 19 * (slotsDone - scroll));
					}
					slotsDone++;
				}
			}
		}
	}
	
	private void drawStack(GuiGraphicsExtractor extractor, ItemStack itemstack, int x, int y)
	{
		extractor.item(itemstack, x, y);
		extractor.itemDecorations(font, itemstack, x, y);
	}

	
	private static String getGunSlotName(int i)
	{
		switch(i)
		{
			case 0: return "Left Nose Gun";
			case 1: return "Right Nose Gun";
			case 2: return "Left Wing Gun";
			case 3: return "Right Wing Gun";
			case 4: return "Tail Gun";
			case 5: return "Left Bay Gun";
			case 6: return "Right Bay Gun";
			case 7: return "Dorsal Gun";
		}
		return "Not a Gun";
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		super.extractBackground(extractor, mouseX, mouseY, partialTick);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
		switch(screen)
		{
			case 0:
			{
				for(int n = 0; n < (numItems > 3 ? 3 : numItems); n++)
				{
					extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 9, topPos + 24 + 19 * n, 176, 0, 37, 18, 256, 256);
				}
				break;
			}
			case 1:
			case 2:
			case 3:
			{
				int m = ((numItems + 7) / 8);
				for(int row = 0; row < (m > 3 ? 3 : m); row++)
				{
					extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 9, topPos + 24 + 19 * row, 7, 97, 18 * ((row + scroll + 1) * 8 <= numItems ? 8 : numItems % 8), 18, 256, 256);
				}
				break;
			}
		}
		if(scroll == 0)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 161, topPos + 41, 176, 18, 10, 10, 256, 256);
		if(scroll == maxScroll)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + 161, topPos + 53, 176, 28, 10, 10, 256, 256);
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl)
	{
		super.mouseClicked(event, bl);
		int m = (int)event.x() - leftPos;
		int n = (int)event.y() - topPos;
		if(scroll > 0 && m > 161 && m < 171 && n > 41 && n < 51)
		{
			scroll--;
			container.updateScroll(scroll);
		}
		if(scroll < maxScroll && m > 161 && m < 171 && n > 53 && n < 63)
		{
			scroll++;
			container.updateScroll(scroll);
		}
		if(m > 161 && m < 171 && n > 5 && n < 15)
		{
			if(driveable instanceof EntityMecha)
				FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.MECHA));
			else
				FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.MENU));
		}
		return true;
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
