package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.boxes.GunBoxType;
import com.flansmod.common.types.InfoType;

public class PacketBuyWeapon extends PacketBase
{
	public String boxShortName;
	private String typeShortName;
	
	public PacketBuyWeapon()
	{
	}
	
	public PacketBuyWeapon(GunBoxType box, InfoType type)
	{
		boxShortName = box.shortName;
		typeShortName = type.shortName;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		writeUTF(data, boxShortName);
		writeUTF(data, typeShortName);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		boxShortName = readUTF(data);
		typeShortName = readUTF(data);
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		GunBoxType box = GunBoxType.getBox(boxShortName);
		box.block.buyGun(InfoType.getType(typeShortName), playerEntity.getInventory(), box);
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.log.warn("Received gun box purchase packet on client. Skipping.");
	}
}
