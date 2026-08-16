package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.FlansMod;
import com.flansmod.common.teams.RoundFinishedData;

public class PacketVoting extends PacketBase
{
	public RoundFinishedData roundFinishedData = new RoundFinishedData();
	
	public PacketVoting()
	{
		
	}
	
	public PacketVoting(RoundFinishedData data)
	{
		roundFinishedData = data;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		roundFinishedData.WriteNumVotesUpdate(data);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		roundFinishedData.ReadNumVotesUpdate(data);
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		FlansMod.log.warn("Received vote info packet on server. Rejecting.");
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		ClientTeamsData.UpdateNumVotes(roundFinishedData);
	}
}
