package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.teams.TeamsManager;

public class PacketVoteCast extends PacketBase
{
	public int vote;
	
	public PacketVoteCast()
	{
	}
	
	public PacketVoteCast(int vote)
	{
		this.vote = vote;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeByte(vote);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		vote = data.readByte();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		if(vote < 0 || vote > TeamsManager.getInstance().voteOptions.length)
		{
			FlansMod.log.warn("Invalid vote " + vote + " from " + playerEntity.getName());
			return;
		}
		PlayerData data = PlayerHandler.getPlayerData(playerEntity);
		data.vote = vote;
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.log.warn("Received vote cast packet on client. Skipping.");
	}
	
}
