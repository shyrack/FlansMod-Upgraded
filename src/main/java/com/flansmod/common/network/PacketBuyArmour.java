package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.FlansMod;
import com.flansmod.common.teams.ArmourBoxType;

public class PacketBuyArmour extends PacketBase
{
	public String boxShortName;
	public String armourShortName;
	public int piece;
	
	public PacketBuyArmour()
	{
	}
	
	public PacketBuyArmour(String box, String armour, int i)
	{
		boxShortName = box;
		armourShortName = armour;
		piece = i;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		writeUTF(data, boxShortName);
		writeUTF(data, armourShortName);
		data.writeByte((byte)piece);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		boxShortName = readUTF(data);
		armourShortName = readUTF(data);
		piece = data.readByte();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		ArmourBoxType box = ArmourBoxType.getBox(boxShortName);
		box.block.buyArmour(armourShortName, piece, playerEntity.getInventory());
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.log.warn("Received armour box purchase packet on client. Skipping.");
	}
	
}
