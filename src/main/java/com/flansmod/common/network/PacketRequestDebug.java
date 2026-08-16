package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.FlansMod;

/**
 * Sent from client to server when player wants to go into debug mode
 * Sent from server to client to confirm that player may go into debug mode (i.e. player is an op)
 *
 * @author James
 */

public class PacketRequestDebug extends PacketBase
{
	public PacketRequestDebug()
	{
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		if(FlansMod.serverInstance.getPlayerList().isOp(new NameAndId(playerEntity.getGameProfile())))
			FlansMod.packetHandler.sendTo(new PacketRequestDebug(), playerEntity);
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.DEBUG = true;
	}
	
}
