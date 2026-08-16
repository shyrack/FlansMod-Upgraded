package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.guns.GunUtil;

public class PacketSeatUpdates extends PacketBase
{
	public int entityId, seatId;
	public float yaw, pitch;
	
	public PacketSeatUpdates()
	{
	}
	
	public PacketSeatUpdates(EntitySeat seat)
	{
		entityId = seat.driveable.getId();
		seatId = seat.seatInfo.id;
		yaw = seat.looking.getYaw();
		pitch = seat.looking.getPitch();
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(entityId);
		data.writeInt(seatId);
		data.writeFloat(yaw);
		data.writeFloat(pitch);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		entityId = data.readInt();
		seatId = data.readInt();
		yaw = data.readFloat();
		pitch = data.readFloat();
		
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		if(playerEntity == null)
		{
			FlansMod.log.warn("Received seat update packet from a null player, skipping!");
			return ;
		}
		EntityDriveable driveable = playerEntity.level().getEntity(entityId) instanceof EntityDriveable ?
				(EntityDriveable)playerEntity.level().getEntity(entityId) : null;
		if(driveable != null)
		{
			driveable.getSeat(seatId).prevLooking = driveable.getSeat(seatId).looking.clone();
			driveable.getSeat(seatId).looking.setAngles(yaw, pitch, 0F);
			//If on the server, update all surrounding players with these new angles
			FlansMod.getPacketHandler().sendToAllAround(this, driveable.getX(), driveable.getY(), driveable.getZ(), FlansMod.soundRange, GunUtil.getDimensionId(driveable.level()));
		}
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		EntityDriveable driveable = clientPlayer.level().getEntity(entityId) instanceof EntityDriveable ?
				(EntityDriveable)clientPlayer.level().getEntity(entityId) : null;
		if(driveable != null)
		{
			//If this is the player who sent the packet in the first place, don't read it
			if(driveable.getSeat(seatId) == null || driveable.getSeat(seatId).getControllingPassenger() == clientPlayer)
				return;
			driveable.getSeat(seatId).prevLooking = driveable.getSeat(seatId).looking.clone();
			driveable.getSeat(seatId).looking.setAngles(yaw, pitch, 0F);
		}
	}
}
