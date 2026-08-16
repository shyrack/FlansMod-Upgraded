package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.FlansMod;
import com.flansmod.common.teams.RewardBox;
import com.flansmod.common.teams.TeamsManagerRanked;

public class PacketOpenRewardBox extends PacketBase
{
	public int boxHash = 0;
	public int unlockHash = 0;
	
	public PacketOpenRewardBox()
	{
	}
	
	/**
	 * Server to client reward request packet
	 */
	public PacketOpenRewardBox(RewardBox box)
	{
		boxHash = box.hashCode();
	}
	
	public PacketOpenRewardBox(int box, int unlock)
	{
		boxHash = box;
		unlockHash = unlock;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(boxHash);
		data.writeInt(unlockHash);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		boxHash = data.readInt();
		unlockHash = data.readInt();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		RewardBox box = RewardBox.GetRewardBox(boxHash);
		if(box == null)
		{
			FlansMod.Assert(false, "Recieved invalid reward box open packet from player " + playerEntity.getName().getString());
		}
		else
		{
			FlansMod.log.info("Recieved reward box open packet from player " + playerEntity.getName().getString() + " for box " + box.shortName);
			TeamsManagerRanked.OpenRewardBox(playerEntity, box);
		}
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		ClientTeamsData.UnlockReward(boxHash, unlockHash);
	}
}
