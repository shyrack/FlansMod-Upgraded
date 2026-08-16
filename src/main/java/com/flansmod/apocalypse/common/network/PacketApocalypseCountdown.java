package com.flansmod.apocalypse.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.apocalypse.client.ClientProxyApocalypse;
import com.flansmod.common.network.PacketBase;

public class PacketApocalypseCountdown extends PacketBase
{
	private int timeRemaining;
	
	public PacketApocalypseCountdown()
	{
		
	}
	
	public PacketApocalypseCountdown(int timeRemaining)
	{
		this.timeRemaining = timeRemaining;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(timeRemaining);
	}

	@Override
	public void decodeInto(ByteBuf data)
	{
		timeRemaining = data.readInt();
	}

	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		//Should not be received on server
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{
		ClientProxyApocalypse.updateApocalypseCountdownTimer(timeRemaining);
	}
}
