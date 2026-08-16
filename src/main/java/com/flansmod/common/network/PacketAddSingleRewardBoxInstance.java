package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.FlansMod;

public class PacketAddSingleRewardBoxInstance extends PacketBase
{
	public int boxHash;
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(boxHash);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		boxHash = data.readInt();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		FlansMod.Assert(false, "Handled single reward box packet on server!");
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		ClientTeamsData.AddRewardBox(boxHash);
	}
	
}
