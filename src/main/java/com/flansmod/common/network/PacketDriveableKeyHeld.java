package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.api.IControllable;
import com.flansmod.common.FlansMod;

public class PacketDriveableKeyHeld extends PacketBase
{
	public int key;
	public boolean held;
	
	public PacketDriveableKeyHeld()
	{
	}
	
	public PacketDriveableKeyHeld(int key, boolean held)
	{
		this.key = key;
		this.held = held;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(key);
		data.writeBoolean(held);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		key = data.readInt();
		held = data.readBoolean();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		if(playerEntity.getVehicle() != null && playerEntity.getVehicle() instanceof IControllable)
		{
			((IControllable)playerEntity.getVehicle()).updateKeyHeldState(key, held);
		}
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.log.warn("Driveable key packet received on client. Skipping.");
	}
}
