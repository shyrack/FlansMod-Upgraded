package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.guns.EntityAAGun;
import com.flansmod.common.guns.EntityMG;

public class PacketMGFire extends PacketBase
{
	public boolean held;
	
	public PacketMGFire()
	{
	}
	
	public PacketMGFire(boolean h)
	{
		held = h;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeBoolean(held);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		held = data.readBoolean();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		EntityMG mg = PlayerHandler.getPlayerData(playerEntity).mountingGun;
		if(mg != null)
		{
			mg.mouseHeld(held);
		}
		else if(playerEntity.getVehicle() instanceof EntityAAGun)
		{
			((EntityAAGun)playerEntity.getVehicle()).setMouseHeld(held);
		}
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.log.warn("MG firing packet received on client. Skipping.");
	}
}
