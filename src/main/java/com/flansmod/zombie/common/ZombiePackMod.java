package com.flansmod.zombie.common;

import com.flansmod.common.FlansMod;
import com.flansmod.common.IFlansModContentProvider;


public class ZombiePackMod implements IFlansModContentProvider
{
	public static final String MODID = "zombiepack";
	public static final String VERSION = "@VERSION@";
	
	@Override
	public String GetContentFolder() 
	{
		return "Zombie Pack";
	}
	
	@Override
	public void RegisterModelRedirects()
	{
		FlansMod.RegisterModelRedirect("zombie", "com.flansmod.zombie.client.model");
	}
}
