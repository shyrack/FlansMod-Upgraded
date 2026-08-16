package com.flansmod.client.gui.teams;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.FlansMod;
import com.flansmod.common.network.PacketVoteCast;

public class GuiVoting extends Screen
{
	public static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/vote.png");
	public static int myVote = 0;
	private int guiHeight;
	
	public GuiVoting()
	{
		super(Component.literal(""));
		myVote = 0;
	}
	
	@Override
	public void init()
	{
		super.init();
		this.clearWidgets();
		
		guiHeight = 29 + ClientTeamsData.roundFinishedData.votingOptions.length * 24;
		
		//Add buttons
		for(int i = 0; i < ClientTeamsData.roundFinishedData.votingOptions.length; i++)
		{
			final int vote = i + 1;
			addRenderableWidget(Button.builder(Component.literal("Vote"), b ->
			{
				myVote = vote;
				FlansMod.getPacketHandler().sendToServer(new PacketVoteCast(myVote));
			}).bounds(width / 2 + 128 - 50, height / 2 - guiHeight / 2 + 24 + 24 * i, 40, 20).build());
		}
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		int k = width;
		int l = height;
		extractMenuBackground(extractor);
		
		int m = k / 2 - 128;
		int n = l / 2 - guiHeight / 2;
		
		
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, m, n, 0F, 0F, 256, 22, 256, 256);
		for(int p = 0; p < ClientTeamsData.roundFinishedData.votingOptions.length; p++)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, m, n + 22 + 24 * p, 0F, 23F, 256, 24, 256, 256);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, m, l / 2 + guiHeight / 2 - 6, 0F, 73F, 256, 7, 256, 256);
		
		extractor.text(font, "Vote for the Next Round", m + 8, n + 8, 0xffffff);
		extractor.text(font, (ClientTeamsData.timeLeftTotal / 20) + "", m + 256 - 20, n + 8, 0xffffff);
		
		for(int p = 0; p < ClientTeamsData.roundFinishedData.votingOptions.length; p++)
		{
			extractor.text(font, ClientTeamsData.roundFinishedData.votingOptions[p].mapName, m + 10, n + 25 + 24 * p, 0xffffff);
			extractor.text(font, ClientTeamsData.roundFinishedData.votingOptions[p].gametype + " : \u00a7" + ClientTeamsData.roundFinishedData.votingOptions[p].teamNames[0] + ", \u00a7" + ClientTeamsData.roundFinishedData.votingOptions[p].teamNames[1], m + 10, n + 35 + 24 * p, 0xffffff);
			
			extractor.centeredText(font, (myVote == p + 1 ? "\u00a72" : "") + ClientTeamsData.roundFinishedData.votingOptions[p].numVotes, m + 196, n + 31 + 24 * p, 0xffffff);
		}
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
