package com.flansmod.client.gui.teams;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import com.flansmod.client.FlansModClient;
import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.network.PacketTeamInfo;
import com.flansmod.common.teams.LoadoutPool;
import com.flansmod.common.teams.LoadoutPool.LoadoutEntryInfoType;
import com.flansmod.common.teams.PlayerRankData;
import com.flansmod.common.teams.RewardBox;

public class GuiMissionResults extends GuiTeamsBase
{
	/**
	 * The background image
	 */
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/missionresults.png");
	
	private static final int WIDTH = 256, HEIGHT = 256;
	
	private enum EnumResultsState
	{
		IDLE,
		
		SHOW_LINE_1_XP,
		SHOW_LINE_2_VICTORY_BONUS,
		SHOW_LINE_3,
		SHOW_LINE_4,
		SHOW_LINE_5_TOTAL,
		
		INCREASE_XP_BAR,
		LEVEL_UP,
		REVEAL_UNLOCK1,
		REVEAL_UNLOCK2,
		REVEAL_UNLOCK3,
		REVEAL_UNLOCK4,
		
		DONE
	}
	
	private static final int[] stateTimes = new int[]{
			0,
			
			4,
			4,
			4,
			4,
			4,
			
			20,
			20,
			5,
			5,
			5,
			25,
			
			0
	};
	
	private static class MissionResultsUnlock
	{
		public boolean isWeapon = true;
		public LoadoutEntryInfoType loadoutEntry = null;
		public RewardBox rewardBox = null;
	}
	
	private int timeInState = 0;
	
	// Temp rank data
	private int displayRank, displayXP, earnedXP;
	private int targetRank, targetXP;
	
	private int lastXP;
	private boolean hasLevelledUp = false, hasDoneFinalLevelUp = false;
	
	private EnumResultsState state = EnumResultsState.IDLE;
	
	private MissionResultsUnlock[] unlocks = new MissionResultsUnlock[4];
	
	public GuiMissionResults()
	{
		super();
		
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		if(data == null || pool == null)
		{
			FlansMod.log.warn("Problem in mission results!");
			return;
		}
		
		state = EnumResultsState.SHOW_LINE_1_XP;
		
		displayRank = data.currentLevel;
		lastXP = displayXP = data.currentXP;
		
		earnedXP = data.pendingXP;
		
		targetXP = data.currentXP + data.pendingXP;
		targetRank = data.currentLevel;
		
		
		int XPForNextLevel = pool.GetXPForLevel(targetRank + 1);
		while(XPForNextLevel > 0 && targetXP >= XPForNextLevel)
		{
			targetXP -= XPForNextLevel;
			targetRank++;
			
			XPForNextLevel = pool.GetXPForLevel(targetRank + 1);
		}
		
		hasLevelledUp = false;
	}
	
	@Override
	public void init()
	{
		super.init();
		
		guiOriginX = width / 2 - WIDTH / 2;
		guiOriginY = height / 2 - HEIGHT / 2;
		
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> Minecraft.getInstance().setScreen(new GuiTeamScores())).bounds(width / 2 - WIDTH / 2 + 214, height / 2 - HEIGHT / 2 + 6, 36, 20).build());
	}
	
	@Override
	public void tick()
	{
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		if(data == null || pool == null)
		{
			FlansMod.log.warn("Problem in mission results!");
			return;
		}
		
		timeInState++;
		
		switch(state)
		{
			case IDLE:
			{
				break;
			}
			case SHOW_LINE_1_XP:
			case SHOW_LINE_2_VICTORY_BONUS:
			case SHOW_LINE_3:
			case SHOW_LINE_4:
			case SHOW_LINE_5_TOTAL:
			{
				if(timeInState > stateTimes[state.ordinal()])
				{
					EnterState(EnumResultsState.values()[(state.ordinal() + 1)]);
				}
				break;
			}
			case INCREASE_XP_BAR:
			{
				int currentTarget = targetXP;
				if(targetRank > displayRank)
				{
					currentTarget = pool.GetXPForLevel(displayRank + 1);
				}
				displayXP = Mth.floor(lastXP + ((float)(currentTarget - lastXP) * (float)(timeInState - 1) / (float)stateTimes[state.ordinal()]));
				
				if(timeInState > stateTimes[state.ordinal()])
				{
					if(targetRank > displayRank)
					{
						EnterState(EnumResultsState.LEVEL_UP);
					}
					else
					{
						EnterState(EnumResultsState.DONE);
					}
				}
				break;
			}
			case LEVEL_UP:
			case REVEAL_UNLOCK1:
			case REVEAL_UNLOCK2:
			case REVEAL_UNLOCK3:
			{
				if(timeInState > stateTimes[state.ordinal()])
				{
					EnterState(EnumResultsState.values()[(state.ordinal() + 1)]);
				}
				break;
			}
			case REVEAL_UNLOCK4:
			{
				if(timeInState > stateTimes[state.ordinal()])
				{
					EnterState(EnumResultsState.INCREASE_XP_BAR);
				}
				break;
			}
			case DONE:
				break;
			default:
				break;
		}
	}
	
	private void EnterState(EnumResultsState newState)
	{
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		if(data == null || pool == null)
		{
			FlansMod.log.warn("Problem in mission results!");
			return;
		}
		
		// Do exit checks?
		
		state = newState;
		timeInState = 0;
		
		switch(state)
		{
			case INCREASE_XP_BAR:
			{
				lastXP = displayXP;
				break;
			}
			case LEVEL_UP:
			{
				displayRank++;
				displayXP = 0;
				
				hasLevelledUp = true;
				
				for(int i = 0; i < 4; i++)
				{
					unlocks[i] = null;
				}
				
				int index = 0;
				
				for(ArrayList<LoadoutEntryInfoType> list : pool.unlocks)
				{
					for(LoadoutEntryInfoType entry : list)
					{
						if(index >= 4) continue;
						
						boolean conflict = false;
						for(int i = 0; i < index; i++)
						{
							if(unlocks[i].isWeapon && unlocks[i].loadoutEntry != null &&
									unlocks[i].loadoutEntry.type.shortName.equals(entry.type.shortName))
								conflict = true;
						}
						if(conflict) continue;
						
						if(entry.unlockLevel == displayRank)
						{
							unlocks[index] = new MissionResultsUnlock();
							unlocks[index].isWeapon = true;
							unlocks[index].loadoutEntry = entry;
							index++;
						}
					}
				}
				
				for(RewardBox box : pool.rewardsPerLevel[displayRank - 1])
				{
					unlocks[index] = new MissionResultsUnlock();
					unlocks[index].isWeapon = false;
					unlocks[index].rewardBox = box;
					index++;
				}
				
				break;
			}
			case REVEAL_UNLOCK4:
			{
				if(displayRank == targetRank)
				{
					hasDoneFinalLevelUp = true;
				}
				break;
			}
			default: break;
		}
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		PacketTeamInfo teamInfo = FlansModClient.teamInfo;
		if(teamInfo == null || teamInfo.gametype == null || teamInfo.gametype.equals("") || teamInfo.teamData == null || teamInfo.teamData.length < 1 || !teamInfo.roundOver())
		{
			Minecraft.getInstance().setScreen(null);
			return;
		}
		
		extractMenuBackground(extractor);
		
		guiOriginX = width / 2 - WIDTH / 2;
		guiOriginY = height / 2 - HEIGHT / 2;
		
		int textureX = 512;
		int textureY = 256;
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		if(data == null || pool == null)
		{
			FlansMod.log.warn("Problem in mission results!");
			return;
		}
		
		//Draw the background
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX, guiOriginY, 0F, 0F, WIDTH, HEIGHT, textureX, textureY);
		
		int XPForNextLevel = pool.GetXPForLevel(displayRank + 1);
		float XPProgress = 0.0f;
		if(XPForNextLevel > 0)
		{
			XPProgress = (float)displayXP / (float)XPForNextLevel;
		}
		else
		{
			XPProgress = 1.0f;
		}
		XPProgress = Mth.clamp(XPProgress, 0.0f, 1.0f);
		
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX + 7, guiOriginY + 109, 259, 109, (int)(242 * XPProgress), 10, textureX, textureY);
		
		if(state.ordinal() >= EnumResultsState.LEVEL_UP.ordinal() && state.ordinal() <= EnumResultsState.REVEAL_UNLOCK4.ordinal())
		{
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX + 5, guiOriginY + 120, 266, 120, 246, 38, textureX, textureY);
		}
		
		if(XPForNextLevel > 0)
		{
			extractor.centeredText(font, displayXP + " / " + XPForNextLevel, guiOriginX + 128, guiOriginY + 110, 0xffffff);
		}
		else
		{
			extractor.centeredText(font, "" + displayXP, guiOriginX + 128, guiOriginY + 110, 0xffffff);
		}
		
		// Draw text
		extractor.text(font, (ClientTeamsData.timeLeftInStage / 20) + "", guiOriginX + 12, guiOriginY + 12, 0xffffff);
		extractor.centeredText(font, "ROUND OVER", guiOriginX + 128, guiOriginY + 12, 0xffffff);
		
		if(state.ordinal() >= EnumResultsState.LEVEL_UP.ordinal() && state.ordinal() <= EnumResultsState.REVEAL_UNLOCK4.ordinal())
		{
			extractor.centeredText(font, "RANK INCREASED", guiOriginX + 128, guiOriginY + 135, 0xffffff);
		}
		else
		{
			extractor.text(font, "Rank " + displayRank, guiOriginX + 44, guiOriginY + 135, 0xffffff);
			extractor.text(font, "Next Rank", guiOriginX + 163, guiOriginY + 135, 0xffffff);
		}
		
		if(state.ordinal() >= EnumResultsState.SHOW_LINE_1_XP.ordinal())
		{
			extractor.text(font, "XP Earned: ", guiOriginX + 11, guiOriginY + 31, 0xffffff);
			extractor.text(font, "" + earnedXP, guiOriginX + 244 - font.width("" + earnedXP), guiOriginY + 31, 0xffffff);
		}
		
		if(state.ordinal() >= EnumResultsState.SHOW_LINE_2_VICTORY_BONUS.ordinal())
			extractor.text(font, "", guiOriginX + 11, guiOriginY + 41, 0xffffff);
		
		if(state.ordinal() >= EnumResultsState.SHOW_LINE_3.ordinal())
			extractor.text(font, "", guiOriginX + 11, guiOriginY + 51, 0xffffff);
		
		if(state.ordinal() >= EnumResultsState.SHOW_LINE_4.ordinal())
			extractor.text(font, "", guiOriginX + 11, guiOriginY + 61, 0xffffff);
		
		if(state.ordinal() >= EnumResultsState.SHOW_LINE_5_TOTAL.ordinal())
		{
			extractor.text(font, "Total: ", guiOriginX + 11, guiOriginY + 91, 0xffffff);
			extractor.text(font, "" + earnedXP, guiOriginX + 244 - font.width("" + earnedXP), guiOriginY + 91, 0xffffff);
		}
		
		// Draw rank icon
		DrawRankIcon(extractor, displayRank, 0, 8, 123, true);
		
		if(displayRank < pool.maxLevel)
		{
			DrawRankIcon(extractor, displayRank + 1, 0, 216, 123, true);
		}
		
		
		boolean hasDoneFinalLevel = hasDoneFinalLevelUp;
		
		if(state.ordinal() >= EnumResultsState.REVEAL_UNLOCK1.ordinal() || hasDoneFinalLevel)
			DrawUnlock(extractor, unlocks[0], guiOriginX + 8, guiOriginY + 160);
		
		if(state.ordinal() >= EnumResultsState.REVEAL_UNLOCK2.ordinal() || hasDoneFinalLevel)
			DrawUnlock(extractor, unlocks[1], guiOriginX + 131, guiOriginY + 160);
		
		if(state.ordinal() >= EnumResultsState.REVEAL_UNLOCK3.ordinal() || hasDoneFinalLevel)
			DrawUnlock(extractor, unlocks[2], guiOriginX + 8, guiOriginY + 207);
		
		if(state.ordinal() >= EnumResultsState.REVEAL_UNLOCK4.ordinal() || hasDoneFinalLevel)
			DrawUnlock(extractor, unlocks[3], guiOriginX + 131, guiOriginY + 207);
	}
	
	private void DrawUnlock(GuiGraphicsExtractor extractor, MissionResultsUnlock entry, int i, int j)
	{
		if(entry == null) return;
		
		if(entry.isWeapon)
		{
			extractor.centeredText(font, "New item unlocked", i + 58, j + 2, 0xffffff);
			extractor.centeredText(font, entry.loadoutEntry.type.name, i + 58, j + 31, 0xffffff);
			
			if(entry.loadoutEntry.type instanceof GunType)
			{
				DrawGun(extractor, new ItemStack(entry.loadoutEntry.type.getItem()), i + 50, j + 24, 25.0f);
			}
			else
			{
				drawSlotInventory(extractor, new ItemStack(entry.loadoutEntry.type.getItem()), i + 49, j + 12);
			}
		}
		else
		{
			extractor.centeredText(font, "Reward obtained", i + 58, j + 2, 0xffffff);
			extractor.centeredText(font, entry.rewardBox.name, i + 58, j + 31, 0xffffff);
			drawSlotInventory(extractor, new ItemStack(entry.rewardBox.getItem()), i + 49, j + 12);
		}
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
