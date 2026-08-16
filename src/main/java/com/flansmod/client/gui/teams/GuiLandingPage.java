package com.flansmod.client.gui.teams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.FlansMod;
import com.flansmod.common.teams.LoadoutPool;
import com.flansmod.common.teams.PlayerRankData;
import com.flansmod.common.teams.RewardBox;

public class GuiLandingPage extends GuiTeamsBase
{
	/**
	 * The background image
	 */
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/landingpage.png");
	
	private static final int WIDTH = 256, HEIGHT = 215;
	
	public GuiLandingPage()
	{
		super();
	}
	
	@Override
	public void init()
	{
		super.init();
		
		guiOriginX = width / 2 - WIDTH / 2;
		guiOriginY = height / 2 - HEIGHT / 2;
		
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		if(data == null || pool == null)
		{
			FlansMod.log.warn("Problem in landing page!");
			Minecraft.getInstance().setScreen(null);
			return;
		}
		
		for(int i = 0; i < 5; i++)
		{
			final int loadout = i;
			if(data.currentLevel >= pool.slotUnlockLevels[i])
			{
				addRenderableWidget(
						Button.builder(Component.literal("Edit"), b -> ClientTeamsData.OpenEditLoadoutPage(loadout)).bounds(width / 2 - WIDTH / 2 + 12 + 49 * i, height / 2 - HEIGHT / 2 + 117, 36, 20).build());
			}
		}
		
		addRenderableWidget(Button.builder(Component.literal("Play >>"), b -> ClientTeamsData.OpenTeamSelectPage()).bounds(width / 2 - WIDTH / 2 + 202, height / 2 - HEIGHT / 2 + 162, 47, 20).build());
		
		for(int i = 0; i < 3; i++)
		{
			int numBoxes = data.GetNumOfUnopenedBoxes(pool.rewardBoxes[i]);
			
			final int box = i;
			Button button = addRenderableWidget(Button.builder(Component.literal("Open"), b -> ClientTeamsData.OpenRewardBox(box)).bounds(width / 2 - WIDTH / 2 + 9 + 65 * i, height / 2 - HEIGHT / 2 + 187, 59, 20).build());
			button.active = numBoxes > 0;
		}
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		extractMenuBackground(extractor);
		
		guiOriginX = width / 2 - WIDTH / 2;
		guiOriginY = height / 2 - HEIGHT / 2;
		
		int textureX = 512;
		int textureY = 256;
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		if(data == null || pool == null)
		{
			FlansMod.log.warn("Problem in landing page!");
			Minecraft.getInstance().setScreen(null);
			return;
		}
		
		//Draw the background
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX, guiOriginY, 0F, 0F, WIDTH, HEIGHT, textureX, textureY);
		
		int XPForNextLevel = pool.GetXPForLevel(data.currentLevel + 1);
		float XPProgress = 0.0f;
		if(XPForNextLevel > 0)
		{
			XPProgress = (float)data.currentXP / (float)XPForNextLevel;
		}
		else
		{
			XPProgress = 1.0f;
		}
		
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX + 106, guiOriginY + 146, 259, 164, (int)(92.0f * XPProgress), 16, textureX, textureY);
		
		// Draw text
		extractor.centeredText(font, ClientTeamsData.motd, guiOriginX + 128, guiOriginY + 12, 0xffffff);
		
		extractor.text(font, Minecraft.getInstance().player.getName().getString(), guiOriginX + 30, guiOriginY + 150, 0xffffff);
		extractor.centeredText(font, "Rank " + data.currentLevel, guiOriginX + 154, guiOriginY + 150, 0xffffff);
		
		// Draw rank icon
		DrawRankIcon(extractor, data.currentLevel, 0, 9, 146, false);
		
		// Draw loadout panels
		for(int n = 0; n < 5; n++)
		{
			DrawLoadoutPanel(extractor, pool, data, guiOriginX + 7 + 49 * n, guiOriginY + 28, n);
		}
		
		// Draw reward box panels
		for(int n = 0; n < 3; n++)
		{
			DrawRewardBoxPanel(extractor, pool, data, guiOriginX + 7 + 65 * n, guiOriginY + 166, n);
		}
	}
	
	private void DrawRewardBoxPanel(GuiGraphicsExtractor extractor, LoadoutPool pool, PlayerRankData data, int x, int y, int index)
	{
		RewardBox box = pool.rewardBoxes[index];
		drawSlotInventory(extractor, new ItemStack(box.getItem()), x + 3, y + 3);
		extractor.centeredText(font, "x " + data.GetNumOfUnopenedBoxes(box), x + 33, y + 7, 0xffffff);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
