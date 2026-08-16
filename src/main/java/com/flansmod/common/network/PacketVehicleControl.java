package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntityVehicle;
import com.flansmod.common.guns.GunUtil;

public class PacketVehicleControl extends PacketDriveableControl
{
	public boolean doors;
	
	public PacketVehicleControl()
	{
	}
	
	public PacketVehicleControl(EntityDriveable driveable)
	{
		super(driveable);
		EntityVehicle vehicle = (EntityVehicle)driveable;
		doors = vehicle.varDoor;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		super.encodeInto(data);
		data.writeBoolean(doors);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		super.decodeInto(data);
		doors = data.readBoolean();
		
	}
	
	@Override
	protected void updateDriveable(EntityDriveable driveable, boolean clientSide)
	{
		super.updateDriveable(driveable, clientSide);
		EntityVehicle vehicle = (EntityVehicle)driveable;
		vehicle.varDoor = doors;
		
		if(!clientSide)
		{
			FlansMod.getPacketHandler().sendToAllAround(new PacketVehicleControl(vehicle),
					posX,
					posY,
					posZ,
					FlansMod.driveableUpdateRange,
					GunUtil.getDimensionId(vehicle.level()));
		}
	}
}
