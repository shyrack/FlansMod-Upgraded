package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.DriveableType;

public class PacketCraftDriveable extends PacketBase
{
	public String shortName;
	
	public PacketCraftDriveable()
	{
	}
	
	public PacketCraftDriveable(String s)
	{
		shortName = s;
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
		DriveableType type = DriveableType.getDriveable(shortName);
		//Try to craft the driveable
		FlansMod.proxy.craftDriveable(playerEntity, type);
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.log.warn("Received driveable repair packet on client side. Skipping.");
	}
}
