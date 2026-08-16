package com.flansmod.client.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import com.flansmod.api.IControllable;

public class MouseInputHandler
{
	private Minecraft mc;

	public static void init()
	{
	}

	public MouseInputHandler()
	{
		mc = Minecraft.getInstance();
	}

	public void checkMouseInput(double dx, double dy)
	{
		if(mc.screen != null)
		{
			return;
		}

		//Handle driving controls
		Player player = mc.player;
		if(player == null)
			return;
		Entity ridingEntity = player.getVehicle();
		if(ridingEntity instanceof IControllable)
		{
			IControllable riding = (IControllable)ridingEntity;
			riding.onMouseMoved((int)dx, (int)dy);
		}
	}
}
