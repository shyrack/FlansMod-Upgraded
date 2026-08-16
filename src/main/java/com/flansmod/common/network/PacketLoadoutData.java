package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.client.gui.teams.EnumLoadoutSlot;
import com.flansmod.client.gui.teams.GuiTeamSelect;
import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.FlansMod;
import com.flansmod.common.teams.LoadoutPool;
import com.flansmod.common.teams.PlayerRankData;
import com.flansmod.common.teams.Team;
import com.flansmod.common.teams.TeamsManagerRanked;

public class PacketLoadoutData extends PacketBase
{
	public String motd = "";
	public Team[] teamsAvailable = new Team[0];
	public PlayerRankData myRankData = new PlayerRankData();
	public LoadoutPool currentPool = null;
	
	public PacketLoadoutData()
	{
		
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		writeUTF(data, motd);
		data.writeInt(teamsAvailable.length);
		for(Team aTeamsAvailable : teamsAvailable)
		{
			data.writeInt(aTeamsAvailable == null ? 0 : aTeamsAvailable.shortName.hashCode());
		}
		
		myRankData.writeToBuf(data);
		
		data.writeInt(currentPool == null ? 0 : currentPool.shortName.hashCode());
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		motd = readUTF(data);
		int numTeams = data.readInt();
		teamsAvailable = new Team[numTeams];
		for(int i = 0; i < teamsAvailable.length; i++)
		{
			teamsAvailable[i] = Team.getTeam(data.readInt());
		}
		
		myRankData.readFromBuf(data);
		
		currentPool = LoadoutPool.GetPool(data.readInt());
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		PlayerRankData rankData = TeamsManagerRanked.rankData.get(playerEntity.getUUID());
		if(rankData == null)
		{
			rankData = new PlayerRankData();
			TeamsManagerRanked.rankData.put(playerEntity.getUUID(), rankData);
		}
		
		// Client to server. The only bit they are authoritative on is their loadouts. But they still need to be checked for cheating.
		// TODO: Verify loadout is valid
		myRankData.currentLevel = rankData.currentLevel;
		if(myRankData.VerifyLoadouts())
		{
			rankData.loadouts = myRankData.loadouts;
		}
		else
		{
			FlansMod.Assert(false, "PLAYER " + playerEntity.getName().getString() + " GAVE INCORRECT LOADOUT.");
			LoadoutPool pool = TeamsManagerRanked.GetInstance().currentPool;
			if(pool != null)
			{
				for(int i = 0; i < 5; i++)
				{
					for(int j = 0; j < EnumLoadoutSlot.values().length; j++)
					{
						if(pool.defaultLoadouts[i].slots[j] != null)
						{
							rankData.loadouts[i].slots[j] = pool.defaultLoadouts[i].slots[j].copy();
						}
						else
							rankData.loadouts[i].slots[j] = null;
					}
				}
			}
		}
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		ClientTeamsData.motd = motd;
		ClientTeamsData.theRankData = myRankData;
		ClientTeamsData.currentPool = currentPool;
		GuiTeamSelect.teamChoices = teamsAvailable;
		ClientTeamsData.OpenLandingPage();
	}
}
