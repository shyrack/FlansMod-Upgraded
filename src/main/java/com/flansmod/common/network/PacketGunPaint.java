package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.common.guns.ContainerGunModTable;
import com.flansmod.common.paintjob.ContainerPaintjobTable;

public class PacketGunPaint extends PacketBase
{
	private int paintjobID;
	
	public PacketGunPaint()
	{
		
	}
	
	public PacketGunPaint(int i)
	{
		paintjobID = i;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(paintjobID);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		paintjobID = data.readInt();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		if(playerEntity.containerMenu instanceof ContainerGunModTable)
		{
			ContainerGunModTable gunModTable = ((ContainerGunModTable)playerEntity.containerMenu);
			gunModTable.clickPaintjob(paintjobID);
		}
		else if(playerEntity.containerMenu instanceof ContainerPaintjobTable)
		{
			ContainerPaintjobTable paintjobTable = ((ContainerPaintjobTable)playerEntity.containerMenu);
			paintjobTable.clickPaintjob(paintjobID);
		}
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
	
	}
}
