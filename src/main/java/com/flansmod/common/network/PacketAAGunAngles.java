package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.guns.EntityAAGun;

public class PacketAAGunAngles extends PacketBase
{
	public int entityID;
	public float gunYaw;
	public float gunPitch;
	
	public PacketAAGunAngles()
	{
	}
	
	public PacketAAGunAngles(EntityAAGun entity)
	{
		entityID = entity.getId();
		gunYaw = entity.gunYaw;
		gunPitch = entity.gunPitch;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(entityID);
		data.writeFloat(gunYaw);
		data.writeFloat(gunPitch);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		entityID = data.readInt();
		gunYaw = data.readFloat();
		gunPitch = data.readFloat();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		Entity entity = clientPlayer.level().getEntity(entityID);
		if(entity instanceof EntityAAGun)
		{
			EntityAAGun aa = (EntityAAGun)entity;
			aa.prevGunYaw = aa.gunYaw;
			aa.prevGunPitch = aa.gunPitch;
			aa.gunYaw = gunYaw;
			aa.gunPitch = gunPitch;
		}
	}
	
}
