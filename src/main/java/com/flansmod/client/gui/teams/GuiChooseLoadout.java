package com.flansmod.client.gui.teams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.FlansMod;
import com.flansmod.common.teams.LoadoutPool;
import com.flansmod.common.teams.PlayerRankData;
import com.flansmod.common.teams.TeamsManagerRanked;

public class GuiChooseLoadout extends GuiTeamsBase
{
	/**
	 * The background image
	 */
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/landingpage.png");
	
	public GuiChooseLoadout()
	{
		super();
	}
	
	@Override
	public void init()
	{
		super.init();
		
		guiOriginX = width / 2 - 128;
		guiOriginY = height / 2 - 99;
		
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		if(data == null || pool == null)
		{
			FlansMod.log.warn("Problem in choose loadout page!");
			return;
		}
		
		for(int i = 0; i < 5; i++)
		{
			final int loadout = i;
			if(data.currentLevel >= pool.slotUnlockLevels[i])
			{
				addRenderableWidget(
						Button.builder(Component.literal("Select"), b ->
						{
							TeamsManagerRanked.ChooseLoadout(loadout);
							Minecraft.getInstance().setScreen(null);
						}).bounds(width / 2 - 128 + 12 + 49 * i, height / 2 - 99 + 117, 36, 20).build());
			}
		}
		
		addRenderableWidget(Button.builder(Component.literal("<< Change Team"), b -> ClientTeamsData.OpenTeamSelectPage()).bounds(width / 2 - 128 + 7, height / 2 - 99 + 144, 88, 20).build());
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		extractMenuBackground(extractor);
		
		guiOriginX = width / 2 - 128;
		guiOriginY = height / 2 - 99;
		
		int textureX = 512;
		int textureY = 256;
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		if(data == null || pool == null)
		{
			FlansMod.log.warn("Problem in choose loadout page!");
			return;
		}
		
		//Draw the background
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX, guiOriginY, 0F, 0F, 256, 143, textureX, textureY);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX, guiOriginY + 143, 256, 180, 256, 76, textureX, textureY);
		
		// Draw text
		extractor.centeredText(font, "Choose a loadout", guiOriginX + 128, guiOriginY + 12, 0xffffff);
		
		// Draw loadout panels
		for(int n = 0; n < 5; n++)
		{
			DrawLoadoutPanel(extractor, pool, data, guiOriginX + 7 + 49 * n, guiOriginY + 28, n);
		}
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
