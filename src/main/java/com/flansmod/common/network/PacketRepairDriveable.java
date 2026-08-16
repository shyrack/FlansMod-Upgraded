package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.driveables.EnumDriveablePart;

public class PacketRepairDriveable extends PacketBase
{
	public String shortName;
	
	public PacketRepairDriveable()
	{
	}
	
	public PacketRepairDriveable(EnumDriveablePart part)
	{
		shortName = part.getShortName();
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		writeUTF(data, shortName);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		shortName = readUTF(data);
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		EnumDriveablePart part = EnumDriveablePart.getPart(shortName);
		//Try to repair the driveable
		FlansMod.proxy.repairDriveable(playerEntity, ((EntitySeat)playerEntity.getVehicle()).driveable, ((EntitySeat)playerEntity.getVehicle()).driveable.getDriveableData().parts.get(part));
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.log.warn("Received driveable repair packet on client side. Skipping.");
	}
}
