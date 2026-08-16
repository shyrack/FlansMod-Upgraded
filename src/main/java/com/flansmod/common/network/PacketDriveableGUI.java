package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.flansmod.client.gui.GuiDriveableFuel;
import com.flansmod.client.gui.GuiDriveableInventory;
import com.flansmod.client.gui.GuiMechaInventory;
import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.driveables.mechas.EntityMecha;

public class PacketDriveableGUI extends PacketBase
{
	public int guiID;
	
	public PacketDriveableGUI()
	{
	}
	
	public PacketDriveableGUI(int i)
	{
		guiID = i;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(guiID);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		guiID = data.readInt();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		if(playerEntity.getVehicle() != null && playerEntity.getVehicle() instanceof EntitySeat)
		{
			//Echo the packet back to the player so the client can open the GUI
			FlansMod.getPacketHandler().sendTo(new PacketDriveableGUI(guiID), playerEntity);
		}
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		if(clientPlayer.getVehicle() == null || !(clientPlayer.getVehicle() instanceof EntitySeat))
			return;
		EntityDriveable d = ((EntitySeat)clientPlayer.getVehicle()).driveable;
		switch(guiID)
		{
			case 0: //Guns
				Minecraft.getInstance().setScreen(new GuiDriveableInventory(clientPlayer.getInventory(), clientPlayer.level(), d, 0));
				break;
			case 1: //Bombs / Mines
				Minecraft.getInstance().setScreen(new GuiDriveableInventory(clientPlayer.getInventory(), clientPlayer.level(), d, 1));
				break;
			case 2: //Fuel
				Minecraft.getInstance().setScreen(new GuiDriveableFuel(clientPlayer.getInventory(), clientPlayer.level(), d));
				break;
			case 3: //Cargo
				Minecraft.getInstance().setScreen(new GuiDriveableInventory(clientPlayer.getInventory(), clientPlayer.level(), d, 2));
				break;
			case 4: //Mecha
				if(d instanceof EntityMecha)
					Minecraft.getInstance().setScreen(new GuiMechaInventory(clientPlayer.getInventory(), clientPlayer.level(), (EntityMecha)d));
				break;
			case 5: //Missiles / Shells
				Minecraft.getInstance().setScreen(new GuiDriveableInventory(clientPlayer.getInventory(), clientPlayer.level(), d, 3));
				break;
		}
	}
}
