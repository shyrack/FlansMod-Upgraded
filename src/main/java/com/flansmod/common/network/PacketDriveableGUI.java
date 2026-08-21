package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import com.flansmod.common.ModMenus;
import com.flansmod.common.driveables.ContainerDriveableInventory;
import com.flansmod.common.driveables.ContainerDriveableMenu;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.driveables.mechas.ContainerMechaInventory;
import com.flansmod.common.driveables.mechas.EntityMecha;

public class PacketDriveableGUI extends PacketBase
{
	public static final int GUNS = 0;
	public static final int BOMBS = 1;
	public static final int FUEL = 2;
	public static final int CARGO = 3;
	public static final int MECHA = 4;
	public static final int MISSILES = 5;
	public static final int MENU = 6;
	
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
		if(!(playerEntity.getVehicle() instanceof EntitySeat))
			return;
		EntityDriveable driveable = ((EntitySeat)playerEntity.getVehicle()).driveable;
		if(driveable == null || driveable.isRemoved())
			return;
		if(guiID == MECHA && !(driveable instanceof EntityMecha))
			return;
		
		//Build the matching container server side and open it. The client
		//side screen is opened by fabric-menu-api's open screen packet, so no
		//echo back to the client is required.
		playerEntity.openMenu(new ExtendedMenuProvider<ModMenus.DriveableMenuData>()
		{
			@Override
			public ModMenus.DriveableMenuData getScreenOpeningData(ServerPlayer player)
			{
				return new ModMenus.DriveableMenuData(driveable.getId(), guiID);
			}
			
			@Override
			public Component getDisplayName()
			{
				return driveable.getDriveableType() != null
						? Component.literal(driveable.getDriveableType().name) : Component.literal("");
			}
			
			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
			{
				switch(guiID)
				{
					case FUEL:
						return new ContainerDriveableMenu(ModMenus.DRIVEABLE_FUEL, containerId, inventory, driveable, true);
					case MENU:
						return new ContainerDriveableMenu(ModMenus.DRIVEABLE_MENU, containerId, inventory, driveable, false);
					case MECHA:
						return new ContainerMechaInventory(ModMenus.MECHA_INVENTORY, containerId, inventory, (EntityMecha)driveable);
					default:
						return new ContainerDriveableInventory(ModMenus.DRIVEABLE_INVENTORY, containerId, inventory,
								driveable, ModMenus.screenForGUI(guiID));
				}
			}
		});
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		//Screen opening is now handled by fabric-menu-api's OpenScreenPayload handler
	}
}
