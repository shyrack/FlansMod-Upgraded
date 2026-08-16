package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.EntityMG;

public class PacketMGMount extends PacketBase
{
	public int playerEntityId;
	public int mgEntityId;
	public boolean mounting;
	
	public PacketMGMount()
	{
	}
	
	public PacketMGMount(Player player, EntityMG mg, boolean mounting)
	{
		playerEntityId = player.getId();
		mgEntityId = mg.getId();
		this.mounting = mounting;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(playerEntityId);
		data.writeInt(mgEntityId);
		data.writeBoolean(mounting);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		playerEntityId = data.readInt();
		mgEntityId = data.readInt();
		mounting = data.readBoolean();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		FlansMod.log.warn("Received MG mount packet on server. Skipping.");
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		Player player = (Player)clientPlayer.level().getEntity(playerEntityId);
		EntityMG mg = (EntityMG)clientPlayer.level().getEntity(mgEntityId);
		if(mg != null && player != null)
			mg.mountGun(player, mounting);
	}
}
