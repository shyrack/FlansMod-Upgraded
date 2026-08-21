package com.flansmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.ContainerDriveableMenu;
import com.flansmod.common.driveables.DriveableType;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.network.PacketDriveableGUI;

public class GuiDriveableMenu extends AbstractContainerScreen<ContainerDriveableMenu>
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/planemenu.png");
	
	public Level world;
	public Inventory inventory;
	public EntityDriveable entity;
	
	public GuiDriveableMenu(ContainerDriveableMenu menu, Inventory inventoryplayer, Component title)
	{
		super(menu, inventoryplayer, title, 176, 180);
		entity = menu.getDriveable();
		world = menu.world;
		inventory = inventoryplayer;
	}
	
	@Override
	public void init()
	{
		super.init();
		if(entity == null)
			return;
		DriveableType type = entity.getDriveableType();
		//Cargo button
		Button cargoButton = Button.builder(Component.literal("Cargo"), b -> actionPerformed(0)).bounds(width / 2 - 60, height / 2 - 71, 58, 20).build();
		cargoButton.active = type.numCargoSlots > 0;
		addRenderableWidget(cargoButton);
		
		//Gun button
		Button gunsButton = Button.builder(Component.literal("Guns"), b -> actionPerformed(1)).bounds(width / 2 + 2, height / 2 - 71, 58, 20).build();
		gunsButton.active = type.ammoSlots() > 0;
		addRenderableWidget(gunsButton);
		
		//Fuel button
		Button fuelButton = Button.builder(Component.literal("Fuel"), b -> actionPerformed(2)).bounds(width / 2 - 60, height / 2 - 49, 58, 20).build();
		fuelButton.active = type.fuelTankSize > 0;
		addRenderableWidget(fuelButton);
		
		//Missile / Shell Button
		Button missileButton = Button.builder(Component.literal(entity.getMissileInventoryName()), b -> actionPerformed(3)).bounds(width / 2 + 2, height / 2 - 49, 58, 20).build();
		missileButton.active = type.numMissileSlots > 0;
		addRenderableWidget(missileButton);
		
		//Mine / Bomb Button
		Button bombButton = Button.builder(Component.literal(entity.getBombInventoryName()), b -> actionPerformed(5)).bounds(width / 2 + 2, height / 2 - 27, 58, 20).build();
		bombButton.active = type.numBombSlots > 0;
		addRenderableWidget(bombButton);
		
		//Repair button
		addRenderableWidget(Button.builder(Component.literal("Repair"), b -> actionPerformed(4)).bounds(width / 2 - 60, height / 2 - 27, 58, 20).build());
	}
	
	private void actionPerformed(int id)
	{
		//Replace with a packet requesting the GUI from the server
		if(id == 0) //Cargo
		{
			FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.CARGO));
		}
		if(id == 1) //Guns
		{
			FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.GUNS));
		}
		if(id == 2) //Fuel
		{
			FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.FUEL));
		}
		if(id == 3) //Missiles
		{
			FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.MISSILES));
		}
		if(id == 4) //Repair
		{
			//No server side required. No interactive slots in this one
			Minecraft.getInstance().setScreen(new GuiDriveableRepair(inventory.player));
		}
		if(id == 5) //Bombs
		{
			FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.BOMBS));
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY)
	{
		if(entity == null)
			return;
		extractor.text(font, Component.literal(entity.getDriveableType().name), 6, 6, 0x404040);
		extractor.text(font, Component.literal("Inventory"), 8, (imageHeight - 96) + 2, 0x404040);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		super.extractBackground(extractor, mouseX, mouseY, partialTick);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
