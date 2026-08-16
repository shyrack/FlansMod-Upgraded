package com.flansmod.client.gui.teams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import com.flansmod.client.FlansModClient;
import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.network.PacketTeamInfo;
import com.flansmod.common.teams.Team;

public class GuiTeamScores extends GuiTeamsBase
{
	public static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/teamsscores.png");
	public static final Identifier texture2 = Identifier.fromNamespaceAndPath("flansmod", "gui/teamsscores2.png");
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
		
		PacketTeamInfo teamInfo = FlansModClient.teamInfo;
		if(teamInfo == null || teamInfo.gametype == null || teamInfo.gametype.equals("") || teamInfo.teamData == null || teamInfo.teamData.length < 1)
		{
			Minecraft.getInstance().setScreen(null);
			return;
		}
		
		if(teamInfo.sortedByTeam)
		{
			renderTwoTeamGUI(extractor, teamInfo);
		}
		else renderDMGUI(extractor, teamInfo);
	}
	
	public void renderTwoTeamGUI(GuiGraphicsExtractor extractor, PacketTeamInfo teamInfo)
	{
		int k = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int l = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		extractMenuBackground(extractor);
		
		int guiHeight = 68 + 9 * teamInfo.numLines;
		
		int m = k / 2 - 156;
		int n = l / 2 - guiHeight / 2;
		
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture2, m, n, 100, 0, 312, 65, 512, 256);
		for(int p = 0; p < teamInfo.numLines; p++)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture2, m, n + 65 + 16 * p, 100, 65, 312, 16, 512, 256);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture2, m, n + 65 + (teamInfo.numLines) * 16, 100, 170, 312, 10, 512, 256);
		
		if(teamInfo.showZombieScore)
		{
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture2, m + 103, n + 51, 412, 0, 29, 11, 512, 256);
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture2, m + 254, n + 51, 412, 0, 29, 11, 512, 256);
		}
		
		extractor.text(font, teamInfo.map, m + 6, n + 6, 0xffffff);
		extractor.text(font, teamInfo.gametype, m + 312 - 6 - font.width(teamInfo.gametype), n + 6, 0xffffff);
		
		if(teamInfo.roundOver())
		{
			Team winners = teamInfo.getWinner();
			//Time limit was hit
			if(winners == null)
			{
				extractor.text(font, "Time Ran Out!", m + 10, n + 20, 0xffffff);
			}
			else
			{
				extractor.text(font, winners.name + " Won!", m + 10, n + 20, 0xffffff);
			}
			
			extractor.text(font, Math.max(ClientTeamsData.timeLeftInStage / 20, 0) + "", m + 312 - 22, n + 20, 0xffffff);
			
		}
		else
		{
			int secondsLeft = teamInfo.timeLeft / 20;
			int minutesLeft = secondsLeft / 60;
			secondsLeft = secondsLeft % 60;
			extractor.text(font, "Time Left : " + minutesLeft + ":" + (secondsLeft < 10 ? "0" + secondsLeft : secondsLeft), m + 10, n + 20, 0xffffff);
			extractor.text(font, "Score Limit : " + teamInfo.scoreLimit, m + 302 - font.width("Score Limit : " + teamInfo.scoreLimit), n + 20, 0xffffff);
		}
		
		for(int i = 0; i < 2; i++)
		{
			extractor.text(font, "\u00a7" + teamInfo.teamData[i].team.textColour + teamInfo.teamData[i].team.name, m + 10 + 151 * i, n + 39, 0xffffff);
			extractor.text(font, "\u00a7" + teamInfo.teamData[i].team.textColour + teamInfo.teamData[i].score, m + 133 + 151 * i, n + 39, 0xffffff);
			for(int j = 0; j < teamInfo.teamData[i].numPlayers; j++)
			{
				if(teamInfo.teamData[i].playerData[j] == null)
					continue;
				DrawRankIcon(extractor, teamInfo.teamData[i].playerData[j].level, 0, m + 10 + 151 * i, n + 65 + 16 * j, false);
				extractor.text(font, teamInfo.teamData[i].playerData[j].username, m + 30 + 151 * i, n + 68 + 16 * j, 0xffffff);
				extractor.centeredText(font, "" + teamInfo.teamData[i].playerData[j].score, m + 111 + 151 * i, n + 68 + 16 * j, 0xffffff);
				extractor.centeredText(font, "" + (teamInfo.showZombieScore ? teamInfo.teamData[i].playerData[j].zombieScore : teamInfo.teamData[i].playerData[j].kills), m + 127 + 151 * i, n + 68 + 16 * j, 0xffffff);
				extractor.centeredText(font, "" + teamInfo.teamData[i].playerData[j].deaths, m + 143 + 151 * i, n + 68 + 16 * j, 0xffffff);
			}
		}
	}
	
	public void renderDMGUI(GuiGraphicsExtractor extractor, PacketTeamInfo teamInfo)
	{
		long newTime = Minecraft.getInstance().level.getLevelData().getGameTime();
		int k = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int l = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		extractMenuBackground(extractor);
		
		int guiHeight = 34 + 9 * teamInfo.numLines;
		int m = k / 2 - 128;
		int n = l / 2 - guiHeight / 2;
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, m, n, 0, 45, 256, 24, 256, 256);
		for(int p = 0; p < teamInfo.numLines; p++)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, m, n + 24 + 9 * p, 0, 71, 256, 9, 256, 256);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, m, l / 2 + guiHeight / 2 - 10, 0, 87, 256, 10, 256, 256);
		
		extractor.centeredText(font, teamInfo.gametype, k / 2, n + 4, 0xffffff);
		extractor.text(font, "Name", m + 8, n + 14, 0xffffff);
		extractor.text(font, "Score", m + 100, n + 14, 0xffffff);
		extractor.text(font, "Kills", m + 150, n + 14, 0xffffff);
		extractor.text(font, "Deaths", m + 200, n + 14, 0xffffff);
		int line = 0;
		if(teamInfo.sortedByTeam)
		{
			for(int p = 0; p < teamInfo.numTeams; p++)
			{
				if(teamInfo.teamData[p] == null || teamInfo.teamData[p].team == null)
					continue;
				extractor.text(font, "\u00a7" + teamInfo.teamData[p].team.textColour + teamInfo.teamData[p].team.name, m + 8, n + 25 + 9 * line, 0xffffff);
				extractor.text(font, "" + teamInfo.teamData[p].score, m + 100, n + 25 + 9 * line, 0xffffff);
				line++;
				for(int q = 0; q < teamInfo.teamData[p].numPlayers; q++)
				{
					extractor.text(font, teamInfo.teamData[p].playerData[q].username, m + 8, n + 25 + 9 * line, 0xffffff);
					extractor.text(font, "" + teamInfo.teamData[p].playerData[q].score, m + 100, n + 25 + 9 * line, 0xffffff);
					extractor.text(font, "" + teamInfo.teamData[p].playerData[q].kills, m + 150, n + 25 + 9 * line, 0xffffff);
					extractor.text(font, "" + teamInfo.teamData[p].playerData[q].deaths, m + 200, n + 25 + 9 * line, 0xffffff);
					line++;
				}
			}
		}
		else
		{
			for(int q = 0; q < teamInfo.teamData[0].numPlayers; q++)
			{
				extractor.text(font, teamInfo.teamData[0].playerData[q].username, m + 8, n + 25 + 9 * line, 0xffffff);
				extractor.text(font, "" + teamInfo.teamData[0].playerData[q].score, m + 100, n + 25 + 9 * line, 0xffffff);
				extractor.text(font, "" + teamInfo.teamData[0].playerData[q].kills, m + 150, n + 25 + 9 * line, 0xffffff);
				extractor.text(font, "" + teamInfo.teamData[0].playerData[q].deaths, m + 200, n + 25 + 9 * line, 0xffffff);
				line++;
			}
		}
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
	
	@Override
	protected boolean AllowEscape()
	{
		return true;
	}
}
