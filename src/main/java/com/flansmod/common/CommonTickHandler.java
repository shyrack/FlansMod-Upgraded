package com.flansmod.common;

import com.flansmod.common.teams.TeamsManager;

public class CommonTickHandler
{
	public CommonTickHandler()
	{
	}
	
	public static void serverTick(net.minecraft.server.MinecraftServer server)
	{
		//Handle all packets received since last tick
		if(TeamsManager.getInstance() != null)
		{
			TeamsManager.getInstance().tick();
		}
		FlansMod.playerHandler.serverTick();
		FlansMod.ticker++;
	}
}
