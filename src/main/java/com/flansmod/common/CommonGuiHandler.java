package com.flansmod.common;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.flansmod.client.gui.GuiGunBox;
import com.flansmod.common.guns.boxes.GunBoxType;

public class CommonGuiHandler
{
	public Object getServerGuiElement(int ID, Player player, Level world, int x, int y, int z)
	{
		return FlansMod.proxy.getServerGui(ID, player, world, x, y, z);
	}
	
	public Object getClientGuiElement(int ID, Player player, Level world, int x, int y, int z)
	{
		return FlansMod.proxy.getClientGui(ID, player, world, x, y, z);
	}
	
	public static void openGunBoxGui(Player player, GunBoxType type)
	{
		if(!FlansMod.isClient())
			return;
		Minecraft.getInstance().setScreen(new GuiGunBox(player.getInventory(), type));
	}
}
