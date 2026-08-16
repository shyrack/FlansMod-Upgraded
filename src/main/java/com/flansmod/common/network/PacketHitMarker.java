package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.client.FlansModClient;
import com.flansmod.common.FlansMod;

public class PacketHitMarker extends PacketBase
{
	
	public PacketHitMarker()
	{
		//no data
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		//no data
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		//no data
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		FlansMod.log.warn("Received PacketHitMarker packet on server. Disregarding.");
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansModClient.addHitMarker();
	}
}
