package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.teams.RoundFinishedData;

public class PacketRoundFinished extends PacketBase
{
	public RoundFinishedData roundFinishedData = new RoundFinishedData();
	
	public PacketRoundFinished()
	{
	}
	
	public PacketRoundFinished(RoundFinishedData data)
	{
		roundFinishedData = data;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		roundFinishedData.WriteInitialData(data);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		roundFinishedData.ReadInitialData(data);
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		ClientTeamsData.SetRoundFinishedData(roundFinishedData);
		ClientTeamsData.StartTimers();
	}
	
}
