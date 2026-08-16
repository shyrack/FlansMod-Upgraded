package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntityPlane;
import com.flansmod.common.driveables.EntityVehicle;

public class PacketDriveableControl extends PacketBase
{
	public int entityId;
	public double posX, posY, posZ;
	public float yaw, pitch, roll;
	public double motX, motY, motZ;
	public float avelx, avely, avelz;
	public float throttle;
	public float fuelInTank;
	public float steeringYaw;
	
	public PacketDriveableControl()
	{
	}
	
	public PacketDriveableControl(EntityDriveable driveable)
	{
		entityId = driveable.getId();
		posX = driveable.getX();
		posY = driveable.getY();
		posZ = driveable.getZ();
		yaw = driveable.axes.getYaw();
		pitch = driveable.axes.getPitch();
		roll = driveable.axes.getRoll();
		motX = driveable.getDeltaMovement().x;
		motY = driveable.getDeltaMovement().y;
		motZ = driveable.getDeltaMovement().z;
		avelx = driveable.angularVelocity.x;
		avely = driveable.angularVelocity.y;
		avelz = driveable.angularVelocity.z;
		throttle = driveable.throttle;
		fuelInTank = driveable.driveableData.fuelInTank;
		if(driveable instanceof EntityVehicle)
		{
			EntityVehicle veh = (EntityVehicle)driveable;
			steeringYaw = veh.wheelsYaw;
		}
		else if(driveable instanceof EntityPlane)
		{
			EntityPlane plane = (EntityPlane)driveable;
			steeringYaw = plane.flapsYaw;
		}
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(entityId);
		data.writeDouble(posX);
		data.writeDouble(posY);
		data.writeDouble(posZ);
		data.writeFloat(yaw);
		data.writeFloat(pitch);
		data.writeFloat(roll);
		data.writeDouble(motX);
		data.writeDouble(motY);
		data.writeDouble(motZ);
		data.writeFloat(avelx);
		data.writeFloat(avely);
		data.writeFloat(avelz);
		data.writeFloat(throttle);
		data.writeFloat(fuelInTank);
		data.writeFloat(steeringYaw);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		entityId = data.readInt();
		posX = data.readDouble();
		posY = data.readDouble();
		posZ = data.readDouble();
		yaw = data.readFloat();
		pitch = data.readFloat();
		roll = data.readFloat();
		motX = data.readDouble();
		motY = data.readDouble();
		motZ = data.readDouble();
		avelx = data.readFloat();
		avely = data.readFloat();
		avelz = data.readFloat();
		throttle = data.readFloat();
		fuelInTank = data.readFloat();
		steeringYaw = data.readFloat();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		if(playerEntity == null || playerEntity.level() == null)
			return;
		EntityDriveable driveable = playerEntity.level().getEntity(entityId) instanceof EntityDriveable ?
				(EntityDriveable)playerEntity.level().getEntity(entityId) : null;
		if(driveable != null)
			updateDriveable(driveable, false);
	}
	
	protected void updateDriveable(EntityDriveable driveable, boolean clientSide)
	{
		driveable.setPositionRotationAndMotion(posX, posY, posZ, yaw, pitch, roll, motX, motY, motZ, avelx, avely, avelz, throttle, steeringYaw);
		driveable.driveableData.fuelInTank = fuelInTank;
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		if(clientPlayer == null || clientPlayer.level() == null)
			return;
		EntityDriveable driveable = clientPlayer.level().getEntity(entityId) instanceof EntityDriveable ?
				(EntityDriveable)clientPlayer.level().getEntity(entityId) : null;
		if(driveable != null)
		{
			driveable.driveableData.fuelInTank = fuelInTank;
			if(driveable.getSeat(0) != null && driveable.getSeat(0).getControllingPassenger() == clientPlayer)
				return;
			updateDriveable(driveable, true);
		}
	}
}
