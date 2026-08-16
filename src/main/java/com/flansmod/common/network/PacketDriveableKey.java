package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.api.IControllable;
import com.flansmod.common.FlansMod;

public class PacketDriveableKey extends PacketBase
{
	public int key;
	
	public PacketDriveableKey()
	{
	}
	
	public PacketDriveableKey(int k)
	{
		key = k;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(key);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		key = data.readInt();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		if(playerEntity.getVehicle() != null && playerEntity.getVehicle() instanceof IControllable)
		{
			((IControllable)playerEntity.getVehicle()).serverHandleKeyPress(key, playerEntity);
		}
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.log.warn("Driveable keypress packet received on client. Skipping.");
	}
	
}
