package com.flansmod.client.gui.teams;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.flansmod.common.FlansMod;
import com.flansmod.common.network.PacketBaseEdit;

public class GuiBaseEditor extends Screen
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/baseedit.png");
	private int guiOriginX;
	private int guiOriginY;
	private EditBox nameEntryField;
	private Button[] teamButtons;
	private Button[] mapButtons;
	private Button leftButton;
	private Button rightButton;
	private int mapsPage;
	
	/**
	 * The packet received from the server containing all the base information. Modify this and send it back
	 */
	public PacketBaseEdit packet;
	
	public GuiBaseEditor(PacketBaseEdit packet)
	{
		super(Component.literal(""));
		this.packet = packet;
	}
	
	@Override
	public void init()
	{
		super.init();
		this.clearWidgets();
		//Setup the text entry field
		nameEntryField = addRenderableWidget(new EditBox(font, width / 2 - 128 + 70, height / 2 - 94 + 24, 179, 16, Component.literal("")));
		nameEntryField.setMaxLength(60);
		nameEntryField.setBordered(true);
		nameEntryField.setVisible(true);
		nameEntryField.setFocused(true);
		nameEntryField.setTextColor(16777215);
		nameEntryField.setValue(packet.baseName);
		
		//Add buttons
		teamButtons = new Button[4];
		teamButtons[0] = addRenderableWidget(Button.builder(Component.literal("No Team"), b -> actionPerformed(0)).bounds(width / 2 - 128 + 6, height / 2 - 94 + 38, 58, 20).build());
		teamButtons[1] = addRenderableWidget(Button.builder(Component.literal("Spectator"), b -> actionPerformed(1)).bounds(width / 2 - 128 + 68, height / 2 - 94 + 38, 58, 20).build());
		teamButtons[2] = addRenderableWidget(Button.builder(Component.literal("Team 1"), b -> actionPerformed(2)).bounds(width / 2 - 128 + 130, height / 2 - 94 + 38, 58, 20).build());
		teamButtons[3] = addRenderableWidget(Button.builder(Component.literal("Team 2"), b -> actionPerformed(3)).bounds(width / 2 - 128 + 192, height / 2 - 94 + 38, 58, 20).build());
		
		mapButtons = new Button[5];
		for(int i = 0; i < 5; i++)
		{
			final int map = i;
			mapButtons[i] = addRenderableWidget(Button.builder(Component.literal("Map " + (i + 1)), b -> actionPerformed(4 + map)).bounds(width / 2 - 128 + 28, height / 2 - 94 + 75 + 22 * i, 200, 20).build());
		}
		
		leftButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> actionPerformed(9)).bounds(width / 2 - 128 + 6, height / 2 - 94 + 119, 20, 20).build());
		rightButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> actionPerformed(10)).bounds(width / 2 + 128 - 26, height / 2 - 94 + 119, 20, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		extractMenuBackground(extractor);
		int k = width;
		int l = height;
		int m = guiOriginX = k / 2 - 128;
		int n = guiOriginY = l / 2 - 94;
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, m, n, 0F, 0F, 256, 189, 256, 256);
		
		extractor.text(font, "Base Settings", guiOriginX + 6, guiOriginY + 6, 0xffffff);
		extractor.text(font, "Base Name : ", guiOriginX + 6, guiOriginY + 24, 0xffffff);
		extractor.text(font, "Map", guiOriginX + 6, guiOriginY + 64, 0xffffff);
	}
	
	private void actionPerformed(int id)
	{
		switch(id)
		{
			case 0: case 1: case 2: case 3:
			packet.teamID = id;
			break;
			case 4: case 5: case 6: case 7: case 8:
			packet.mapID = mapsPage * 5 + id - 4;
			break;
			case 9: mapsPage--;
				break;
			case 10: mapsPage++;
				break;
		}
	}
	
	@Override
	public void tick()
	{
		for(int i = 0; i < 4; i++)
		{
			teamButtons[i].active = packet.teamID != i;
		}
		for(int i = 0; i < 5; i++)
		{
			mapButtons[i].visible = packet.maps.length > i + mapsPage * 5;
			if(mapButtons[i].visible)
			{
				mapButtons[i].setMessage(Component.literal(packet.maps[i + mapsPage * 5]));
				mapButtons[i].active = i + mapsPage * 5 != packet.mapID;
			}
		}
		rightButton.visible = packet.maps.length > (mapsPage + 1) * 5;
		leftButton.visible = mapsPage > 0;
	}
	
	@Override
	public void onClose()
	{
		super.onClose();
		packet.baseName = nameEntryField.getValue();
		FlansMod.getPacketHandler().sendToServer(packet);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
