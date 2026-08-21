package com.flansmod.client.teams;

import com.flansmod.common.teams.Team;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import com.flansmod.client.gui.teams.GuiChooseLoadout;

/**
 * Client-only teams GUI glue. The common TeamsManagerRanked calls into this
 * class reflectively so a dedicated server never resolves any client class
 * (Screen/GuiChooseLoadout/Minecraft) from its bytecode.
 */
public class TeamsClientHook
{
	private TeamsClientHook()
	{
	}

	/** TeamsManagerRanked.SelectTeam: close the screen or open the loadout GUI. */
	public static void selectTeam(Team team)
	{
		Minecraft mc = Minecraft.getInstance();
		if(team == null)
		{
			mc.setScreen(null);
		}
		else
		{
			mc.setScreen(new GuiChooseLoadout());
		}
	}

	/** TeamsManagerRanked.ConfirmLoadoutChanges: send the local loadout to the server. */
	public static void confirmLoadoutChanges()
	{
		com.flansmod.common.network.PacketLoadoutData packet = new com.flansmod.common.network.PacketLoadoutData();
		packet.myRankData = ClientTeamsData.theRankData;
		com.flansmod.common.FlansMod.getPacketHandler().sendToServer(packet);
	}

	/** TeamsManagerRanked.LocalPlayerOwnsUnlock: unlock check against local data. */
	public static boolean localPlayerOwnsUnlock(int unlockHash)
	{
		return ClientTeamsData.theRankData.OwnsUnlock(unlockHash);
	}
}
